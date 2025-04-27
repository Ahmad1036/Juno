package com.example.juno.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.example.juno.services.SyncService;

/**
 * Broadcast receiver for network state changes
 * Used to trigger sync when network becomes available
 */
public class NetworkReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            boolean isNetworkAvailable = isNetworkAvailable(context);
            Log.d(TAG, "Network state changed. Available: " + isNetworkAvailable);
            
            if (isNetworkAvailable) {
                // Start sync service for the currently logged in user
                SharedPreferences prefs = context.getSharedPreferences("JunoUserPrefs", Context.MODE_PRIVATE);
                String userId = prefs.getString("userId", "");
                boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
                
                if (isLoggedIn && !userId.isEmpty()) {
                    Log.d(TAG, "Starting sync service for user: " + userId);
                    SyncService.startSync(context, userId);
                }
            }
        }
    }
    
    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
} 