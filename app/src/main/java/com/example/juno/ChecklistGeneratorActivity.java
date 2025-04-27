package com.example.juno;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.juno.adapter.TaskSelectionAdapter;
import com.example.juno.model.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChecklistGeneratorActivity extends BaseActivity {

    private static final String TAG = "ChecklistGeneratorActivity";
    
    private ImageView backButton;
    private TextView titleText;
    private TextView subtitleText;
    private RecyclerView taskRecyclerView;
    private Button generateButton;
    private ProgressBar progressBar;
    private TextView noTasksText;
    
    private TaskSelectionAdapter adapter;
    private List<Task> incompleteTasks;
    private List<Task> selectedTasks;
    
    private String userId;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checklist_generator);
        
        // Check user session
        if (!checkUserSession()) {
            finish();
            return;
        }
        
        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        // Initialize UI components
        backButton = findViewById(R.id.back_button);
        titleText = findViewById(R.id.title_text);
        subtitleText = findViewById(R.id.subtitle_text);
        taskRecyclerView = findViewById(R.id.task_recycler_view);
        generateButton = findViewById(R.id.generate_button);
        progressBar = findViewById(R.id.progress_bar);
        noTasksText = findViewById(R.id.no_tasks_text);
        
        // Set up task list
        incompleteTasks = new ArrayList<>();
        selectedTasks = new ArrayList<>();
        
        // Set up RecyclerView
        adapter = new TaskSelectionAdapter(this, incompleteTasks, this::onTaskSelectionChanged);
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskRecyclerView.setAdapter(adapter);
        
        // Set up click listeners
        backButton.setOnClickListener(v -> finish());
        
        generateButton.setOnClickListener(v -> generateChecklist());
        
        // Load incomplete tasks
        loadIncompleteTasks();
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
    
    private void loadIncompleteTasks() {
        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        noTasksText.setVisibility(View.GONE);
        generateButton.setEnabled(false);
        
        // Query for incomplete tasks
        mDatabase.child("users").child(userId).child("tasks")
                .orderByChild("completed")
                .equalTo(false)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        incompleteTasks.clear();
                        
                        for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                            Task task = taskSnapshot.getValue(Task.class);
                            if (task != null) {
                                task.setId(taskSnapshot.getKey());
                                incompleteTasks.add(task);
                            }
                        }
                        
                        // Update UI
                        progressBar.setVisibility(View.GONE);
                        
                        if (incompleteTasks.isEmpty()) {
                            noTasksText.setVisibility(View.VISIBLE);
                            taskRecyclerView.setVisibility(View.GONE);
                        } else {
                            noTasksText.setVisibility(View.GONE);
                            taskRecyclerView.setVisibility(View.VISIBLE);
                            adapter.notifyDataSetChanged();
                        }
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        progressBar.setVisibility(View.GONE);
                        noTasksText.setText("Failed to load tasks");
                        noTasksText.setVisibility(View.VISIBLE);
                        taskRecyclerView.setVisibility(View.GONE);
                    }
                });
    }
    
    private void onTaskSelectionChanged(Task task, boolean isSelected) {
        if (isSelected) {
            selectedTasks.add(task);
        } else {
            selectedTasks.remove(task);
        }
        
        // Enable/disable generate button based on selection
        generateButton.setEnabled(!selectedTasks.isEmpty());
    }
    
    private void generateChecklist() {
        if (selectedTasks.isEmpty()) {
            Toast.makeText(this, "Please select at least one task", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        generateButton.setEnabled(false);
        
        // Prepare the task data
        List<String> taskDescriptions = new ArrayList<>();
        Map<String, Task> taskMap = new HashMap<>();
        
        for (Task task : selectedTasks) {
            taskDescriptions.add(task.getTitle());
            taskMap.put(task.getId(), task);
        }
        
        // Open ChecklistResultActivity with selected tasks
        // Pass the selected tasks to the next activity for processing with Gemini
        Bundle bundle = new Bundle();
        bundle.putStringArrayList("taskDescriptions", new ArrayList<>(taskDescriptions));
        bundle.putStringArrayList("taskIds", new ArrayList<>(taskMap.keySet()));
        
        // Navigate to result screen
        android.content.Intent intent = new android.content.Intent(this, ChecklistResultActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
        
        // Reset loading state
        progressBar.setVisibility(View.GONE);
        generateButton.setEnabled(true);
    }
} 