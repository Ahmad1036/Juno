package com.example.juno;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class ChangePasswordActivity extends AppCompatActivity {

    private static final String TAG = "ChangePasswordActivity";
    private static final String USERS_PATH = "users";
    private static final String PREFS_NAME = "JunoUserPrefs";

    private ImageView backButton;
    private TextInputEditText currentPasswordEditText, newPasswordEditText, confirmPasswordEditText;
    private Button changePasswordButton;
    private TextInputLayout currentPasswordInputLayout, newPasswordInputLayout, confirmPasswordInputLayout;
    private ProgressBar progressBar;
    
    // Firebase Realtime Database
    private DatabaseReference mDatabase;
    
    // User data
    private String userId;
    private String userEmail;
    private String storedHashedPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Initialize Realtime Database
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://juno-aa7d2-default-rtdb.firebaseio.com/");
        mDatabase = database.getReference();

        // Initialize UI components
        initializeViews();
        
        // Create progress bar programmatically
        setupProgressBar();

        // Get user data from SharedPreferences
        retrieveUserData();
        
        // Set up animations for UI elements
        setupAnimations();

        // Set up click listeners
        setupClickListeners();
        
        // Load user password data
        loadPasswordData();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        
        // Password fields
        currentPasswordEditText = findViewById(R.id.currentPasswordEditText);
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        currentPasswordInputLayout = findViewById(R.id.currentPasswordInputLayout);
        newPasswordInputLayout = findViewById(R.id.newPasswordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);
        changePasswordButton = findViewById(R.id.changePasswordButton);
    }
    
    private void setupProgressBar() {
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        
        // Add to layout
        FrameLayout rootView = (FrameLayout) findViewById(android.R.id.content);
        rootView.addView(progressBar);
        
        // Center in parent
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = android.view.Gravity.CENTER;
        progressBar.setLayoutParams(params);
    }

    private void retrieveUserData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        userEmail = prefs.getString("userEmail", "");
        
        if (userId == null) {
            // No user logged in, redirect to sign in
            Toast.makeText(this, "Please sign in to change your password", Toast.LENGTH_LONG).show();
            finish();
        }
    }
    
    private void setupAnimations() {
        // Fade in animations for form elements
        View[] views = {currentPasswordEditText, newPasswordEditText, confirmPasswordEditText, changePasswordButton};
        
        for (int i = 0; i < views.length; i++) {
            views[i].setAlpha(0f);
            views[i].animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setStartDelay(100 + (i * 100))
                    .start();
        }
    }

    private void setupClickListeners() {
        // Back button click
        backButton.setOnClickListener(v -> onBackPressed());
        
        // Change Password button click
        changePasswordButton.setOnClickListener(v -> {
            if (validatePasswordForm()) {
                showProgressBar(true);
                changeUserPassword();
            }
        });
    }
    
    private void loadPasswordData() {
        if (userId != null) {
            showProgressBar(true);
            
            // Try to get user data by querying on email field first
            mDatabase.child(USERS_PATH).orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // User found by email query
                        for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                            if (childSnapshot.hasChild("password")) {
                                storedHashedPassword = childSnapshot.child("password").getValue(String.class);
                                showProgressBar(false);
                            } else {
                                showProgressBar(false);
                                Toast.makeText(ChangePasswordActivity.this, 
                                        "No password found for this account", 
                                        Toast.LENGTH_LONG).show();
                            }
                            return; // Process only the first match
                        }
                    } else {
                        // Try with encoded email as fallback
                        String encodedEmail = encodeEmail(userEmail);
                        mDatabase.child(USERS_PATH).child(encodedEmail).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    if (dataSnapshot.hasChild("password")) {
                                        storedHashedPassword = dataSnapshot.child("password").getValue(String.class);
                                        showProgressBar(false);
                                    } else {
                                        showProgressBar(false);
                                        Toast.makeText(ChangePasswordActivity.this, 
                                                "No password found for this account", 
                                                Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    // As a last resort, check all users
                                    mDatabase.child(USERS_PATH).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot allUsersSnapshot) {
                                            boolean userFound = false;
                                            for (DataSnapshot userSnapshot : allUsersSnapshot.getChildren()) {
                                                // Check if this node has our email
                                                if (userSnapshot.hasChild("email") && 
                                                    userEmail.equals(userSnapshot.child("email").getValue(String.class))) {
                                                    // Found the user
                                                    userFound = true;
                                                    
                                                    if (userSnapshot.hasChild("password")) {
                                                        storedHashedPassword = userSnapshot.child("password").getValue(String.class);
                                                    } else {
                                                        Toast.makeText(ChangePasswordActivity.this, 
                                                                "No password found for this account", 
                                                                Toast.LENGTH_LONG).show();
                                                    }
                                                    break;
                                                }
                                            }
                                            
                                            showProgressBar(false);
                                            if (!userFound) {
                                                Toast.makeText(ChangePasswordActivity.this, 
                                                        "User data not found. Please sign in again.", 
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        }
                                        
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                            showProgressBar(false);
                                            Log.e(TAG, "Database error loading all users: " + databaseError.getMessage());
                                            Toast.makeText(ChangePasswordActivity.this, 
                                                "Error loading data: " + databaseError.getMessage(), 
                                                Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                showProgressBar(false);
                                Log.e(TAG, "Database error loading with encoded email: " + databaseError.getMessage());
                                Toast.makeText(ChangePasswordActivity.this, 
                                    "Error loading data: " + databaseError.getMessage(), 
                                    Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    showProgressBar(false);
                    Log.e(TAG, "Database error loading by email query: " + databaseError.getMessage());
                    Toast.makeText(ChangePasswordActivity.this, 
                        "Error loading data: " + databaseError.getMessage(), 
                        Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private boolean validatePasswordForm() {
        boolean valid = true;

        String currentPassword = currentPasswordEditText.getText().toString();
        String newPassword = newPasswordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();
        
        // Validate current password
        if (TextUtils.isEmpty(currentPassword)) {
            currentPasswordInputLayout.setError("Current password is required");
            valid = false;
        } else {
            currentPasswordInputLayout.setError(null);
        }

        // Validate new password
        if (TextUtils.isEmpty(newPassword)) {
            newPasswordInputLayout.setError("New password is required");
            valid = false;
        } else if (newPassword.length() < 6) {
            newPasswordInputLayout.setError("Password must be at least 6 characters");
            valid = false;
        } else {
            newPasswordInputLayout.setError(null);
        }

        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInputLayout.setError("Please confirm your password");
            valid = false;
        } else if (!confirmPassword.equals(newPassword)) {
            confirmPasswordInputLayout.setError("Passwords do not match");
            valid = false;
        } else {
            confirmPasswordInputLayout.setError(null);
        }
        
        // Make sure new password is different from current
        if (valid && hashPassword(currentPassword).equals(hashPassword(newPassword))) {
            newPasswordInputLayout.setError("New password must be different from current password");
            valid = false;
        }

        return valid;
    }
    
    private void changeUserPassword() {
        String currentPassword = currentPasswordEditText.getText().toString();
        String newPassword = newPasswordEditText.getText().toString();
        
        // Hash current password to compare with stored password
        String hashedCurrentPassword = hashPassword(currentPassword);
        
        if (storedHashedPassword != null && hashedCurrentPassword.equals(storedHashedPassword)) {
            // Current password is correct, proceed with update
            String hashedNewPassword = hashPassword(newPassword);
            
            // Find the correct user node by querying on email
            mDatabase.child(USERS_PATH).orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // User found by email query
                        for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                            // Update password in Firebase
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("password", hashedNewPassword);
                            
                            // Get the key of this user node
                            String userNodeKey = childSnapshot.getKey();
                            
                            mDatabase.child(USERS_PATH).child(userNodeKey).updateChildren(updates)
                                .addOnSuccessListener(aVoid -> {
                                    showProgressBar(false);
                                    
                                    // Clear password fields
                                    currentPasswordEditText.setText("");
                                    newPasswordEditText.setText("");
                                    confirmPasswordEditText.setText("");
                                    
                                    Toast.makeText(ChangePasswordActivity.this, 
                                            "Password changed successfully", Toast.LENGTH_SHORT).show();
                                            
                                    // Finish activity after a brief delay
                                    new android.os.Handler().postDelayed(ChangePasswordActivity.this::finish, 1500);
                                })
                                .addOnFailureListener(e -> {
                                    showProgressBar(false);
                                    Log.e(TAG, "Failed to change password", e);
                                    Toast.makeText(ChangePasswordActivity.this, 
                                            "Failed to change password: " + e.getMessage(), 
                                            Toast.LENGTH_LONG).show();
                                });
                            
                            return; // Process only the first match
                        }
                    } else {
                        // Try with encoded email as fallback
                        String encodedEmail = encodeEmail(userEmail);
                        
                        // Update password in Firebase
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("password", hashedNewPassword);
                        
                        mDatabase.child(USERS_PATH).child(encodedEmail).updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                showProgressBar(false);
                                
                                // Clear password fields
                                currentPasswordEditText.setText("");
                                newPasswordEditText.setText("");
                                confirmPasswordEditText.setText("");
                                
                                Toast.makeText(ChangePasswordActivity.this, 
                                        "Password changed successfully", Toast.LENGTH_SHORT).show();
                                        
                                // Finish activity after a brief delay
                                new android.os.Handler().postDelayed(ChangePasswordActivity.this::finish, 1500);
                            })
                            .addOnFailureListener(e -> {
                                showProgressBar(false);
                                Log.e(TAG, "Failed to change password", e);
                                Toast.makeText(ChangePasswordActivity.this, 
                                        "Failed to change password: " + e.getMessage(), 
                                        Toast.LENGTH_LONG).show();
                            });
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    showProgressBar(false);
                    Log.e(TAG, "Database error finding user for password change", databaseError.toException());
                    Toast.makeText(ChangePasswordActivity.this, 
                            "Error updating password: " + databaseError.getMessage(), 
                            Toast.LENGTH_LONG).show();
                }
            });
        } else {
            showProgressBar(false);
            currentPasswordInputLayout.setError("Current password is incorrect");
            Toast.makeText(ChangePasswordActivity.this, 
                    "Current password is incorrect", Toast.LENGTH_LONG).show();
        }
    }
    
    // Helper method to hash password
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            
            // Convert byte array to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error hashing password", e);
            // Fallback to simple encoding if hashing fails
            return Base64.encodeToString(password.getBytes(), Base64.NO_WRAP);
        }
    }
    
    private String encodeEmail(String email) {
        return email.replace(".", ",");
    }
    
    private void showProgressBar(boolean show) {
        if (progressBar != null) {
            if (show) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        }
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
} 