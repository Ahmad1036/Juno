package com.example.juno;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.HashMap;
import java.util.Map;

import android.util.Base64;

public class ResetPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ResetPasswordActivity";

    private EditText tokenEditText;
    private EditText newPasswordEditText;
    private EditText confirmPasswordEditText;
    private Button resetButton;
    private TextView backToLoginText;
    private ProgressBar progressBar;
    
    private String userId;
    private String resetToken;
    private String email;
    
    // Firebase Realtime Database
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);
        
        // Get data from intent
        userId = getIntent().getStringExtra("userId");
        resetToken = getIntent().getStringExtra("resetToken");
        email = getIntent().getStringExtra("email");
        
        if (userId == null || resetToken == null || email == null) {
            Toast.makeText(this, "Invalid reset information", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize Firebase Realtime Database
        mDatabase = FirebaseDatabase.getInstance("https://juno-aa7d2-default-rtdb.firebaseio.com/").getReference();

        // Initialize UI components
        tokenEditText = findViewById(R.id.token_edit_text);
        newPasswordEditText = findViewById(R.id.new_password_edit_text);
        confirmPasswordEditText = findViewById(R.id.confirm_password_edit_text);
        resetButton = findViewById(R.id.reset_button);
        backToLoginText = findViewById(R.id.back_to_login_text);
        progressBar = findViewById(R.id.progress_bar);
        
        // Pre-fill token
        tokenEditText.setText(resetToken);
        
        // Initially hide progress bar
        progressBar.setVisibility(View.GONE);

        // Set up click listeners
        setupClickListeners();
        
        // Set up animations
        setupAnimations();
    }
    
    private void setupClickListeners() {
        // Reset password button click
        resetButton.setOnClickListener(v -> {
            String token = tokenEditText.getText().toString().trim();
            String newPassword = newPasswordEditText.getText().toString();
            String confirmPassword = confirmPasswordEditText.getText().toString();
            
            if (validateForm(token, newPassword, confirmPassword)) {
                confirmReset(token, newPassword);
            }
        });
        
        // Back to login text click
        backToLoginText.setOnClickListener(v -> {
            Intent intent = new Intent(ResetPasswordActivity.this, SignInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
    
    private boolean validateForm(String token, String newPassword, String confirmPassword) {
        boolean valid = true;
        
        if (TextUtils.isEmpty(token)) {
            tokenEditText.setError("Required.");
            valid = false;
        } else if (!token.equals(resetToken)) {
            tokenEditText.setError("Invalid token.");
            valid = false;
        } else {
            tokenEditText.setError(null);
        }
        
        if (TextUtils.isEmpty(newPassword)) {
            newPasswordEditText.setError("Required.");
            valid = false;
        } else if (newPassword.length() < 6) {
            newPasswordEditText.setError("Password must be at least 6 characters.");
            valid = false;
        } else {
            newPasswordEditText.setError(null);
        }
        
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordEditText.setError("Required.");
            valid = false;
        } else if (!confirmPassword.equals(newPassword)) {
            confirmPasswordEditText.setError("Passwords do not match.");
            valid = false;
        } else {
            confirmPasswordEditText.setError(null);
        }
        
        return valid;
    }
    
    private void confirmReset(String token, String newPassword) {
        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);
        
        // Verify token is valid and not expired
        mDatabase.child("password_reset_tokens").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get token data
                    String storedToken = dataSnapshot.child("token").getValue(String.class);
                    Long expirationTime = dataSnapshot.child("expiration").getValue(Long.class);
                    
                    long currentTime = System.currentTimeMillis();
                    
                    if (storedToken != null && storedToken.equals(token) && expirationTime != null && currentTime < expirationTime) {
                        // Token is valid and not expired, update password
                        updatePassword(newPassword);
                    } else if (expirationTime != null && currentTime > expirationTime) {
                        // Token expired
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ResetPasswordActivity.this, 
                                "Reset token has expired. Please request a new one.", 
                                Toast.LENGTH_LONG).show();
                    } else {
                        // Invalid token
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ResetPasswordActivity.this, 
                                "Invalid reset token. Please try again.", 
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    // No token found
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ResetPasswordActivity.this, 
                            "Reset token not found. Please request a new one.", 
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Log.w(TAG, "verifyToken:onCancelled", databaseError.toException());
                Toast.makeText(ResetPasswordActivity.this, 
                        "Database error: " + databaseError.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updatePassword(String newPassword) {
        // Hash the new password
        String hashedPassword = hashPassword(newPassword);
        
        // Update the password in database
        mDatabase.child("users").child(userId).child("password").setValue(hashedPassword)
            .addOnCompleteListener(task -> {
                // Delete the reset token
                mDatabase.child("password_reset_tokens").child(userId).removeValue();
                
                progressBar.setVisibility(View.GONE);
                
                if (task.isSuccessful()) {
                    Toast.makeText(ResetPasswordActivity.this, 
                            "Password reset successful! You can now sign in with your new password.", 
                            Toast.LENGTH_LONG).show();
                    
                    // Navigate back to sign in
                    Intent intent = new Intent(ResetPasswordActivity.this, SignInActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(ResetPasswordActivity.this, 
                            "Failed to reset password: " + Objects.requireNonNull(task.getException()).getMessage(), 
                            Toast.LENGTH_SHORT).show();
                }
            });
    }
    
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
    
    private void setupAnimations() {
        // Animate UI elements to fade in
        View[] views = {tokenEditText, newPasswordEditText, confirmPasswordEditText, resetButton, backToLoginText};
        
        for (int i = 0; i < views.length; i++) {
            views[i].setAlpha(0f);
            views[i].animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setStartDelay(300 + (i * 100))
                    .start();
        }
    }
} 