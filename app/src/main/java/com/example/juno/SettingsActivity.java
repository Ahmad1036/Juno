package com.example.juno;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;

public class SettingsActivity extends BaseActivity {

    private ImageView backButton;
    private TextView userEmailText;
    private TextView editProfileButton;
    private TextView changePasswordButton;
    private TextView logoutButton;
    private Switch darkModeSwitch;
    private SeekBar fontSizeSeekBar;
    private TextView fontSizeValueText;
    private RadioGroup layoutStyleGroup;
    private RadioButton compactLayoutRadio;
    private RadioButton comfortableLayoutRadio;
    private Spinner notificationStyleSpinner;
    private Switch notificationsEnabledSwitch;
    private TextView privacyPolicyButton;
    private TextView termsOfServiceButton;
    private TextView versionInfo;

    private SharedPreferences prefs;

    // Settings keys
    public static final String KEY_DARK_MODE = "dark_mode";
    public static final String KEY_FONT_SIZE = "font_size";
    public static final String KEY_LAYOUT_STYLE = "layout_style";
    public static final String KEY_NOTIFICATION_STYLE = "notification_style";
    public static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    
    // Default values
    private static final boolean DEFAULT_DARK_MODE = true;
    private static final int DEFAULT_FONT_SIZE = 16;
    private static final String DEFAULT_LAYOUT_STYLE = "comfortable";
    private static final String DEFAULT_NOTIFICATION_STYLE = "standard";
    private static final boolean DEFAULT_NOTIFICATIONS_ENABLED = true;

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
        
        // Load saved preferences
        loadPreferences();
        
        // Set up listeners
        setupListeners();
    }
    
    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        userEmailText = findViewById(R.id.accountEmail);
        editProfileButton = findViewById(R.id.edit_profile_button);
        changePasswordButton = findViewById(R.id.change_password_button);
        logoutButton = findViewById(R.id.logoutButton);
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        fontSizeSeekBar = findViewById(R.id.fontSizeSeekBar);
        fontSizeValueText = findViewById(R.id.fontSizeValueText);
        layoutStyleGroup = findViewById(R.id.layoutStyleGroup);
        compactLayoutRadio = findViewById(R.id.compactLayoutRadio);
        comfortableLayoutRadio = findViewById(R.id.comfortableLayoutRadio);
        notificationStyleSpinner = findViewById(R.id.notificationStyleSpinner);
        notificationsEnabledSwitch = findViewById(R.id.notificationsEnabledSwitch);
        privacyPolicyButton = findViewById(R.id.privacyPolicyTextView);
        termsOfServiceButton = findViewById(R.id.termsOfServiceTextView);
        versionInfo = findViewById(R.id.appVersionTextView);
        
        // Set app version
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            versionInfo.setText("Version " + version);
        } catch (PackageManager.NameNotFoundException e) {
            versionInfo.setText("Version 1.0.0");
        }
        
        // Set up notification style spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.notification_styles, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        notificationStyleSpinner.setAdapter(adapter);
    }
    
    private void loadUserData() {
        String userEmail = prefs.getString("userEmail", "");
        if (!userEmail.isEmpty()) {
            userEmailText.setText(userEmail);
        } else {
            userEmailText.setText("Not signed in");
        }
    }
    
    private void loadPreferences() {
        // Load dark mode setting
        boolean darkMode = prefs.getBoolean(KEY_DARK_MODE, DEFAULT_DARK_MODE);
        darkModeSwitch.setChecked(darkMode);
        
        // Load font size setting
        int fontSize = prefs.getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
        fontSizeSeekBar.setProgress(fontSize - 12); // Adjust for minimum font size of 12
        fontSizeValueText.setText(fontSize + " sp");
        
        // Load layout style setting
        String layoutStyle = prefs.getString(KEY_LAYOUT_STYLE, DEFAULT_LAYOUT_STYLE);
        if (layoutStyle.equals("compact")) {
            compactLayoutRadio.setChecked(true);
        } else {
            comfortableLayoutRadio.setChecked(true);
        }
        
        // Load notification style setting
        String notificationStyle = prefs.getString(KEY_NOTIFICATION_STYLE, DEFAULT_NOTIFICATION_STYLE);
        int spinnerPosition = ((ArrayAdapter) notificationStyleSpinner.getAdapter())
                .getPosition(notificationStyle);
        if (spinnerPosition >= 0) {
            notificationStyleSpinner.setSelection(spinnerPosition);
        }
        
        // Load notifications enabled setting
        boolean notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, 
                DEFAULT_NOTIFICATIONS_ENABLED);
        notificationsEnabledSwitch.setChecked(notificationsEnabled);
    }
    
    private void setupListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());
        
        // Edit Profile button
        editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, EditProfileActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        
        // Change Password button
        changePasswordButton.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        
        // Logout button
        logoutButton.setOnClickListener(v -> logout());
        
        // Dark mode switch
        darkModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                saveDarkModeSetting(isChecked);
                applyDarkMode(isChecked);
            }
        });
        
        // Font size seekbar
        fontSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int fontSize = progress + 12; // Add 12 to get font size between 12sp and 24sp
                fontSizeValueText.setText(fontSize + " sp");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not needed
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int fontSize = seekBar.getProgress() + 12;
                saveFontSizeSetting(fontSize);
                applyFontSize(fontSize);
            }
        });
        
        // Layout style radio group
        layoutStyleGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                String layoutStyle;
                if (checkedId == R.id.compactLayoutRadio) {
                    layoutStyle = "compact";
                } else {
                    layoutStyle = "comfortable";
                }
                saveLayoutStyleSetting(layoutStyle);
                applyLayoutStyle(layoutStyle);
            }
        });
        
        // Notification style spinner
        notificationStyleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String notificationStyle = parent.getItemAtPosition(position).toString();
                saveNotificationStyleSetting(notificationStyle);
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Not needed
            }
        });
        
        // Notifications enabled switch
        notificationsEnabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                saveNotificationsEnabledSetting(isChecked);
                toggleNotificationOptions(isChecked);
            }
        });
        
        // Privacy Policy button
        privacyPolicyButton.setOnClickListener(v -> {
            Toast.makeText(SettingsActivity.this, "Privacy Policy coming soon", Toast.LENGTH_SHORT).show();
        });
        
        // Terms of Service button
        termsOfServiceButton.setOnClickListener(v -> {
            Toast.makeText(SettingsActivity.this, "Terms of Service coming soon", Toast.LENGTH_SHORT).show();
        });
        
        // Smart Reminders & Nudges
        View smartRemindersView = findViewById(R.id.smart_reminders_setting);
        if (smartRemindersView != null) {
            smartRemindersView.setOnClickListener(v -> openReminderSettings());
        }
    }
    
    private void saveDarkModeSetting(boolean darkMode) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_DARK_MODE, darkMode);
        editor.apply();
    }
    
    private void saveFontSizeSetting(int fontSize) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_FONT_SIZE, fontSize);
        editor.apply();
    }
    
    private void saveLayoutStyleSetting(String layoutStyle) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_LAYOUT_STYLE, layoutStyle);
        editor.apply();
    }
    
    private void saveNotificationStyleSetting(String notificationStyle) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_NOTIFICATION_STYLE, notificationStyle);
        editor.apply();
    }
    
    private void saveNotificationsEnabledSetting(boolean enabled) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled);
        editor.apply();
    }
    
    private void applyDarkMode(boolean darkMode) {
        // Save setting and let the application handle the theme change
        // The app will handle recreation of all activities via SharedPreferences listener
        if (darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
    
    private void applyFontSize(int fontSize) {
        // Font size changes will be applied when activities are recreated
        // This would normally require a custom application class to handle global font scaling
    }
    
    private void applyLayoutStyle(String layoutStyle) {
        // Layout style changes will be applied when activities are recreated
    }
    
    private void toggleNotificationOptions(boolean enabled) {
        notificationStyleSpinner.setEnabled(enabled);
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
    
    /**
     * Static utility methods for other activities to access settings
     */
    public static boolean isDarkModeEnabled(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_DARK_MODE, DEFAULT_DARK_MODE);
    }
    
    public static int getFontSize(SharedPreferences prefs) {
        return prefs.getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
    }
    
    public static String getLayoutStyle(SharedPreferences prefs) {
        return prefs.getString(KEY_LAYOUT_STYLE, DEFAULT_LAYOUT_STYLE);
    }
    
    public static String getNotificationStyle(SharedPreferences prefs) {
        return prefs.getString(KEY_NOTIFICATION_STYLE, DEFAULT_NOTIFICATION_STYLE);
    }
    
    public static boolean areNotificationsEnabled(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, DEFAULT_NOTIFICATIONS_ENABLED);
    }

    // Add this method to open the ReminderSettingsActivity
    private void openReminderSettings() {
        Intent intent = new Intent(this, ReminderSettingsActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
} 