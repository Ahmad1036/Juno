package com.example.juno.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central manager for handling offline data storage and synchronization
 */
public class DataManager {
    private static final String TAG = "DataManager";
    
    // SharedPreferences keys
    private static final String PREFS_NAME = "JunoOfflineData";
    private static final String PENDING_OPERATIONS_KEY = "pending_operations";
    
    // Singleton instance
    private static DataManager instance;
    
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    private final NetworkManager networkManager;
    private final DatabaseReference firebaseDatabase;
    
    // Private constructor for singleton pattern
    private DataManager(Context context) {
        this.context = context.getApplicationContext();
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.networkManager = new NetworkManager(context);
        this.firebaseDatabase = FirebaseDatabase.getInstance().getReference();
        
        // Set up network change listener
        networkManager.setNetworkChangeListener(new NetworkManager.NetworkChangeListener() {
            @Override
            public void onNetworkAvailable() {
                Log.d(TAG, "Network available - syncing pending operations");
                syncPendingOperations();
                showNetworkStatusToast(true);
            }
            
            @Override
            public void onNetworkUnavailable() {
                Log.d(TAG, "Network unavailable - switching to offline mode");
                showNetworkStatusToast(false);
            }
        });
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized DataManager getInstance(Context context) {
        if (instance == null) {
            instance = new DataManager(context);
        }
        return instance;
    }
    
    /**
     * Check if network is available
     */
    public boolean isNetworkAvailable() {
        return networkManager.isNetworkAvailable();
    }
    
    /**
     * Save data to SharedPreferences
     */
    public <T> void saveData(String key, T data) {
        String json = gson.toJson(data);
        sharedPreferences.edit().putString(key, json).apply();
        Log.d(TAG, "Saved data for key: " + key);
    }
    
    /**
     * Save data to SharedPreferences and attempt to sync if online
     */
    public <T> void saveDataWithSync(String key, T data, String firebasePath, Map<String, Object> updateMap) {
        // Save locally first
        saveData(key, data);
        
        // If online, update Firebase directly
        if (isNetworkAvailable()) {
            updateFirebase(firebasePath, updateMap);
        } else {
            // Store the operation to perform when back online
            addPendingOperation(firebasePath, updateMap);
        }
    }
    
    /**
     * Get data from SharedPreferences
     */
    public <T> T getData(String key, Class<T> classType) {
        String json = sharedPreferences.getString(key, null);
        if (json == null) {
            return null;
        }
        return gson.fromJson(json, classType);
    }
    
    /**
     * Get list data from SharedPreferences
     */
    public <T> List<T> getListData(String key, Class<T> classType) {
        String json = sharedPreferences.getString(key, null);
        if (json == null) {
            return new ArrayList<>();
        }
        
        Type listType = TypeToken.getParameterized(ArrayList.class, classType).getType();
        return gson.fromJson(json, listType);
    }
    
    /**
     * Update data in Firebase
     */
    private void updateFirebase(String path, Map<String, Object> updateMap) {
        DatabaseReference reference = firebaseDatabase.child(path);
        reference.updateChildren(updateMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Firebase update successful for: " + path))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase update failed for: " + path, e);
                    // Save as pending operation since it failed
                    addPendingOperation(path, updateMap);
                });
    }
    
    /**
     * Store pending operation for later synchronization
     */
    private void addPendingOperation(String path, Map<String, Object> updateMap) {
        List<PendingOperation> pendingOperations = getPendingOperations();
        pendingOperations.add(new PendingOperation(path, updateMap));
        String json = gson.toJson(pendingOperations);
        sharedPreferences.edit().putString(PENDING_OPERATIONS_KEY, json).apply();
        Log.d(TAG, "Added pending operation for path: " + path);
    }
    
    /**
     * Get all pending operations
     */
    private List<PendingOperation> getPendingOperations() {
        String json = sharedPreferences.getString(PENDING_OPERATIONS_KEY, null);
        if (json == null) {
            return new ArrayList<>();
        }
        
        Type listType = TypeToken.getParameterized(ArrayList.class, PendingOperation.class).getType();
        return gson.fromJson(json, listType);
    }
    
    /**
     * Synchronize all pending operations with Firebase
     */
    public void syncPendingOperations() {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Cannot sync, network not available");
            return;
        }
        
        List<PendingOperation> pendingOperations = getPendingOperations();
        if (pendingOperations.isEmpty()) {
            Log.d(TAG, "No pending operations to sync");
            return;
        }
        
        Log.d(TAG, "Syncing " + pendingOperations.size() + " pending operations");
        
        // Create a copy of the list to avoid concurrent modification
        List<PendingOperation> operationsToProcess = new ArrayList<>(pendingOperations);
        
        // Clear pending operations immediately to avoid duplicate processing
        sharedPreferences.edit().remove(PENDING_OPERATIONS_KEY).apply();
        
        // Process each operation
        for (PendingOperation operation : operationsToProcess) {
            updateFirebase(operation.getPath(), operation.getUpdateMap());
        }
    }
    
    /**
     * Show toast message about network status
     */
    private void showNetworkStatusToast(boolean isOnline) {
        String message = isOnline ? "Back online. Syncing data..." : "You are offline. Changes will be saved locally.";
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        networkManager.unregisterNetworkCallbacks();
    }
    
    /**
     * Class to represent a pending operation
     */
    private static class PendingOperation {
        private final String path;
        private final Map<String, Object> updateMap;
        
        public PendingOperation(String path, Map<String, Object> updateMap) {
            this.path = path;
            this.updateMap = updateMap;
        }
        
        public String getPath() {
            return path;
        }
        
        public Map<String, Object> getUpdateMap() {
            return updateMap;
        }
    }
} 