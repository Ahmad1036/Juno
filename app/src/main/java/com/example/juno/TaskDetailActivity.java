package com.example.juno;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.bumptech.glide.Glide;

public class TaskDetailActivity extends AppCompatActivity {

    private static final String TAG = "TaskDetailActivity";
    
    // UI Components
    private ImageButton backButton;
    private ImageView priorityIndicator;
    private TextView taskTitleView;
    private TextView dueDateView;
    private TextView descriptionView;
    private CheckBox completedCheckbox;
    private CardView taskImageCard;
    private ImageView taskImageView;
    private Button editTaskButton;
    private Button deleteTaskButton;
    
    // Data
    private String taskId;
    private String userId;
    private Task currentTask;
    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        
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
        
        initializeViews();
        setupClickListeners();
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
        editTaskButton = findViewById(R.id.edit_task_button);
        deleteTaskButton = findViewById(R.id.delete_task_button);
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        
        completedCheckbox.setOnClickListener(v -> {
            if (currentTask != null) {
                boolean isChecked = completedCheckbox.isChecked();
                currentTask.setCompleted(isChecked);
                updateTaskCompletionStatus(isChecked);
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
        mDatabase.child("users").child(userId).child("tasks").child(taskId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(TaskDetailActivity.this, "Task deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(TaskDetailActivity.this, "Failed to delete task", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error deleting task", e);
                });
    }
    
    private void updateTaskCompletionStatus(boolean isCompleted) {
        mDatabase.child("users").child(userId).child("tasks").child(taskId).child("completed")
                .setValue(isCompleted)
                .addOnSuccessListener(aVoid -> {
                    String message = isCompleted ? "Task marked as complete" : "Task marked as incomplete";
                    Toast.makeText(TaskDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(TaskDetailActivity.this, "Failed to update task status", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating task status", e);
                });
    }
    
    private void loadTaskDetails() {
        mDatabase.child("users").child(userId).child("tasks").child(taskId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentTask = dataSnapshot.getValue(Task.class);
                if (currentTask != null) {
                    currentTask.setId(taskId);
                    displayTaskDetails();
                } else {
                    Toast.makeText(TaskDetailActivity.this, "Task not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "loadTask:onCancelled", databaseError.toException());
                Toast.makeText(TaskDetailActivity.this, "Failed to load task", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
    
    private void displayTaskDetails() {
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
        setPriorityIndicator(currentTask.getPriorityInt());
        
        // Load task image if available
        if (currentTask.getImageUrl() != null && !currentTask.getImageUrl().isEmpty()) {
            taskImageCard.setVisibility(View.VISIBLE);
            Glide.with(this)
                 .load(currentTask.getImageUrl())
                 .placeholder(R.drawable.placeholder_image)
                 .error(R.drawable.error_image)
                 .into(taskImageView);
        } else {
            taskImageCard.setVisibility(View.GONE);
        }
    }
    
    private void setPriorityIndicator(int priority) {
        int priorityDrawable;
        
        switch (priority) {
            case 1: // High
                priorityDrawable = R.drawable.priority_high_indicator;
                break;
            case 3: // Low
                priorityDrawable = R.drawable.priority_low_indicator;
                break;
            default: // Medium (2) or any other value
                priorityDrawable = R.drawable.priority_medium_indicator;
                break;
        }
        
        priorityIndicator.setImageDrawable(ContextCompat.getDrawable(this, priorityDrawable));
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload task details when returning to this activity (in case it was edited)
        if (taskId != null && !taskId.isEmpty()) {
            loadTaskDetails();
        }
    }
} 