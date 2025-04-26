package com.example.juno;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
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

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";
    private static final int RC_SIGN_IN = 9001;

    private EditText fullNameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button signUpButton;
    private Button googleSignUpButton;
    private TextView signInText;
    private TextView termsText;
    private ProgressBar progressBar;
    
    // Firebase Authentication
    private FirebaseAuth mAuth;
    // Firebase Realtime Database
    private DatabaseReference mDatabase;
    // Google Sign In Client
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Ensure Firebase is initialized
        try {
            // Check if Firebase is already initialized
            FirebaseApp defaultApp = FirebaseApp.getInstance();
            if (defaultApp == null) {
                // If not initialized, initialize with explicit options
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setApiKey("AIzaSyBWvNG1XvPkK5UHwOCkDfqpTEiUKaQDFMI")
                        .setApplicationId("1:586442288097:android:a8a4f24d76ed40ac18c0a0")
                        .setDatabaseUrl("https://juno-aa7d2-default-rtdb.firebaseio.com")
                        .setProjectId("juno-aa7d2")
                        .build();
                FirebaseApp.initializeApp(this, options);
            }
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization error", e);
        }

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Initialize Realtime Database without persistence
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://juno-aa7d2-default-rtdb.firebaseio.com/");
        mDatabase = database.getReference();
        
        // Configure Google Sign In with default web client ID
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("586442288097-uvwxyz1234567890abcdefghijklmnop.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize UI components
        fullNameEditText = findViewById(R.id.full_name_edit_text);
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        confirmPasswordEditText = findViewById(R.id.confirm_password_edit_text);
        signUpButton = findViewById(R.id.sign_up_button);
        googleSignUpButton = findViewById(R.id.google_sign_up_button);
        signInText = findViewById(R.id.sign_in_text);
        termsText = findViewById(R.id.terms_text);
        
        // Create progress bar programmatically if not in layout
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

        // Set up animations for UI elements
        setupAnimations();

        // Set up click listeners
        setupClickListeners();
    }

    private void setupAnimations() {
        // Fade in animations for all elements
        View[] views = {fullNameEditText, emailEditText, passwordEditText, confirmPasswordEditText, 
                        signUpButton, googleSignUpButton, signInText, termsText};
        
        for (int i = 0; i < views.length; i++) {
            views[i].setAlpha(0f);
            views[i].animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setStartDelay(300 + (i * 100))
                    .start();
        }
    }

    private void setupClickListeners() {
        // Sign Up button click
        signUpButton.setOnClickListener(v -> {
            if (validateForm()) {
                showProgressBar(true);
                createAccount(
                    fullNameEditText.getText().toString(),
                    emailEditText.getText().toString(),
                    passwordEditText.getText().toString()
                );
            }
        });

        // Google Sign Up button click
        googleSignUpButton.setOnClickListener(v -> {
            showProgressBar(true);
            googleSignUp();
        });

        // Sign In text click
        signInText.setOnClickListener(v -> {
            // Navigate back to sign in screen with animations
            Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
        
        // Terms text click
        termsText.setOnClickListener(v -> {
            Toast.makeText(SignUpActivity.this, "Terms and Privacy Policy will be displayed here", 
                    Toast.LENGTH_SHORT).show();
        });
    }

    private boolean validateForm() {
        boolean valid = true;

        String fullName = fullNameEditText.getText().toString();
        if (TextUtils.isEmpty(fullName)) {
            fullNameEditText.setError("Required.");
            valid = false;
        } else {
            fullNameEditText.setError(null);
        }

        String email = emailEditText.getText().toString();
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Required.");
            valid = false;
        } else if (!isValidEmail(email)) {
            emailEditText.setError("Invalid email format.");
            valid = false;
        } else {
            emailEditText.setError(null);
        }

        String password = passwordEditText.getText().toString();
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Required.");
            valid = false;
        } else if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters.");
            valid = false;
        } else {
            passwordEditText.setError(null);
        }

        String confirmPassword = confirmPasswordEditText.getText().toString();
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordEditText.setError("Required.");
            valid = false;
        } else if (!confirmPassword.equals(password)) {
            confirmPasswordEditText.setError("Passwords do not match.");
            valid = false;
        } else {
            confirmPasswordEditText.setError(null);
        }

        return valid;
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void createAccount(String name, String email, String password) {
        Log.d(TAG, "Attempting to create user: " + email);
        showProgressBar(true);
        
        // First check if email already exists
        String encodedEmail = encodeEmail(email);
        mDatabase.child("user_emails").child(encodedEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Email already exists
                    showProgressBar(false);
                    emailEditText.setError("Email already in use. Please use a different email or sign in.");
                    
                    // Add toast message
                    Toast.makeText(SignUpActivity.this, 
                        "An account with this email already exists", Toast.LENGTH_LONG).show();
                    
                    // Scroll to the email field
                    emailEditText.requestFocus();
                } else {
                    // Email doesn't exist, proceed with account creation
                    proceedWithAccountCreation(name, email, password);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Database error occurred
                showProgressBar(false);
                Log.e(TAG, "Database error checking email: " + databaseError.getMessage());
                Toast.makeText(SignUpActivity.this, 
                    "Error checking email availability: " + databaseError.getMessage(), 
                    Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void proceedWithAccountCreation(String name, String email, String password) {
        // Generate a unique ID for the user
        String userId = mDatabase.child("users").push().getKey();
        
        // Hash the password before storing
        String hashedPassword = hashPassword(password);
        
        // Create a User map
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("password", hashedPassword); // Store hashed password
        user.put("createdAt", System.currentTimeMillis());
        
        // Save to Firebase Realtime Database
        mDatabase.child("users").child(userId).setValue(user)
            .addOnSuccessListener(aVoid -> {
                showProgressBar(false);
                Log.d(TAG, "User data saved successfully");
                Toast.makeText(SignUpActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                
                // Also save a lookup by email for easier login later
                mDatabase.child("user_emails").child(encodeEmail(email)).setValue(userId);
                
                // Store user session locally
                storeUserSession(userId, name, email);
                
                // Navigate to Dashboard
                navigateToDashboard();
            })
            .addOnFailureListener(e -> {
                showProgressBar(false);
                Log.e(TAG, "Failed to save user data", e);
                Toast.makeText(SignUpActivity.this, "Failed to save user data: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
            });
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
    
    // Helper method to encode email for Firebase path (replacing . with ,)
    private String encodeEmail(String email) {
        return email.replace(".", ",");
    }
    
    // Store user session locally
    private void storeUserSession(String userId, String name, String email) {
        SharedPreferences prefs = getSharedPreferences("JunoUserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("userId", userId);
        editor.putString("userName", name);
        editor.putString("userEmail", email);
        editor.putBoolean("isLoggedIn", true);
        editor.apply();
    }
    
    private void googleSignUp() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed
                showProgressBar(false);
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    // Sign in success
                    Log.d(TAG, "signInWithCredential:success");
                    FirebaseUser user = mAuth.getCurrentUser();
                    
                    // Check if user is new
                    boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();
                    if (isNewUser && user != null) {
                        // Save user data to Realtime Database
                        saveUserToDatabase(user.getUid(), user.getDisplayName(), user.getEmail());
                    } else {
                        // Just navigate to Dashboard
                        navigateToDashboard();
                    }
                } else {
                    // If sign in fails, display a message to the user
                    showProgressBar(false);
                    Log.w(TAG, "signInWithCredential:failure", task.getException());
                    Toast.makeText(SignUpActivity.this, "Authentication failed: " + 
                            task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void saveUserToDatabase(String userId, String name, String email) {
        Log.d(TAG, "Saving user to database: " + userId);
        
        // Create a User map
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("createdAt", System.currentTimeMillis());
        
        // Save to Firebase Realtime Database
        mDatabase.child("users").child(userId).setValue(user)
            .addOnSuccessListener(aVoid -> {
                showProgressBar(false);
                Log.d(TAG, "User data saved successfully");
                Toast.makeText(SignUpActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                // Navigate to Dashboard
                navigateToDashboard();
            })
            .addOnFailureListener(e -> {
                showProgressBar(false);
                Log.e(TAG, "Failed to save user data", e);
                Toast.makeText(SignUpActivity.this, "Failed to save user data: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                // Still navigate to dashboard
                navigateToDashboard();
            });
    }
    
    private void navigateToDashboard() {
        Intent intent = new Intent(SignUpActivity.this, DashboardActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
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
        // Navigate back to sign in screen with animations
        Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
} 