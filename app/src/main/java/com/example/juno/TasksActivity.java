package com.example.juno;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class TasksActivity extends AppCompatActivity {

    private static final String TAG = "TasksActivity";

    private RecyclerView tasksRecyclerView;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private FloatingActionButton addTaskFab;
    private ImageButton backButton;
    private TextView emptyStateText;
    
    private String userId;
    private DatabaseReference mDatabase;

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

        // Initialize Firebase Database
        mDatabase = FirebaseDatabase.getInstance("https://juno-aa7d2-default-rtdb.firebaseio.com/").getReference();

        // Initialize UI components
        tasksRecyclerView = findViewById(R.id.tasks_recycler_view);
        addTaskFab = findViewById(R.id.add_task_fab);
        backButton = findViewById(R.id.back_button);
        emptyStateText = findViewById(R.id.empty_state_text);

        // Set up RecyclerView
        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(this, taskList, new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(Task task, int position) {
                TasksActivity.this.onTaskClick(task);
            }

            @Override
            public void onCheckboxClicked(Task task, int position, boolean isChecked) {
                onTaskCompleteToggle(task);
            }
        });
        
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tasksRecyclerView.setAdapter(taskAdapter);

        // Set up click listeners
        addTaskFab.setOnClickListener(v -> {
            // Launch CreateTaskActivity instead of going back
            Intent intent = new Intent(TasksActivity.this, CreateTaskActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        backButton.setOnClickListener(v -> finish());

        // Load tasks
        loadTasks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh tasks list when returning to this activity
        loadTasks();
    }

    private void loadTasks() {
        mDatabase.child("users").child(userId).child("tasks").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                taskList.clear();
                
                for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                    Task task = taskSnapshot.getValue(Task.class);
                    if (task != null) {
                        task.setId(taskSnapshot.getKey());
                        taskList.add(task);
                    }
                }
                
                taskAdapter.notifyDataSetChanged();
                
                // Show/hide empty state
                if (taskList.isEmpty()) {
                    emptyStateText.setVisibility(View.VISIBLE);
                    tasksRecyclerView.setVisibility(View.GONE);
                } else {
                    emptyStateText.setVisibility(View.GONE);
                    tasksRecyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "loadTasks:onCancelled", databaseError.toException());
                Toast.makeText(TasksActivity.this, "Failed to load tasks.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onTaskClick(Task task) {
        // Navigate to TaskDetailActivity
        Intent intent = new Intent(TasksActivity.this, TaskDetailActivity.class);
        intent.putExtra("taskId", task.getId());
        startActivity(intent);
    }

    private void onTaskCompleteToggle(Task task) {
        // Update task completion status in database
        mDatabase.child("users").child(userId).child("tasks").child(task.getId()).child("completed")
                .setValue(task.isCompleted())
                .addOnSuccessListener(aVoid -> {
                    String message = task.isCompleted() ? "Task marked as complete" : "Task marked as incomplete";
                    Toast.makeText(TasksActivity.this, message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(TasksActivity.this, "Failed to update task status", Toast.LENGTH_SHORT).show();
                });
    }
} 