package com.example.juno.repository;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.juno.model.Task;
import com.example.juno.utils.DataManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Repository for handling task operations with offline support
 */
public class TaskRepository {
    private static final String TAG = "TaskRepository";
    private static final String TASKS_KEY = "offline_tasks";
    
    // Callback interface for task operations
    public interface TaskCallback {
        void onTaskSaved(Task task);
        void onError(Exception e);
    }
    
    private final Context context;
    private final String userId;
    private final DatabaseReference mDatabase;
    private final DataManager dataManager;
    
    private final MutableLiveData<List<Task>> tasksLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    public TaskRepository(Context context, String userId) {
        this.context = context;
        this.userId = userId;
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
        this.dataManager = DataManager.getInstance(context);
        
        // Keep tasks offline for 30 days
        mDatabase.child("users").child(userId).child("tasks").keepSynced(true);
        
        // Load initial data
        loadTasks();
    }
    
    /**
     * Load tasks from both online and offline sources
     */
    public void loadTasks() {
        isLoading.setValue(true);
        
        // First load tasks from offline storage to show immediately
        List<Task> offlineTasks = getOfflineTasks();
        if (offlineTasks != null && !offlineTasks.isEmpty()) {
            tasksLiveData.setValue(offlineTasks);
            Toast.makeText(context, "Loading cached data...", Toast.LENGTH_SHORT).show();
        }
        
        if (dataManager.isNetworkAvailable()) {
            // Online mode - load from Firebase
            loadTasksFromFirebase();
        } else {
            // If we're offline, just use the offline tasks we already loaded
            isLoading.setValue(false);
            Toast.makeText(context, "Offline mode: Using locally saved data", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Create a new task
     */
    public void createTask(Task task) {
        // Generate local ID if not present
        if (task.getId() == null || task.getId().isEmpty()) {
            task.setId(UUID.randomUUID().toString());
        }
        
        // Set user ID
        task.setUserId(userId);
        
        if (dataManager.isNetworkAvailable()) {
            // Online mode - add to Firebase directly
            addTaskToFirebase(task);
        } else {
            // Offline mode - save locally with pending status
            task.setSyncStatus(1); // Pending create
            
            // Update the task list
            List<Task> currentTasks = tasksLiveData.getValue();
            if (currentTasks == null) {
                currentTasks = new ArrayList<>();
            }
            currentTasks.add(task);
            tasksLiveData.setValue(currentTasks);
            
            // Save to offline storage
            saveOfflineTasks(currentTasks);
            
            // Add to pending operations
            String path = "users/" + userId + "/tasks/" + task.getId();
            dataManager.saveDataWithSync(
                "task_" + task.getId(), 
                task, 
                path, 
                task.toMap()
            );
            
            Toast.makeText(context, "Task saved offline. Will sync when online.", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Update an existing task
     */
    public void updateTask(Task task) {
        if (dataManager.isNetworkAvailable()) {
            // Online mode - update in Firebase directly
            updateTaskInFirebase(task);
        } else {
            // Offline mode - update locally with pending status
            task.setSyncStatus(2); // Pending update
            
            // Update the task list
            List<Task> currentTasks = tasksLiveData.getValue();
            if (currentTasks != null) {
                for (int i = 0; i < currentTasks.size(); i++) {
                    if (currentTasks.get(i).getId().equals(task.getId())) {
                        currentTasks.set(i, task);
                        break;
                    }
                }
                tasksLiveData.setValue(currentTasks);
                
                // Save to offline storage
                saveOfflineTasks(currentTasks);
                
                // Add to pending operations
                String path = "users/" + userId + "/tasks/" + task.getId();
                dataManager.saveDataWithSync(
                    "task_" + task.getId(), 
                    task, 
                    path, 
                    task.toMap()
                );
                
                Toast.makeText(context, "Task updated offline. Will sync when online.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Delete a task
     */
    public void deleteTask(Task task) {
        if (dataManager.isNetworkAvailable()) {
            // Online mode - delete from Firebase directly
            deleteTaskFromFirebase(task);
        } else {
            // Offline mode - mark for deletion with pending status
            task.setSyncStatus(3); // Pending delete
            
            // Update the task list
            List<Task> currentTasks = tasksLiveData.getValue();
            if (currentTasks != null) {
                currentTasks.removeIf(t -> t.getId().equals(task.getId()));
                tasksLiveData.setValue(currentTasks);
                
                // Save to offline storage
                saveOfflineTasks(currentTasks);
                
                // Add to pending operations for deletion
                String path = "users/" + userId + "/tasks/" + task.getId();
                Map<String, Object> deleteMap = new HashMap<>();
                deleteMap.put(task.getId(), null); // Set to null to delete in Firebase
                
                dataManager.saveDataWithSync(
                    "delete_task_" + task.getId(), 
                    task.getId(), 
                    path, 
                    deleteMap
                );
                
                Toast.makeText(context, "Task deleted offline. Will sync when online.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Toggle task completion status
     */
    public void toggleTaskCompletion(Task task) {
        task.setCompleted(!task.isCompleted());
        updateTask(task);
    }
    
    /**
     * Check for network and sync pending tasks
     */
    public void syncIfNeeded() {
        if (dataManager.isNetworkAvailable()) {
            dataManager.syncPendingOperations();
        }
    }
    
    /**
     * Get observable live data of tasks
     */
    public LiveData<List<Task>> getTasksLiveData() {
        return tasksLiveData;
    }
    
    /**
     * Get loading state
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    /**
     * Get error messages
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Save task with callback
     */
    public void saveTask(Task task, TaskCallback callback) {
        if (task.getId() == null || task.getId().isEmpty()) {
            // New task
            task.setId(UUID.randomUUID().toString());
            task.setUserId(userId);
        }
        
        if (dataManager.isNetworkAvailable()) {
            // Online - save to Firebase
            DatabaseReference taskRef = mDatabase.child("users").child(userId).child("tasks").child(task.getId());
            
            taskRef.setValue(task.toMap())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Task saved to Firebase successfully");
                        saveTaskToOfflineStorage(task);
                        callback.onTaskSaved(task);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error saving task to Firebase", e);
                        task.setSyncStatus(task.getId() == null ? 1 : 2); // Create or update
                        saveTaskToOfflineStorage(task);
                        
                        // Add to pending operations
                        String path = "users/" + userId + "/tasks/" + task.getId();
                        dataManager.saveDataWithSync(
                            "task_" + task.getId(),
                            task,
                            path,
                            task.toMap()
                        );
                        
                        callback.onError(e);
                    });
        } else {
            // Offline - save locally
            task.setSyncStatus(task.getId() == null ? 1 : 2); // Create or update
            saveTaskToOfflineStorage(task);
            
            // Add to pending operations
            String path = "users/" + userId + "/tasks/" + task.getId();
            dataManager.saveDataWithSync(
                "task_" + task.getId(),
                task,
                path,
                task.toMap()
            );
            
            callback.onTaskSaved(task);
        }
    }
    
    // Private methods for internal operations
    
    /**
     * Load tasks from Firebase
     */
    private void loadTasksFromFirebase() {
        DatabaseReference tasksRef = mDatabase.child("users").child(userId).child("tasks");
        tasksRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Task> tasks = new ArrayList<>();
                
                for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                    try {
                        Task task = taskSnapshot.getValue(Task.class);
                        if (task != null) {
                            task.setId(taskSnapshot.getKey());
                            tasks.add(task);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing task: " + e.getMessage());
                    }
                }
                
                Log.d(TAG, "Loaded " + tasks.size() + " tasks from Firebase");
                tasksLiveData.setValue(tasks);
                saveOfflineTasks(tasks);
                isLoading.setValue(false);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Firebase load cancelled: " + databaseError.getMessage());
                errorMessage.setValue("Failed to load tasks: " + databaseError.getMessage());
                isLoading.setValue(false);
                
                // Fall back to offline data if Firebase load fails
                List<Task> offlineTasks = getOfflineTasks();
                if (offlineTasks != null && !offlineTasks.isEmpty()) {
                    tasksLiveData.setValue(offlineTasks);
                }
            }
        });
    }
    
    /**
     * Save task to Firebase
     */
    private void addTaskToFirebase(Task task) {
        DatabaseReference taskRef;
        if (task.getId() != null) {
            taskRef = mDatabase.child("users").child(userId).child("tasks").child(task.getId());
        } else {
            taskRef = mDatabase.child("users").child(userId).child("tasks").push();
            task.setId(taskRef.getKey());
        }
        
        taskRef.setValue(task.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task added to Firebase successfully");
                    // Update local cache
                    saveTaskToOfflineStorage(task);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding task to Firebase", e);
                    errorMessage.setValue("Failed to save task: " + e.getMessage());
                    
                    // Still save locally in case of failure
                    task.setSyncStatus(1); // Mark as pending create
                    saveTaskToOfflineStorage(task);
                    
                    // Add to pending operations
                    String path = "users/" + userId + "/tasks/" + task.getId();
                    dataManager.saveDataWithSync(
                        "task_" + task.getId(),
                        task,
                        path,
                        task.toMap()
                    );
                });
    }
    
    /**
     * Update task in Firebase
     */
    private void updateTaskInFirebase(Task task) {
        DatabaseReference taskRef = mDatabase.child("users").child(userId).child("tasks").child(task.getId());
        
        taskRef.updateChildren(task.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task updated in Firebase successfully");
                    // Update local cache
                    saveTaskToOfflineStorage(task);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating task in Firebase", e);
                    errorMessage.setValue("Failed to update task: " + e.getMessage());
                    
                    // Still save locally in case of failure
                    task.setSyncStatus(2); // Mark as pending update
                    saveTaskToOfflineStorage(task);
                    
                    // Add to pending operations
                    String path = "users/" + userId + "/tasks/" + task.getId();
                    dataManager.saveDataWithSync(
                        "task_" + task.getId(),
                        task,
                        path, 
                        task.toMap()
                    );
                });
    }
    
    /**
     * Delete task from Firebase
     */
    private void deleteTaskFromFirebase(Task task) {
        DatabaseReference taskRef = mDatabase.child("users").child(userId).child("tasks").child(task.getId());
        
        taskRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task deleted from Firebase successfully");
                    
                    // Update local cache by removing from task list
                    List<Task> currentTasks = tasksLiveData.getValue();
                    if (currentTasks != null) {
                        currentTasks.removeIf(t -> t.getId().equals(task.getId()));
                        tasksLiveData.setValue(currentTasks);
                        saveOfflineTasks(currentTasks);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting task from Firebase", e);
                    errorMessage.setValue("Failed to delete task: " + e.getMessage());
                    
                    // Mark as pending delete locally
                    task.setSyncStatus(3);
                    
                    // Add to pending operations for deletion
                    String path = "users/" + userId + "/tasks/" + task.getId();
                    Map<String, Object> deleteMap = new HashMap<>();
                    deleteMap.put(task.getId(), null); // Set to null to delete in Firebase
                    
                    dataManager.saveDataWithSync(
                        "delete_task_" + task.getId(),
                        task.getId(),
                        path,
                        deleteMap
                    );
                });
    }
    
    /**
     * Save a single task to offline storage
     */
    private void saveTaskToOfflineStorage(Task task) {
        List<Task> currentTasks = tasksLiveData.getValue();
        if (currentTasks == null) {
            currentTasks = new ArrayList<>();
        }
        
        // Replace or add the task
        boolean found = false;
        for (int i = 0; i < currentTasks.size(); i++) {
            if (currentTasks.get(i).getId().equals(task.getId())) {
                currentTasks.set(i, task);
                found = true;
                break;
            }
        }
        
        if (!found) {
            currentTasks.add(task);
        }
        
        tasksLiveData.setValue(currentTasks);
        saveOfflineTasks(currentTasks);
    }
    
    /**
     * Save all tasks to offline storage
     */
    private void saveOfflineTasks(List<Task> tasks) {
        dataManager.saveData(TASKS_KEY + "_" + userId, tasks);
    }
    
    /**
     * Get tasks from offline storage
     */
    private List<Task> getOfflineTasks() {
        return dataManager.getListData(TASKS_KEY + "_" + userId, Task.class);
    }
} 