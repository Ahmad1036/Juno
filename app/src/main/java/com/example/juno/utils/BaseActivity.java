package com.example.juno.utils;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.juno.utils.NetworkManager.NetworkChangeListener;

/**
 * Base activity for handling common functionality like offline data syncing
 */
public abstract class BaseActivity extends AppCompatActivity implements NetworkChangeListener {
    private static final String TAG = "BaseActivity";
    
    protected DataManager dataManager;
    protected ApiManager apiManager;
    private NetworkManager networkManager;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize managers
        dataManager = DataManager.getInstance(this);
        apiManager = ApiManager.getInstance(this);
        networkManager = new NetworkManager(this);
        
        // Set this activity as a network change listener
        networkManager.setNetworkChangeListener(this);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Check network status and sync if needed
        if (dataManager.isNetworkAvailable()) {
            syncData();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Clean up network manager
        if (networkManager != null) {
            networkManager.unregisterNetworkCallbacks();
        }
    }
    
    @Override
    public void onNetworkAvailable() {
        Log.d(TAG, "Network became available");
        runOnUiThread(() -> {
            Toast.makeText(this, "Back online. Syncing data...", Toast.LENGTH_SHORT).show();
            syncData();
        });
    }
    
    @Override
    public void onNetworkUnavailable() {
        Log.d(TAG, "Network became unavailable");
        runOnUiThread(() -> {
            Toast.makeText(this, "You are offline. Changes will be saved locally.", Toast.LENGTH_SHORT).show();
        });
    }
    
    /**
     * Sync data when back online
     * Override this in subclasses to provide specific syncing logic
     */
    protected void syncData() {
        dataManager.syncPendingOperations();
        apiManager.syncPendingRequests();
    }
    
    /**
     * Check if network is available
     */
    protected boolean isNetworkAvailable() {
        return dataManager.isNetworkAvailable();
    }
    
    /**
     * Show offline mode toast
     */
    protected void showOfflineToast() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "You are in offline mode. Data will be synchronized when you're back online.", Toast.LENGTH_SHORT).show();
        }
    }
} 