package com.example.juno.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.juno.model.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Background service for syncing tasks with Firebase
 */
public class SyncService extends Service {
    private static final String TAG = "SyncService";
    private static final String PENDING_TASKS_KEY = "pending_tasks";
    
    private ExecutorService executorService;
    private DatabaseReference mDatabase;
    private SharedPreferences sharedPreferences;
    private Gson gson;
    
    @Override
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newSingleThreadExecutor();
        mDatabase = FirebaseDatabase.getInstance("https://juno-aa7d2-default-rtdb.firebaseio.com/").getReference();
        sharedPreferences = getSharedPreferences("JunoTasksPrefs", Context.MODE_PRIVATE);
        gson = new Gson();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("com.example.juno.action.SYNC_TASKS".equals(action)) {
                String userId = intent.getStringExtra("userId");
                if (userId != null && !userId.isEmpty()) {
                    syncTasks(userId);
                }
            }
        }
        return START_NOT_STICKY;
    }
    
    private void syncTasks(String userId) {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "No network available, can't sync tasks");
            stopSelf();
            return;
        }
        
        executorService.execute(() -> {
            Log.d(TAG, "Starting task sync for user: " + userId);
            List<Task> pendingTasks = getPendingTasks(userId);
            if (pendingTasks.isEmpty()) {
                Log.d(TAG, "No pending tasks to sync");
                stopSelf();
                return;
            }
            
            Log.d(TAG, "Found " + pendingTasks.size() + " tasks to sync");
            
            for (Task task : pendingTasks) {
                switch (task.getSyncStatus()) {
                    case 1: // Pending create
                        createTask(userId, task);
                        break;
                    case 2: // Pending update
                        updateTask(userId, task);
                        break;
                    case 3: // Pending delete
                        deleteTask(userId, task);
                        break;
                }
            }
            
            // Clear pending tasks after sync attempt
            sharedPreferences.edit().putString(PENDING_TASKS_KEY + "_" + userId, "").apply();
            Log.d(TAG, "Sync completed and pending tasks cleared");
            
            stopSelf();
        });
    }
    
    private void createTask(String userId, Task task) {
        Log.d(TAG, "Creating task: " + task.getTitle());
        mDatabase.child("users").child(userId).child("tasks").child(task.getId())
                .setValue(task.toMap())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Task created successfully: " + task.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to create task: " + e.getMessage()));
    }
    
    private void updateTask(String userId, Task task) {
        Log.d(TAG, "Updating task: " + task.getTitle());
        mDatabase.child("users").child(userId).child("tasks").child(task.getId())
                .setValue(task.toMap())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Task updated successfully: " + task.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update task: " + e.getMessage()));
    }
    
    private void deleteTask(String userId, Task task) {
        Log.d(TAG, "Deleting task: " + task.getId());
        mDatabase.child("users").child(userId).child("tasks").child(task.getId())
                .removeValue()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Task deleted successfully: " + task.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete task: " + e.getMessage()));
    }
    
    private List<Task> getPendingTasks(String userId) {
        String pendingTasksJson = sharedPreferences.getString(PENDING_TASKS_KEY + "_" + userId, "");
        if (pendingTasksJson.isEmpty()) {
            return new ArrayList<>();
        }
        
        Type type = new TypeToken<List<Task>>() {}.getType();
        List<Task> pendingTasks = gson.fromJson(pendingTasksJson, type);
        return pendingTasks != null ? pendingTasks : new ArrayList<>();
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    /**
     * Helper method to start this service
     */
    public static void startSync(Context context, String userId) {
        Intent intent = new Intent(context, SyncService.class);
        intent.setAction("com.example.juno.action.SYNC_TASKS");
        intent.putExtra("userId", userId);
        context.startService(intent);
    }
} 