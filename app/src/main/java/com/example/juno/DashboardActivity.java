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
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";
    private static final int MAX_DASHBOARD_TASKS = 3;

    private TextView dateTimeText;
    private TextView greetingText;
    private TextView moodText;
    private CardView tasksCard;
    private CardView journalCard;
    private CardView calendarCard;
    private CardView suggestionsCard;
    private ImageButton settingsButton;
    private ImageButton addTaskButton;
    
    // Task-related views
    private RecyclerView dashboardTasksRecyclerView;
    private TextView dashboardTasksCount;
    private TextView dashboardNoTasksText;
    private DashboardTaskAdapter taskAdapter;
    private List<Task> taskList;
    
    // Firebase
    private DatabaseReference mDatabase;

    // User info
    private String userId;
    private String userName;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Check if user is logged in
        if (!checkUserSession()) {
            // User not logged in, redirect to SignInActivity
            startActivity(new Intent(DashboardActivity.this, SignInActivity.class));
            finish();
            return;
        }
        
        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance("https://juno-aa7d2-default-rtdb.firebaseio.com/").getReference();

        // Initialize UI components
        dateTimeText = findViewById(R.id.date_time_text);
        greetingText = findViewById(R.id.greeting_text);
        moodText = findViewById(R.id.mood_text);
        tasksCard = findViewById(R.id.tasks_card);
        journalCard = findViewById(R.id.journal_card);
        calendarCard = findViewById(R.id.calendar_card);
        suggestionsCard = findViewById(R.id.suggestions_card);
        settingsButton = findViewById(R.id.settings_button);
        addTaskButton = findViewById(R.id.add_task_button);
        
        // Initialize task views
        dashboardTasksRecyclerView = findViewById(R.id.dashboard_tasks_recycler_view);
        dashboardTasksCount = findViewById(R.id.dashboard_tasks_count);
        dashboardNoTasksText = findViewById(R.id.dashboard_no_tasks_text);
        
        // Set up RecyclerView
        taskList = new ArrayList<>();
        taskAdapter = new DashboardTaskAdapter(this, taskList, this::openTaskDetail);
        dashboardTasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dashboardTasksRecyclerView.setAdapter(taskAdapter);

        // Set up the date and time
        updateDateTime();

        // Set up the greeting
        updateGreeting();

        // Set up click listeners
        setupClickListeners();

        // Set up animations
        setupAnimations();
        
        // Load tasks for dashboard
        loadDashboardTasks();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh tasks when returning to dashboard
        loadDashboardTasks();
    }

    private boolean checkUserSession() {
        SharedPreferences prefs = getSharedPreferences("JunoUserPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        
        if (isLoggedIn) {
            userId = prefs.getString("userId", "");
            userName = prefs.getString("userName", "");
            userEmail = prefs.getString("userEmail", "");
            return true;
        }
        
        return false;
    }

    private void logout() {
        // Clear user session
        SharedPreferences prefs = getSharedPreferences("JunoUserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        
        // Redirect to SignInActivity
        startActivity(new Intent(DashboardActivity.this, SignInActivity.class));
        finish();
    }

    private void updateDateTime() {
        // Update the current date and time
        Date currentDate = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        String dateStr = dateFormat.format(currentDate);
        String timeStr = timeFormat.format(currentDate);
        
        dateTimeText.setText(dateStr + " • " + timeStr);
    }

    private void updateGreeting() {
        // Get the current hour of day to determine appropriate greeting
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (hourOfDay < 12) {
            greeting = "good morning";
        } else if (hourOfDay < 18) {
            greeting = "good afternoon";
        } else {
            greeting = "good evening";
        }

        greetingText.setText(greeting);
    }

    private void setupClickListeners() {
        // Tasks card click
        tasksCard.setOnClickListener(v -> {
            // Navigate to Tasks screen
            Intent intent = new Intent(DashboardActivity.this, TasksActivity.class);
            startActivity(intent);
        });

        // Journal card click
        journalCard.setOnClickListener(v -> {
            // Navigate to Journal screen
            Intent intent = new Intent(DashboardActivity.this, JournalListActivity.class);
            startActivity(intent);
        });

        // Calendar card click
        calendarCard.setOnClickListener(v -> {
            // Navigate to Calendar screen
            Toast.makeText(DashboardActivity.this, "Calendar feature coming soon", Toast.LENGTH_SHORT).show();
        });
        
        // Create Task card click (new)
        CardView createTaskCard = findViewById(R.id.create_task_card);
        if (createTaskCard != null) {
            createTaskCard.setOnClickListener(v -> {
                // Navigate to Task Creation screen
                Intent intent = new Intent(DashboardActivity.this, CreateTaskActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        // Suggestions card click
        suggestionsCard.setOnClickListener(v -> {
            // Navigate to Suggestions screen
            Toast.makeText(DashboardActivity.this, "Suggestions feature coming soon", Toast.LENGTH_SHORT).show();
        });

        // Settings button click
        settingsButton.setOnClickListener(v -> {
            // Navigate to Settings screen
            Intent intent = new Intent(DashboardActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // Add task button click
        addTaskButton.setOnClickListener(v -> {
            // Navigate to Task Creation screen
            Intent intent = new Intent(DashboardActivity.this, CreateTaskActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void setupAnimations() {
        // Animate UI elements to fade in sequentially
        View[] views = {
            dateTimeText, greetingText, moodText,
            tasksCard, journalCard, calendarCard, suggestionsCard,
            settingsButton, addTaskButton
        };
        
        for (int i = 0; i < views.length; i++) {
            views[i].setAlpha(0f);
            views[i].animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setStartDelay(100 + (i * 100))
                    .start();
        }
    }
    
    private void loadDashboardTasks() {
        // Show loading state
        dashboardTasksCount.setText("loading tasks...");
        dashboardNoTasksText.setVisibility(View.GONE);
        
        // Query only high priority tasks, limited to 3 for the dashboard
        Query query = mDatabase.child("users").child(userId).child("tasks")
                .orderByChild("priority")
                .equalTo("high")
                .limitToFirst(MAX_DASHBOARD_TASKS);
        
        query.addListenerForSingleValueEvent(new ValueEventListener() {
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
                
                if (taskList.isEmpty()) {
                    // No high-priority tasks, try to get any tasks
                    loadAnyTasks();
                } else {
                    // Update UI with high-priority tasks
                    updateTasksUI();
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading tasks", databaseError.toException());
                dashboardTasksCount.setText("failed to load tasks");
                dashboardNoTasksText.setVisibility(View.VISIBLE);
            }
        });
    }
    
    private void loadAnyTasks() {
        // If there are no high-priority tasks, just get any tasks
        mDatabase.child("users").child(userId).child("tasks")
                .limitToFirst(MAX_DASHBOARD_TASKS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
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
                        
                        updateTasksUI();
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error loading any tasks", databaseError.toException());
                        dashboardTasksCount.setText("failed to load tasks");
                        dashboardNoTasksText.setVisibility(View.VISIBLE);
                    }
                });
    }
    
    private void updateTasksUI() {
        if (taskList.isEmpty()) {
            dashboardTasksCount.setText("no tasks yet");
            dashboardNoTasksText.setVisibility(View.VISIBLE);
            dashboardTasksRecyclerView.setVisibility(View.GONE);
        } else {
            int taskCount = taskList.size();
            dashboardTasksCount.setText("you have " + taskCount + " task" + (taskCount > 1 ? "s" : ""));
            dashboardNoTasksText.setVisibility(View.GONE);
            dashboardTasksRecyclerView.setVisibility(View.VISIBLE);
            taskAdapter.updateData(taskList);
        }
    }
    
    private void openTaskDetail(Task task) {
        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra("taskId", task.getId());
        startActivity(intent);
    }
} 