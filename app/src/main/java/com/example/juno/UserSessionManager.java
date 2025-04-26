package com.example.juno;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Manages user session data like user ID and preferences
 */
public class UserSessionManager {
    private static final String PREF_NAME = "JunoUserPrefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_NAME = "userName";
    
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Context context;
    
    public UserSessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }
    
    /**
     * Get current user ID, either from preferences or Firebase Auth
     */
    public String getUserId() {
        String userId = pref.getString(KEY_USER_ID, "");
        
        // If no user ID in preferences, try to get from Firebase Auth
        if (userId.isEmpty()) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                userId = currentUser.getUid();
                saveUserId(userId);
            }
        }
        
        return userId;
    }
    
    /**
     * Save user ID to preferences
     */
    public void saveUserId(String userId) {
        editor.putString(KEY_USER_ID, userId);
        editor.apply();
    }
    
    /**
     * Get user email
     */
    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, "");
    }
    
    /**
     * Save user email to preferences
     */
    public void saveUserEmail(String email) {
        editor.putString(KEY_USER_EMAIL, email);
        editor.apply();
    }
    
    /**
     * Get user name
     */
    public String getUserName() {
        return pref.getString(KEY_USER_NAME, "");
    }
    
    /**
     * Save user name to preferences
     */
    public void saveUserName(String name) {
        editor.putString(KEY_USER_NAME, name);
        editor.apply();
    }
    
    /**
     * Clear all user data from preferences
     */
    public void logout() {
        editor.clear();
        editor.apply();
        
        // Sign out from Firebase Auth
        FirebaseAuth.getInstance().signOut();
    }
} 