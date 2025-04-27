package com.example.juno;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.example.juno.utils.ThemeUtils;

/**
 * Base activity that all other activities should extend.
 * Handles theme and style application consistently across the app.
 */
public class BaseActivity extends AppCompatActivity {

    protected SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize preferences
        prefs = getSharedPreferences("JunoUserPrefs", MODE_PRIVATE);
        
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Apply font and layout settings to the current activity
        ViewGroup rootView = findViewById(android.R.id.content);
        if (rootView != null && rootView.getChildCount() > 0) {
            View mainContainer = rootView.getChildAt(0);
            if (mainContainer instanceof ViewGroup) {
                ThemeUtils.applyFontSizeToLayout((ViewGroup) mainContainer, this);
                ThemeUtils.applyLayoutStyle((ViewGroup) mainContainer, this);
            }
        }
    }
} 