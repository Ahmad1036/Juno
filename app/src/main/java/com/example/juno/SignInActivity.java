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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class SignInActivity extends AppCompatActivity {

    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button signInButton;
    private Button googleSignInButton;
    private TextView forgotPasswordText;
    private TextView signUpText;
    
    // Firebase Authentication
    private FirebaseAuth mAuth;
    // Firebase Realtime Database
    private DatabaseReference mDatabase;
    // Google Sign In Client
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Initialize Realtime Database
        mDatabase = FirebaseDatabase.getInstance("https://juno-aa7d2-default-rtdb.firebaseio.com/").getReference();
        
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize UI components
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        signInButton = findViewById(R.id.sign_in_button);
        googleSignInButton = findViewById(R.id.google_sign_in_button);
        forgotPasswordText = findViewById(R.id.forgot_password_text);
        signUpText = findViewById(R.id.sign_up_text);

        // Set up animations for UI elements
        setupAnimations();

        // Set up click listeners
        setupClickListeners();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in and update UI accordingly
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already signed in, go directly to Dashboard
            startActivity(new Intent(SignInActivity.this, DashboardActivity.class));
            finish();
        }
    }

    private void setupAnimations() {
        // Fade in animations for all elements
        View[] views = {emailEditText, passwordEditText, signInButton, googleSignInButton, forgotPasswordText, signUpText};
        
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
        // Sign In button click
        signInButton.setOnClickListener(v -> {
            if (validateForm()) {
                signIn(emailEditText.getText().toString(), passwordEditText.getText().toString());
            }
        });

        // Google Sign In button click
        googleSignInButton.setOnClickListener(v -> {
            googleSignIn();
        });

        // Forgot Password click
        forgotPasswordText.setOnClickListener(v -> {
            Intent intent = new Intent(SignInActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        // Sign Up click
        signUpText.setOnClickListener(v -> {
            // Navigate to sign up screen
            Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
            startActivity(intent);
            // Use a fade transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private boolean validateForm() {
        boolean valid = true;

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

        return valid;
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void signIn(String email, String password) {
        // Show loading indicator
        // If you have a progress bar, uncomment this line
        // progressBar.setVisibility(View.VISIBLE);
        
        Log.d(TAG, "Attempting to sign in with email: " + email);
        
        // Encode email for Firebase path
        String encodedEmail = encodeEmail(email);
        
        // First, get the user ID from the email lookup
        mDatabase.child("user_emails").child(encodedEmail).get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().getValue() != null) {
                    // Got the user ID, now get the full user data
                    String userId = task.getResult().getValue().toString();
                    
                    mDatabase.child("users").child(userId).get()
                        .addOnCompleteListener(userTask -> {
                            // Hide loading indicator
                            // progressBar.setVisibility(View.GONE);
                            
                            if (userTask.isSuccessful() && userTask.getResult() != null) {
                                // Get user data
                                Map<String, Object> userData = (Map<String, Object>) userTask.getResult().getValue();
                                
                                if (userData != null) {
                                    // Verify password
                                    String hashedPassword = hashPassword(password);
                                    String storedPassword = (String) userData.get("password");
                                    
                                    if (storedPassword != null && storedPassword.equals(hashedPassword)) {
                                        // Password correct, store user session
                                        String name = (String) userData.get("name");
                                        String userEmail = (String) userData.get("email");
                                        
                                        storeUserSession(userId, name, userEmail);
                                        
                                        // Login successful
                                        Toast.makeText(SignInActivity.this, "Sign in successful!", Toast.LENGTH_SHORT).show();
                                        navigateToDashboard();
                                    } else {
                                        // Password incorrect
                                        Toast.makeText(SignInActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    // User data is null
                                    Toast.makeText(SignInActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                // Failed to get user data
                                Log.w(TAG, "Failed to get user data", userTask.getException());
                                Toast.makeText(SignInActivity.this, "Failed to get user data", Toast.LENGTH_SHORT).show();
                            }
                        });
                } else {
                    // Hide loading indicator
                    // progressBar.setVisibility(View.GONE);
                    
                    // Email not found
                    Log.w(TAG, "Email not found", task.getException());
                    Toast.makeText(SignInActivity.this, "Email not registered", Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    // Helper method to encode email for Firebase path (replacing . with ,)
    private String encodeEmail(String email) {
        return email.replace(".", ",");
    }
    
    // Helper method to hash password - must match the one in SignUpActivity
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
    
    private void googleSignIn() {
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
                    Log.w(TAG, "signInWithCredential:failure", task.getException());
                    Toast.makeText(SignInActivity.this, "Authentication failed: " + 
                            task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void saveUserToDatabase(String userId, String name, String email) {
        // Create a User map
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("createdAt", System.currentTimeMillis());
        
        // Save to Firebase Realtime Database
        mDatabase.child("users").child(userId).setValue(user)
            .addOnSuccessListener(aVoid -> {
                // Navigate to Dashboard
                navigateToDashboard();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(SignInActivity.this, "Failed to save user data: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                // Still navigate to dashboard even if saving to database fails
                navigateToDashboard();
            });
    }
    
    private void navigateToDashboard() {
        Toast.makeText(SignInActivity.this, "Sign in successful!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(SignInActivity.this, DashboardActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
    
    private void resetPassword(String email) {
        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(SignInActivity.this, 
                            "Password reset email sent to " + email, 
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SignInActivity.this, 
                            "Failed to send reset email: " + task.getException().getMessage(), 
                            Toast.LENGTH_SHORT).show();
                }
            });
    }
} 