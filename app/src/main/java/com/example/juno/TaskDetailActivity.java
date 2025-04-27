package com.example.juno;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.juno.model.Task;
import com.example.juno.repository.TaskRepository;
import com.example.juno.utils.NetworkManager;
import com.example.juno.utils.ReminderManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.List;

public class TaskDetailActivity extends AppCompatActivity implements NetworkManager.NetworkChangeListener {

    private static final String TAG = "TaskDetailActivity";
    
    // UI Components
    private ImageButton backButton;
    private View priorityIndicator;
    private TextView taskTitleView;
    private TextView dueDateView;
    private TextView descriptionView;
    private CheckBox completedCheckbox;
    private CardView taskImageCard;
    private ImageView taskImageView;
    private ProgressBar imageProgressBar;
    private Button editTaskButton;
    private Button deleteTaskButton;
    private TextView offlineIndicator;
    private ProgressBar loadingProgressBar;
    
    // Data
    private String taskId;
    private String userId;
    private Task currentTask;
    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;
    private TaskRepository taskRepository;
    private NetworkManager networkManager;
    private ReminderManager reminderManager;
    private String selectedPriority = Task.PRIORITY_MEDIUM; // Default priority

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance("https://juno-aa7d2-default-rtdb.firebaseio.com/").getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        
        // Get user ID
        SharedPreferences prefs = getSharedPreferences("JunoUserPrefs", MODE_PRIVATE);
        userId = prefs.getString("userId", "");
        if (userId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }
        
        // Get task ID from intent
        taskId = getIntent().getStringExtra("taskId");
        if (taskId == null || taskId.isEmpty()) {
            Toast.makeText(this, "No task specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize TaskRepository for offline support
        taskRepository = new TaskRepository(this, userId);
        
        // Initialize NetworkManager
        networkManager = new NetworkManager(this);
        networkManager.setNetworkChangeListener(this);
        
        // Initialize the ReminderManager
        reminderManager = new ReminderManager(this, userId);
        
        initializeViews();
        setupClickListeners();
        setupRepositoryObservers();
        
        // Use repository to load task
        loadTaskDetails();
    }
    
    private void initializeViews() {
        backButton = findViewById(R.id.back_button);
        priorityIndicator = findViewById(R.id.priority_indicator);
        taskTitleView = findViewById(R.id.task_title);
        dueDateView = findViewById(R.id.due_date);
        descriptionView = findViewById(R.id.task_description);
        completedCheckbox = findViewById(R.id.completed_checkbox);
        taskImageCard = findViewById(R.id.task_image_card);
        taskImageView = findViewById(R.id.task_image);
        imageProgressBar = findViewById(R.id.image_progress_bar);
        editTaskButton = findViewById(R.id.edit_task_button);
        deleteTaskButton = findViewById(R.id.delete_task_button);
        loadingProgressBar = findViewById(R.id.loading_progress_bar);
        
        // Create an offline indicator TextView
        offlineIndicator = new TextView(this);
        offlineIndicator.setText("Offline Mode - Changes will sync when online");
        offlineIndicator.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        offlineIndicator.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_light));
        offlineIndicator.setPadding(32, 16, 32, 16);
        offlineIndicator.setVisibility(View.GONE);
        
        // Add to layout at the top
        findViewById(android.R.id.content).post(() -> {
            View root = findViewById(android.R.id.content);
            if (root instanceof android.view.ViewGroup) {
                ((android.view.ViewGroup) root).addView(offlineIndicator, 0);
            }
        });
    }
    
    private void setupRepositoryObservers() {
        // Observe tasks to find the current one
        taskRepository.getTasksLiveData().observe(this, tasks -> {
            if (tasks != null) {
                for (Task task : tasks) {
                    if (taskId.equals(task.getId())) {
                        currentTask = task;
                        displayTaskDetails();
                        break;
                    }
                }
            }
        });
        
        // Observe error messages
        taskRepository.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        
        completedCheckbox.setOnClickListener(v -> {
            if (currentTask != null) {
                boolean isChecked = completedCheckbox.isChecked();
                currentTask.setCompleted(isChecked);
                
                // Use repository to update task
                taskRepository.updateTask(currentTask);
                
                // Show feedback
                String message = isChecked ? "Task marked as complete" : "Task marked as incomplete";
                Toast.makeText(TaskDetailActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
        
        editTaskButton.setOnClickListener(v -> {
            if (currentTask != null) {
                Intent intent = new Intent(TaskDetailActivity.this, CreateTaskActivity.class);
                intent.putExtra("taskId", taskId);
                intent.putExtra("isEditing", true);
                startActivity(intent);
            }
        });
        
        deleteTaskButton.setOnClickListener(v -> {
            if (currentTask != null) {
                showDeleteConfirmationDialog();
            }
        });
    }
    
    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Task");
        builder.setMessage("Are you sure you want to delete this task?");
        builder.setPositiveButton("Delete", (dialog, which) -> deleteTask());
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void deleteTask() {
        // Use repository to delete task
        taskRepository.deleteTask(currentTask);
        Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    private void loadTaskDetails() {
        // Load tasks from repository, our observer will find the right one
        taskRepository.loadTasks();
        
        // For backward compatibility, also try to load from Firebase directly
        if (!networkManager.isNetworkAvailable()) {
            // Show offline indicator
            offlineIndicator.setVisibility(View.VISIBLE);
            return;
        }
        
        mDatabase.child("users").child(userId).child("tasks").child(taskId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Task task = dataSnapshot.getValue(Task.class);
                if (task != null) {
                    task.setId(taskId);
                    currentTask = task;
                    displayTaskDetails();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "loadTask:onCancelled", databaseError.toException());
                // The repository should handle this case
            }
        });
    }
    
    private void displayTaskDetails() {
        if (currentTask == null) return;
        
        // Set title
        taskTitleView.setText(currentTask.getTitle());
        
        // Set due date
        if (currentTask.getDueDate() > 0) {
            // Format the date
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
            String formattedDate = dateFormat.format(new java.util.Date(currentTask.getDueDate()));
            dueDateView.setText(formattedDate);
            dueDateView.setVisibility(View.VISIBLE);
        } else {
            dueDateView.setVisibility(View.GONE);
        }
        
        // Set description
        if (currentTask.getDescription() != null && !currentTask.getDescription().isEmpty()) {
            descriptionView.setText(currentTask.getDescription());
            descriptionView.setVisibility(View.VISIBLE);
        } else {
            descriptionView.setVisibility(View.GONE);
        }
        
        // Set completion status
        completedCheckbox.setChecked(currentTask.isCompleted());
        
        // Set priority indicator
        String priority = currentTask.getPriority();
        if (priority != null && !priority.isEmpty()) {
            if (priority.equalsIgnoreCase("high")) {
                setPriorityIndicator(2);
            } else if (priority.equalsIgnoreCase("medium")) {
                setPriorityIndicator(1);
            } else {
                setPriorityIndicator(0);
            }
        }
        
        // Load task image if available
        loadTaskImage();
        
        // Show completed status visually
        if (currentTask.isCompleted()) {
            taskTitleView.setPaintFlags(taskTitleView.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            taskTitleView.setAlpha(0.6f);
            descriptionView.setPaintFlags(descriptionView.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            descriptionView.setAlpha(0.6f);
        } else {
            taskTitleView.setPaintFlags(taskTitleView.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
            taskTitleView.setAlpha(1.0f);
            descriptionView.setPaintFlags(descriptionView.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
            descriptionView.setAlpha(1.0f);
        }
    }
    
    private void loadTaskImage() {
        // Check for image data (Base64 encoded string)
        if (currentTask == null) return;
        
        String imageData = currentTask.getImageData();
        
        // For backward compatibility, also check imageUrl if imageData isn't available
        String imageUrl = currentTask.getImageUrl();
        
        if (imageData != null && !imageData.isEmpty()) {
            // Show the image card and progress indicator
            taskImageCard.setVisibility(View.VISIBLE);
            imageProgressBar.setVisibility(View.VISIBLE);
            
            try {
                // Decode Base64 string to bitmap
                byte[] decodedString = Base64.decode(imageData, Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                
                // Display the decoded bitmap
                taskImageView.setImageBitmap(decodedBitmap);
                imageProgressBar.setVisibility(View.GONE);
            } catch (Exception e) {
                Log.e(TAG, "Failed to decode image data", e);
                imageProgressBar.setVisibility(View.GONE);
                taskImageCard.setVisibility(View.GONE);
            }
        } else if (imageUrl != null && !imageUrl.isEmpty() && networkManager.isNetworkAvailable()) {
            // If imageData isn't available but imageUrl is, and we're online,
            // load it from Firebase Storage
            taskImageCard.setVisibility(View.VISIBLE);
            imageProgressBar.setVisibility(View.VISIBLE);
            
            // Use Glide to load the image
            Glide.with(this)
                .load(imageUrl)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Log.e(TAG, "Failed to load image from URL", e);
                        imageProgressBar.setVisibility(View.GONE);
                        taskImageCard.setVisibility(View.GONE);
                        return false;
                    }
                    
                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        imageProgressBar.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(taskImageView);
        } else {
            // No image available
            taskImageCard.setVisibility(View.GONE);
        }
    }
    
    private void setPriorityIndicator(int priority) {
        if (priorityIndicator != null) {
            int colorResId;
            switch (priority) {
                case 2: // High
                    colorResId = R.color.high_priority;
                    selectedPriority = Task.PRIORITY_HIGH;
                    break;
                case 1: // Medium
                    colorResId = R.color.medium_priority;
                    selectedPriority = Task.PRIORITY_MEDIUM;
                    break;
                case 0: // Low
                default:
                    colorResId = R.color.low_priority;
                    selectedPriority = Task.PRIORITY_LOW;
                    break;
            }
            
            priorityIndicator.setBackgroundColor(ContextCompat.getColor(this, colorResId));
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Check network status and show/hide offline indicator
        updateOfflineIndicator();
        
        // Reload task details
        loadTaskDetails();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Clean up network callbacks
        if (networkManager != null) {
            networkManager.unregisterNetworkCallbacks();
        }
    }
    
    @Override
    public void onNetworkAvailable() {
        // Network is available, sync any pending changes
        updateOfflineIndicator();
        taskRepository.syncIfNeeded();
    }
    
    @Override
    public void onNetworkUnavailable() {
        // Network is gone - update UI
        updateOfflineIndicator();
    }
    
    private void updateOfflineIndicator() {
        boolean isNetworkAvailable = networkManager.isNetworkAvailable();
        
        runOnUiThread(() -> {
            if (isNetworkAvailable) {
                offlineIndicator.setVisibility(View.GONE);
            } else {
                offlineIndicator.setVisibility(View.VISIBLE);
            }
        });
    }

    private void saveTask() {
        // Get task data from UI
        String title = taskTitleView.getText().toString().trim();
        String description = descriptionView.getText().toString().trim();
        
        // Validate title
        if (title.isEmpty()) {
            taskTitleView.setError("Task title is required");
            return;
        }
        
        // Update task object with UI data
        currentTask.setTitle(title);
        currentTask.setDescription(description);
        currentTask.setDueDate(System.currentTimeMillis());
        currentTask.setPriority(selectedPriority);
        
        // Show loading
        showLoading(true);
        
        // Save task
        taskRepository.saveTask(currentTask, new TaskRepository.TaskCallback() {
            @Override
            public void onTaskSaved(Task task) {
                runOnUiThread(() -> {
                    showLoading(false);
                    
                    // Schedule reminder for the saved task
                    reminderManager.scheduleReminder(task);
                    
                    Toast.makeText(TaskDetailActivity.this, "Task saved", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
            
            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(TaskDetailActivity.this, "Error saving task: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Show or hide loading indicator
     */
    private void showLoading(boolean show) {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
} 