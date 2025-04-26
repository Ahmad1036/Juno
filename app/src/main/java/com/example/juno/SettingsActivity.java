package com.example.juno;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private ImageButton backButton;
    private TextView userEmailText;
    private TextView editProfileButton;
    private TextView changePasswordButton;
    private TextView logoutButton;
    private Switch darkModeSwitch;
    private Switch notificationsSwitch;
    private TextView privacyPolicyButton;
    private TextView termsOfServiceButton;
    private TextView versionInfo;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize preferences
        prefs = getSharedPreferences("JunoUserPrefs", MODE_PRIVATE);
        
        // Initialize UI components
        initializeViews();
        
        // Load user data
        loadUserData();
        
        // Load settings
        loadSettings();
        
        // Set up listeners
        setupListeners();
    }
    
    private void initializeViews() {
        backButton = findViewById(R.id.back_button);
        userEmailText = findViewById(R.id.user_email_text);
        editProfileButton = findViewById(R.id.edit_profile_button);
        changePasswordButton = findViewById(R.id.change_password_button);
        logoutButton = findViewById(R.id.logout_button);
        darkModeSwitch = findViewById(R.id.dark_mode_switch);
        notificationsSwitch = findViewById(R.id.notifications_switch);
        privacyPolicyButton = findViewById(R.id.privacy_policy_button);
        termsOfServiceButton = findViewById(R.id.terms_of_service_button);
        versionInfo = findViewById(R.id.version_info);
        
        // Set app version
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            versionInfo.setText("Version " + version);
        } catch (PackageManager.NameNotFoundException e) {
            versionInfo.setText("Version 1.0.0");
        }
    }
    
    private void loadUserData() {
        String userEmail = prefs.getString("userEmail", "");
        if (!userEmail.isEmpty()) {
            userEmailText.setText(userEmail);
        } else {
            userEmailText.setText("Not signed in");
        }
    }
    
    private void loadSettings() {
        // Load dark mode setting
        boolean darkModeEnabled = prefs.getBoolean("darkModeEnabled", true);
        darkModeSwitch.setChecked(darkModeEnabled);
        
        // Load notifications setting
        boolean notificationsEnabled = prefs.getBoolean("notificationsEnabled", true);
        notificationsSwitch.setChecked(notificationsEnabled);
    }
    
    private void setupListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());
        
        // Edit Profile button
        editProfileButton.setOnClickListener(v -> {
            Toast.makeText(SettingsActivity.this, "Edit Profile coming soon", Toast.LENGTH_SHORT).show();
        });
        
        // Change Password button
        changePasswordButton.setOnClickListener(v -> {
            Toast.makeText(SettingsActivity.this, "Change Password coming soon", Toast.LENGTH_SHORT).show();
        });
        
        // Logout button
        logoutButton.setOnClickListener(v -> logout());
        
        // Dark Mode switch
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveDarkModeSetting(isChecked);
            Toast.makeText(SettingsActivity.this, 
                    isChecked ? "Dark mode enabled" : "Dark mode disabled", 
                    Toast.LENGTH_SHORT).show();
        });
        
        // Notifications switch
        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationsSetting(isChecked);
            Toast.makeText(SettingsActivity.this, 
                    isChecked ? "Notifications enabled" : "Notifications disabled", 
                    Toast.LENGTH_SHORT).show();
        });
        
        // Privacy Policy button
        privacyPolicyButton.setOnClickListener(v -> {
            Toast.makeText(SettingsActivity.this, "Privacy Policy coming soon", Toast.LENGTH_SHORT).show();
        });
        
        // Terms of Service button
        termsOfServiceButton.setOnClickListener(v -> {
            Toast.makeText(SettingsActivity.this, "Terms of Service coming soon", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void saveDarkModeSetting(boolean enabled) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("darkModeEnabled", enabled);
        editor.apply();
        
        // In a real app, you would change the app theme here
        // AppCompatDelegate.setDefaultNightMode(enabled ? 
        //        AppCompatDelegate.MODE_NIGHT_YES : 
        //        AppCompatDelegate.MODE_NIGHT_NO);
    }
    
    private void saveNotificationsSetting(boolean enabled) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("notificationsEnabled", enabled);
        editor.apply();
        
        // In a real app, you would register/unregister for notifications
    }
    
    private void logout() {
        // Clear user session data
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("isLoggedIn");
        editor.remove("userId");
        editor.remove("userName");
        editor.remove("userEmail");
        // Keep settings
        // editor.remove("darkModeEnabled");
        // editor.remove("notificationsEnabled");
        editor.apply();
        
        // Navigate to SignInActivity and clear back stack
        Intent intent = new Intent(SettingsActivity.this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
} 