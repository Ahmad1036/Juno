package com.example.juno;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.juno.adapter.TaskAdapter;
import com.example.juno.model.Task;
import com.example.juno.repository.TaskRepository;
import com.example.juno.utils.BaseActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class TasksActivity extends BaseActivity {

    private static final String TAG = "TasksActivity";

    private RecyclerView tasksRecyclerView;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private FloatingActionButton addTaskFab;
    private ImageButton backButton;
    private TextView emptyStateText;
    private ProgressBar progressBar;
    
    private String userId;
    private TaskRepository taskRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        // Check if user is logged in
        SharedPreferences prefs = getSharedPreferences("JunoUserPrefs", MODE_PRIVATE);
        userId = prefs.getString("userId", "");
        if (userId.isEmpty()) {
            // User not logged in, redirect to SignInActivity
            startActivity(new Intent(TasksActivity.this, SignInActivity.class));
            finish();
            return;
        }

        // Initialize UI components
        tasksRecyclerView = findViewById(R.id.tasks_recycler_view);
        addTaskFab = findViewById(R.id.add_task_fab);
        backButton = findViewById(R.id.back_button);
        emptyStateText = findViewById(R.id.empty_state_text);
        progressBar = findViewById(R.id.progress_bar);
        
        if (progressBar == null) {
            // Create progress bar if not in layout
            progressBar = new ProgressBar(this);
            progressBar.setId(View.generateViewId());
            progressBar.setVisibility(View.GONE);
            
            // Add to root view
            findViewById(android.R.id.content).post(() -> {
                View root = findViewById(android.R.id.content);
                if (root instanceof android.view.ViewGroup) {
                    ((android.view.ViewGroup) root).addView(progressBar);
                }
            });
        }

        // Set up RecyclerView
        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskList, new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(int position) {
                onTaskClicked(taskList.get(position));
            }

            @Override
            public void onCheckBoxClick(int position, boolean isChecked) {
                onTaskCheckToggled(taskList.get(position), isChecked);
            }

            @Override
            public void onTaskCheckboxClicked(Task task) {
                onTaskCheckToggled(task, task.isCompleted());
            }
        });
        
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tasksRecyclerView.setAdapter(taskAdapter);

        // Set up click listeners
        addTaskFab.setOnClickListener(v -> {
            // Launch CreateTaskActivity
            Intent intent = new Intent(TasksActivity.this, CreateTaskActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        backButton.setOnClickListener(v -> finish());
        
        // Initialize Task Repository
        taskRepository = new TaskRepository(this, userId);
        setupRepositoryObservers();
        
        // Show network status
        showNetworkStatus(isNetworkAvailable());
    }
    
    private void setupRepositoryObservers() {
        // Observe tasks
        taskRepository.getTasksLiveData().observe(this, tasks -> {
            taskList.clear();
            if (tasks != null) {
                taskList.addAll(tasks);
            }
            taskAdapter.notifyDataSetChanged();
            
            // Show/hide empty state
            if (taskList.isEmpty()) {
                emptyStateText.setVisibility(View.VISIBLE);
                emptyStateText.setText("No tasks found. Add a task to get started!");
                tasksRecyclerView.setVisibility(View.GONE);
            } else {
                emptyStateText.setVisibility(View.GONE);
                tasksRecyclerView.setVisibility(View.VISIBLE);
            }
        });
        
        // Observe loading state
        taskRepository.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                progressBar.setVisibility(View.VISIBLE);
                emptyStateText.setText("Loading tasks...");
                emptyStateText.setVisibility(View.VISIBLE);
                tasksRecyclerView.setVisibility(View.GONE);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        });
        
        // Observe error messages
        taskRepository.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh tasks
        taskRepository.loadTasks();
    }
    
    private void onTaskClicked(Task task) {
        // Navigate to TaskDetailActivity
        Intent intent = new Intent(TasksActivity.this, TaskDetailActivity.class);
        intent.putExtra("taskId", task.getId());
        startActivity(intent);
    }

    private void onTaskCheckToggled(Task task, boolean isChecked) {
        // Update task completion status
        task.setCompleted(isChecked);
        taskRepository.updateTask(task);
    }
    
    @Override
    protected void syncData() {
        super.syncData();
        taskRepository.syncIfNeeded();
        taskRepository.loadTasks();
    }
    
    private void showNetworkStatus(boolean isNetworkAvailable) {
        View rootView = findViewById(android.R.id.content);
        
        if (!isNetworkAvailable) {
            Snackbar.make(rootView, "You are offline. Changes will be saved locally.", Snackbar.LENGTH_LONG)
                    .setAction("Retry", v -> {
                        // Try to reload data (which will check network again)
                        taskRepository.loadTasks();
                    })
                    .show();
        } else {
            // Maybe show a brief message that we're back online?
            Snackbar.make(rootView, "Back online. Syncing changes...", Snackbar.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onNetworkAvailable() {
        super.onNetworkAvailable();
        showNetworkStatus(true);
    }
    
    @Override
    public void onNetworkUnavailable() {
        super.onNetworkUnavailable();
        showNetworkStatus(false);
    }
} 