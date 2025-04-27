package com.example.juno.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.example.juno.R;
import com.example.juno.SettingsActivity;

public class ThemeUtils {

    // Apply theme settings to activities
    public static void applyTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("JunoUserPrefs", Context.MODE_PRIVATE);
        
        // Apply dark mode
        boolean darkMode = SettingsActivity.isDarkModeEnabled(prefs);
        AppCompatDelegate.setDefaultNightMode(darkMode ? 
                AppCompatDelegate.MODE_NIGHT_YES : 
                AppCompatDelegate.MODE_NIGHT_NO);
    }
    
    // Apply font size to specific TextView
    public static void applyFontSize(TextView textView, Context context) {
        SharedPreferences prefs = context.getSharedPreferences("JunoUserPrefs", Context.MODE_PRIVATE);
        int fontSize = SettingsActivity.getFontSize(prefs);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
    }
    
    // Apply font size to all TextViews in a layout recursively
    public static void applyFontSizeToLayout(ViewGroup layout, Context context) {
        SharedPreferences prefs = context.getSharedPreferences("JunoUserPrefs", Context.MODE_PRIVATE);
        int fontSize = SettingsActivity.getFontSize(prefs);
        
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
            } else if (child instanceof ViewGroup) {
                applyFontSizeToLayout((ViewGroup) child, context);
            }
        }
    }
    
    // Apply layout style (compact or comfortable)
    public static void applyLayoutStyle(ViewGroup layout, Context context) {
        SharedPreferences prefs = context.getSharedPreferences("JunoUserPrefs", Context.MODE_PRIVATE);
        String layoutStyle = SettingsActivity.getLayoutStyle(prefs);
        
        // Set padding based on layout style
        int padding = layoutStyle.equals("compact") ? 
                context.getResources().getDimensionPixelSize(R.dimen.padding_compact) : 
                context.getResources().getDimensionPixelSize(R.dimen.padding_comfortable);
        
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof ViewGroup) {
                child.setPadding(padding, padding, padding, padding);
                applyLayoutStyle((ViewGroup) child, context);
            }
        }
    }
    
    // Check if the current theme is dark mode
    public static boolean isDarkTheme(Context context) {
        return (context.getResources().getConfiguration().uiMode & 
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }
    
    // Get appropriate color based on current theme
    public static int getThemeColor(Context context, int lightColorRes, int darkColorRes) {
        return isDarkTheme(context) ? 
                ContextCompat.getColor(context, darkColorRes) : 
                ContextCompat.getColor(context, lightColorRes);
    }
} 