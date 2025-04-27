package com.example.juno;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.juno.utils.DataManager;
import com.example.juno.utils.NotificationUtils;
import com.example.juno.utils.ThemeUtils;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Application class for Juno
 * Handles global initialization like Firebase persistence and offline data management
 */
public class JunoApplication extends Application implements SharedPreferences.OnSharedPreferenceChangeListener, Application.ActivityLifecycleCallbacks {
    private static final String TAG = "JunoApplication";
    private DataManager dataManager;
    private List<Activity> activities = new ArrayList<>();
    private SharedPreferences prefs;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Register activity lifecycle callbacks to track active activities
        registerActivityLifecycleCallbacks(this);
        
        // Initialize preferences
        prefs = getSharedPreferences("JunoUserPrefs", MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(this);
        
        // Initialize Firebase persistence only once at app startup
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            Log.d(TAG, "Firebase persistence enabled successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error enabling Firebase persistence: " + e.getMessage());
        }
        
        // Initialize DataManager
        dataManager = DataManager.getInstance(this);
        Log.d(TAG, "DataManager initialized");
        
        // Apply dark mode
        boolean darkMode = SettingsActivity.isDarkModeEnabled(prefs);
        AppCompatDelegate.setDefaultNightMode(darkMode ? 
                AppCompatDelegate.MODE_NIGHT_YES : 
                AppCompatDelegate.MODE_NIGHT_NO);
        
        // Create notification channel
        NotificationUtils.createNotificationChannel(this);
        
        // Apply font scaling
        adjustFontScale(prefs);
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (SettingsActivity.KEY_DARK_MODE.equals(key)) {
            // Apply dark mode
            boolean darkMode = SettingsActivity.isDarkModeEnabled(prefs);
            AppCompatDelegate.setDefaultNightMode(darkMode ? 
                    AppCompatDelegate.MODE_NIGHT_YES : 
                    AppCompatDelegate.MODE_NIGHT_NO);
            
            // Force all activities to recreate
            for (Activity activity : activities) {
                if (!activity.isFinishing()) {
                    activity.recreate();
                }
            }
        } else if (SettingsActivity.KEY_FONT_SIZE.equals(key)) {
            // Apply font scaling
            adjustFontScale(prefs);
            
            // Force all activities to recreate
            for (Activity activity : activities) {
                if (!activity.isFinishing()) {
                    activity.recreate();
                }
            }
        } else if (SettingsActivity.KEY_LAYOUT_STYLE.equals(key)) {
            // Force all activities to recreate
            for (Activity activity : activities) {
                if (!activity.isFinishing()) {
                    activity.recreate();
                }
            }
        }
    }
    
    private void adjustFontScale(SharedPreferences prefs) {
        // Get font size from preferences
        int fontSize = SettingsActivity.getFontSize(prefs);
        
        // Calculate the font scale factor
        float fontScale = fontSize / 16f; // 16sp is our baseline
        
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        
        // Apply font scale
        configuration.fontScale = fontScale;
        
        DisplayMetrics metrics = resources.getDisplayMetrics();
        resources.updateConfiguration(configuration, metrics);
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        
        // Unregister as shared preference listener
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        
        // Unregister activity lifecycle callbacks
        unregisterActivityLifecycleCallbacks(this);
        
        // Clean up DataManager resources
        if (dataManager != null) {
            dataManager.cleanup();
            Log.d(TAG, "DataManager resources cleaned up");
        }
    }

    // ActivityLifecycleCallbacks implementation
    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        activities.add(activity);
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        activities.remove(activity);
    }
} 