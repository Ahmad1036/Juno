package com.example.juno;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final long SPLASH_DELAY = 4000; // 3 seconds - giving more time to see the full GIF

    private ImageView logoImageView;
    private TextView taglineTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize Firebase
        initializeFirebase();

        // Initialize UI elements
        logoImageView = findViewById(R.id.logo_image);
        taglineTextView = findViewById(R.id.tagline_text);

        // Load GIF using Glide
        loadLogoGif();

        // Set up animations for tagline
        setupAnimations();

        // Navigate to next screen after delay
        navigateAfterDelay();
    }

    private void loadLogoGif() {
        // Load the GIF from raw directory using Glide
        Glide.with(this)
             .asGif()
             .load(R.raw.logo_animation)  // Make sure to put your logo_animation.gif in the raw folder
             .into(logoImageView);
    }

    private void initializeFirebase() {
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
            
            // Enable offline persistence
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization error", e);
        }
    }

    private void setupAnimations() {
        // Tagline animation
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        slideUp.setStartOffset(500);
        taglineTextView.startAnimation(slideUp);
    }

    private void navigateAfterDelay() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Check if user is logged in
            if (isUserLoggedIn()) {
                // User is logged in, go to dashboard
                startActivity(new Intent(SplashActivity.this, DashboardActivity.class));
            } else {
                // User is not logged in, go to sign in
                startActivity(new Intent(SplashActivity.this, SignInActivity.class));
            }
            // Close splash activity
            finish();
        }, SPLASH_DELAY);
    }

    private boolean isUserLoggedIn() {
        SharedPreferences prefs = getSharedPreferences("JunoUserPrefs", MODE_PRIVATE);
        return prefs.getBoolean("isLoggedIn", false);
    }
} 