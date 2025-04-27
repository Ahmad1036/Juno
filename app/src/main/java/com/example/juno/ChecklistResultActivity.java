package com.example.juno;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.juno.adapter.SubtaskAdapter;
import com.example.juno.model.Task;
import com.example.juno.utils.BaseActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.gms.tasks.OnCompleteListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChecklistResultActivity extends BaseActivity {

    private static final String TAG = "ChecklistResultActivity";
    
    private ImageView backButton;
    private TextView titleText;
    private TextView subtitleText;
    private RecyclerView subtaskRecyclerView;
    private Button saveButton;
    private ProgressBar progressBar;
    private TextView loadingText;
    
    private SubtaskAdapter adapter;
    private List<Task> subtasks;
    
    private ArrayList<String> taskDescriptions;
    private ArrayList<String> taskIds;
    private String userId;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checklist_result);
        
        // Check user session
        if (!checkUserSession()) {
            finish();
            return;
        }
        
        // Get task descriptions from intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            taskDescriptions = extras.getStringArrayList("taskDescriptions");
            taskIds = extras.getStringArrayList("taskIds");
        }
        
        if (taskDescriptions == null || taskDescriptions.isEmpty()) {
            Toast.makeText(this, "No tasks selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        // Initialize UI components
        backButton = findViewById(R.id.back_button);
        titleText = findViewById(R.id.title_text);
        subtitleText = findViewById(R.id.subtitle_text);
        subtaskRecyclerView = findViewById(R.id.subtask_recycler_view);
        saveButton = findViewById(R.id.save_button);
        progressBar = findViewById(R.id.progress_bar);
        loadingText = findViewById(R.id.loading_text);
        
        // Set up subtask list
        subtasks = new ArrayList<>();
        
        // Set up RecyclerView
        adapter = new SubtaskAdapter(this, subtasks);
        subtaskRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        subtaskRecyclerView.setAdapter(adapter);
        
        // Set up click listeners
        backButton.setOnClickListener(v -> finish());
        
        saveButton.setOnClickListener(v -> saveSubtasks());
        
        // Show loading state
        showLoading(true);
        
        // Generate subtasks using Gemini
        generateSubtasks();
    }
    
    private boolean checkUserSession() {
        SharedPreferences prefs = getSharedPreferences("JunoUserPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        
        if (isLoggedIn) {
            userId = prefs.getString("userId", "");
            return true;
        }
        
        return false;
    }
    
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            loadingText.setVisibility(View.VISIBLE);
            subtaskRecyclerView.setVisibility(View.GONE);
            saveButton.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            loadingText.setVisibility(View.GONE);
            subtaskRecyclerView.setVisibility(View.VISIBLE);
            saveButton.setEnabled(true);
        }
    }
    
    private void generateSubtasks() {
        // Create task/subtask mapping
        Map<String, String> taskIdToTitle = new HashMap<>();
        Map<String, List<Task>> subtasksByParent = new HashMap<>();
        
        // Track pending operations to know when all are complete
        final int[] pendingTasks = {taskIds.size()};
        
        // Process each selected task
        for (int i = 0; i < taskIds.size(); i++) {
            final String taskId = taskIds.get(i);
            final String taskTitle = taskDescriptions.get(i);
            taskIdToTitle.put(taskId, taskTitle);
            
            // Create the parent task entry
            Task parentTask = new Task();
            parentTask.setTitle(taskTitle);
            parentTask.setPriority(Task.PRIORITY_MEDIUM);
            parentTask.setCompleted(false);
            parentTask.setId(taskId); // Use real task ID
            
            // Add parent task to the display list
            subtasks.add(parentTask);
            
            // Generate subtasks using Gemini API
            final int parentIndex = subtasks.size() - 1; // Track where we inserted the parent
            
            GeminiChecklistGenerator.generateSubtasksAsync(taskTitle, null, subtasksList -> {
                // Prepare the subtasks for this parent
                List<Task> generatedSubtasks = new ArrayList<>();
                
                for (String subtaskTitle : subtasksList) {
                    Task subtask = new Task();
                    subtask.setTitle(subtaskTitle);
                    subtask.setPriority(Task.PRIORITY_MEDIUM);
                    subtask.setCompleted(false);
                    subtask.setParentId(taskId); // Link to the actual task ID
                    subtask.setUserId(userId);
                    
                    generatedSubtasks.add(subtask);
                }
                
                // Store for saving later
                subtasksByParent.put(taskId, generatedSubtasks);
                
                // Add to display list right after the parent
                runOnUiThread(() -> {
                    // Insert all subtasks after the parent task
                    subtasks.addAll(parentIndex + 1, generatedSubtasks);
                    adapter.notifyDataSetChanged();
                    
                    // Decrease pending counter and check if all tasks are processed
                    pendingTasks[0]--;
                    if (pendingTasks[0] <= 0) {
                        showLoading(false);
                    }
                });
            });
        }
    }
    
    private void saveSubtasks() {
        // Show loading state
        showLoading(true);
        loadingText.setText("Saving subtasks...");
        
        // Map to store tasks
        Map<String, Object> taskUpdates = new HashMap<>();
        
        // Process each subtask
        for (Task subtask : subtasks) {
            // Skip parent tasks (which don't have parentId)
            if (subtask.getParentId() == null || subtask.getParentId().isEmpty()) {
                continue;
            }
            
            // Create a new ID for this subtask
            String newTaskId = UUID.randomUUID().toString();
            subtask.setId(newTaskId);
            subtask.setUserId(userId);
            
            // Add to update map
            taskUpdates.put("/users/" + userId + "/tasks/" + newTaskId, subtask.toMap());
        }
        
        // Save all tasks at once
        mDatabase.updateChildren(taskUpdates)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    
                    if (task.isSuccessful()) {
                        Toast.makeText(ChecklistResultActivity.this, 
                                "Subtasks saved successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(ChecklistResultActivity.this,
                                "Failed to save subtasks", Toast.LENGTH_SHORT).show();
                    }
                });
    }
} 