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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;
import java.util.UUID;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPasswordActivity";

    private EditText emailEditText;
    private Button resetButton;
    private TextView backToLoginText;
    private ProgressBar progressBar;
    
    // Firebase Realtime Database
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        
        // Initialize Firebase Realtime Database
        mDatabase = FirebaseDatabase.getInstance("https://juno-aa7d2-default-rtdb.firebaseio.com/").getReference();

        // Initialize UI components
        emailEditText = findViewById(R.id.email_edit_text);
        resetButton = findViewById(R.id.reset_button);
        backToLoginText = findViewById(R.id.back_to_login_text);
        progressBar = findViewById(R.id.progress_bar);
        
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
            String email = emailEditText.getText().toString().trim();
            
            if (validateForm(email)) {
                resetPassword(email);
            }
        });
        
        // Back to login text click
        backToLoginText.setOnClickListener(v -> {
            finish(); // Go back to previous activity (SignInActivity)
        });
    }
    
    private boolean validateForm(String email) {
        boolean valid = true;
        
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Required.");
            valid = false;
        } else if (!isValidEmail(email)) {
            emailEditText.setError("Invalid email format.");
            valid = false;
        } else {
            emailEditText.setError(null);
        }
        
        return valid;
    }
    
    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    
    private void resetPassword(String email) {
        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);
        
        // Convert email to database-safe format
        String encodedEmail = encodeEmail(email);
        
        // Check if the email exists in our database
        mDatabase.child("user_emails").child(encodedEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Email exists, generate reset token
                    String userId = dataSnapshot.getValue(String.class);
                    generateResetToken(userId, email);
                } else {
                    // Email not found
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ForgotPasswordActivity.this, 
                            "No account found with this email address", 
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Log.w(TAG, "getUser:onCancelled", databaseError.toException());
                Toast.makeText(ForgotPasswordActivity.this, 
                        "Database error: " + databaseError.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void generateResetToken(String userId, String email) {
        // Generate a random token
        String resetToken = UUID.randomUUID().toString();
        
        // Set expiration time (24 hours from now)
        long expirationTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000);
        
        // Save reset token in database
        mDatabase.child("password_reset_tokens").child(userId).child("token").setValue(resetToken);
        mDatabase.child("password_reset_tokens").child(userId).child("email").setValue(email);
        mDatabase.child("password_reset_tokens").child(userId).child("expiration").setValue(expirationTime)
            .addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                
                if (task.isSuccessful()) {
                    // In a real app, this would send an email with a reset link
                    // For this demo, we'll just show a success message with the token
                    Toast.makeText(ForgotPasswordActivity.this, 
                            "Password reset token generated: " + resetToken, 
                            Toast.LENGTH_LONG).show();
                            
                    // Navigate to reset password screen
                    Intent intent = new Intent(ForgotPasswordActivity.this, ResetPasswordActivity.class);
                    intent.putExtra("userId", userId);
                    intent.putExtra("resetToken", resetToken);
                    intent.putExtra("email", email);
                    startActivity(intent);
                } else {
                    Toast.makeText(ForgotPasswordActivity.this, 
                            "Failed to generate reset token: " + Objects.requireNonNull(task.getException()).getMessage(), 
                            Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private String encodeEmail(String email) {
        return email.replace(".", ",");
    }
    
    private void setupAnimations() {
        // Animate UI elements to fade in
        View[] views = {emailEditText, resetButton, backToLoginText};
        
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