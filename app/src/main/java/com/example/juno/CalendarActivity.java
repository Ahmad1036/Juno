package com.example.juno;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.juno.adapter.CalendarTaskAdapter;
import com.example.juno.model.Task;
import com.example.juno.view.PriorityCalendarView;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarActivity extends AppCompatActivity implements CalendarTaskAdapter.OnTaskClickListener {

    private static final String TAG = "CalendarActivity";

    // UI elements
    private PriorityCalendarView calendarView;
    private RecyclerView taskRecyclerView;
    private TextView noTasksText;
    private ProgressBar progressBar;
    private ImageButton backButton;

    // Firebase
    private DatabaseReference mDatabase;
    
    // Data
    private String userId;
    private CalendarTaskAdapter taskAdapter;
    private Map<String, List<Task>> tasksByDate = new HashMap<>();
    private List<Task> allTasks = new ArrayList<>();
    private List<Task> tasksForSelectedDate = new ArrayList<>();
    private Date selectedDate = new Date();
    
    // Map to track priority tasks by date
    private Map<String, Boolean> hasHighPriorityTask = new HashMap<>();
    private Map<String, Boolean> hasMediumPriorityTask = new HashMap<>();
    private Map<String, Boolean> hasLowPriorityTask = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        // Initialize Firebase Database
        mDatabase = FirebaseDatabase.getInstance("https://juno-aa7d2-default-rtdb.firebaseio.com/").getReference();
        
        // Get user ID from shared preferences
        SharedPreferences prefs = getSharedPreferences("JunoUserPrefs", MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize UI elements
        calendarView = findViewById(R.id.calendarView);
        taskRecyclerView = findViewById(R.id.task_recycler_view);
        noTasksText = findViewById(R.id.no_tasks_text);
        progressBar = findViewById(R.id.progress_bar);
        backButton = findViewById(R.id.back_button);
        
        // Debug log
        Log.d(TAG, "CalendarActivity created, user ID: " + userId);
        
        // Set up back button
        backButton.setOnClickListener(v -> onBackPressed());
        
        // Set up RecyclerView
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new CalendarTaskAdapter(this, tasksForSelectedDate, this);
        taskRecyclerView.setAdapter(taskAdapter);
        
        // Set up calendar selection listener
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            selectedDate = calendar.getTime();
            Log.d(TAG, "Selected date: " + formatDateKey(selectedDate));
            updateTaskListForSelectedDate();
        });
        
        // Load all tasks for the user
        loadUserTasks();
    }
    
    private void loadUserTasks() {
        showProgress(true);
        Log.d(TAG, "Loading user tasks...");
        
        // Update the database reference to match TasksActivity implementation
        DatabaseReference tasksRef = mDatabase.child("users").child(userId).child("tasks");
        tasksRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allTasks.clear();
                tasksByDate.clear();
                
                // Clear priority maps
                hasHighPriorityTask.clear();
                hasMediumPriorityTask.clear();
                hasLowPriorityTask.clear();
                
                Log.d(TAG, "Task data snapshot exists: " + dataSnapshot.exists());
                Log.d(TAG, "Task data snapshot has children: " + dataSnapshot.hasChildren());
                Log.d(TAG, "Task data snapshot child count: " + dataSnapshot.getChildrenCount());
                
                for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                    try {
                        Task task = taskSnapshot.getValue(Task.class);
                        if (task != null) {
                            // Set the task ID from the snapshot key
                            task.setId(taskSnapshot.getKey());
                            allTasks.add(task);
                            
                            // Get date components
                            Calendar taskCalendar = Calendar.getInstance();
                            taskCalendar.setTimeInMillis(task.getDueDate());
                            int year = taskCalendar.get(Calendar.YEAR);
                            int month = taskCalendar.get(Calendar.MONTH);
                            int day = taskCalendar.get(Calendar.DAY_OF_MONTH);
                            
                            Log.d(TAG, "Loaded task: " + task.getTitle() + " for date: " + year + "-" + month + "-" + day 
                                + " with priority: " + task.getPriority());
                            
                            // Group tasks by date for easy access
                            String dateKey = formatDateKey(new Date(task.getDueDate()));
                            if (!tasksByDate.containsKey(dateKey)) {
                                tasksByDate.put(dateKey, new ArrayList<>());
                            }
                            tasksByDate.get(dateKey).add(task);
                            
                            // Track task priorities by date
                            String priorityDateKey = String.format("%d-%d-%d", year, month, day);
                            
                            // Update priority maps based on task priority
                            String priority = task.getPriority().toUpperCase();
                            if (priority.equals("HIGH")) {
                                hasHighPriorityTask.put(priorityDateKey, true);
                            } else if (priority.equals("MEDIUM")) {
                                hasMediumPriorityTask.put(priorityDateKey, true);
                            } else if (priority.equals("LOW")) {
                                hasLowPriorityTask.put(priorityDateKey, true);
                            }
                        } else {
                            Log.w(TAG, "Failed to parse task for key: " + taskSnapshot.getKey());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing task: " + e.getMessage());
                    }
                }
                
                // Log total task count by priority for debugging
                Log.d(TAG, "Loaded " + allTasks.size() + " tasks");
                Log.d(TAG, "High priority dates: " + hasHighPriorityTask.size());
                Log.d(TAG, "Medium priority dates: " + hasMediumPriorityTask.size());
                Log.d(TAG, "Low priority dates: " + hasLowPriorityTask.size());
                
                // Mark dates on calendar that have tasks
                markDatesWithTasks();
                
                // Update task list for currently selected date
                updateTaskListForSelectedDate();
                
                showProgress(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "loadUserTasks:onCancelled", databaseError.toException());
                Toast.makeText(CalendarActivity.this, "Failed to load tasks", Toast.LENGTH_SHORT).show();
                showProgress(false);
            }
        });
    }
    
    private void markDatesWithTasks() {
        // Clear any existing markers
        calendarView.clearAllPriorities();
        Log.d(TAG, "Marking dates with tasks, cleared previous markers");
        
        // Go through all tasks and mark their dates on the calendar
        for (Task task : allTasks) {
            Calendar taskCalendar = Calendar.getInstance();
            taskCalendar.setTimeInMillis(task.getDueDate());
            
            int year = taskCalendar.get(Calendar.YEAR);
            int month = taskCalendar.get(Calendar.MONTH);
            int day = taskCalendar.get(Calendar.DAY_OF_MONTH);
            
            String dateKey = String.format("%d-%d-%d", year, month, day);
            
            // Determine which color to use for the task (highest priority wins)
            int priorityColor;
            if (hasHighPriorityTask.containsKey(dateKey)) {
                priorityColor = ContextCompat.getColor(this, R.color.high_priority);
                Log.d(TAG, "Marking date " + dateKey + " with HIGH priority");
            } else if (hasMediumPriorityTask.containsKey(dateKey)) {
                priorityColor = ContextCompat.getColor(this, R.color.medium_priority);
                Log.d(TAG, "Marking date " + dateKey + " with MEDIUM priority");
            } else if (hasLowPriorityTask.containsKey(dateKey)) {
                priorityColor = ContextCompat.getColor(this, R.color.low_priority);
                Log.d(TAG, "Marking date " + dateKey + " with LOW priority");
            } else {
                priorityColor = ContextCompat.getColor(this, R.color.primary);
                Log.d(TAG, "Marking date " + dateKey + " with default priority");
            }
            
            // Add the priority to the date
            calendarView.addPriorityToDate(year, month, day, priorityColor);
        }
        
        // Force calendar to refresh by calling invalidate and requestLayout
        calendarView.invalidate();
        calendarView.requestLayout();
        
        // Add a delayed refresh to ensure calendar updates properly
        calendarView.postDelayed(() -> {
            calendarView.invalidate();
            calendarView.requestLayout();
            Log.d(TAG, "Final calendar refresh applied");
        }, 300);
    }
    
    private void updateTaskListForSelectedDate() {
        String dateKey = formatDateKey(selectedDate);
        
        tasksForSelectedDate.clear();
        if (tasksByDate.containsKey(dateKey)) {
            tasksForSelectedDate.addAll(tasksByDate.get(dateKey));
        }
        
        if (tasksForSelectedDate.isEmpty()) {
            noTasksText.setVisibility(View.VISIBLE);
            taskRecyclerView.setVisibility(View.GONE);
        } else {
            noTasksText.setVisibility(View.GONE);
            taskRecyclerView.setVisibility(View.VISIBLE);
            
            // Update adapter
            taskAdapter.updateTasks(tasksForSelectedDate);
        }
    }
    
    private String formatDateKey(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(date);
    }
    
    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onTaskClick(Task task) {
        // Show task details dialog
        TaskDetailsDialog dialog = new TaskDetailsDialog(task);
        dialog.show(getSupportFragmentManager(), "TaskDetailsDialog");
    }
    
    /**
     * Dialog fragment to show task details
     */
    public static class TaskDetailsDialog extends androidx.fragment.app.DialogFragment {
        
        private Task task;
        private DatabaseReference mDatabase;
        private String userId;
        
        public TaskDetailsDialog(Task task) {
            this.task = task;
            // Required empty public constructor
        }
        
        @Override
        public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireActivity(), R.style.DarkAlertDialog);
            
            // Inflate custom layout for dialog
            View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_task_details, null);
            
            // Set task details in the view
            TextView titleView = view.findViewById(R.id.task_details_title);
            TextView descriptionView = view.findViewById(R.id.task_details_description);
            TextView dateTimeView = view.findViewById(R.id.task_details_date_time);
            TextView priorityView = view.findViewById(R.id.task_details_priority);
            View priorityIndicator = view.findViewById(R.id.priority_indicator);
            
            // Initialize database
            mDatabase = FirebaseDatabase.getInstance().getReference();
            
            // Get user ID from SharedPreferences
            SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("JunoPrefs", Context.MODE_PRIVATE);
            userId = sharedPreferences.getString("userId", null);
            
            titleView.setText(task.getTitle());
            descriptionView.setText(task.getDescription());
            
            // Format date
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
            String dateStr = sdf.format(new Date(task.getDueDate()));
            dateTimeView.setText(dateStr + " • " + task.getTime());
            
            // Set priority with color
            priorityView.setText("Priority: " + task.getPriority());
            
            int priorityColor;
            switch (task.getPriority().toUpperCase()) {
                case "HIGH":
                    priorityColor = R.color.high_priority;
                    break;
                case "MEDIUM":
                    priorityColor = R.color.medium_priority;
                    break;
                case "LOW":
                    priorityColor = R.color.low_priority;
                    break;
                default:
                    priorityColor = R.color.primary;
                    break;
            }
            
            priorityIndicator.setBackgroundColor(getResources().getColor(priorityColor));
            
            // Create the completed checkbox programmatically
            CheckBox completedCheckbox = new CheckBox(requireContext());
            completedCheckbox.setText("Mark as completed");
            completedCheckbox.setTextColor(getResources().getColor(R.color.dashboard_text_primary));
            completedCheckbox.setChecked(task.isCompleted());
            
            // Set checkbox listener
            completedCheckbox.setOnClickListener(v -> {
                if (userId != null && task.getId() != null) {
                    boolean isChecked = completedCheckbox.isChecked();
                    task.setCompleted(isChecked);
                    updateTaskCompletionStatus(isChecked);
                }
            });
            
            // Create a container to add the checkbox above the buttons
            LinearLayout container = new LinearLayout(requireContext());
            container.setOrientation(LinearLayout.VERTICAL);
            container.addView(view);
            
            // Add some padding around the checkbox
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(64, 16, 16, 0);
            completedCheckbox.setLayoutParams(params);
            container.addView(completedCheckbox);
            
            builder.setView(container)
                   .setPositiveButton("Close", (dialog, id) -> dismiss())
                   .setNeutralButton("Delete", (dialog, id) -> {
                       if (userId != null && task.getId() != null) {
                           showDeleteConfirmationDialog();
                       }
                   });
            
            return builder.create();
        }
        
        private void showDeleteConfirmationDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setTitle("Delete Task");
            builder.setMessage("Are you sure you want to delete this task?");
            builder.setPositiveButton("Delete", (dialog, which) -> deleteTask());
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }
        
        private void deleteTask() {
            mDatabase.child("users").child(userId).child("tasks").child(task.getId()).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireActivity(), "Task deleted", Toast.LENGTH_SHORT).show();
                        dismiss();
                        // Refresh tasks in the parent activity
                        ((CalendarActivity)requireActivity()).loadUserTasks();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireActivity(), "Failed to delete task", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error deleting task", e);
                    });
        }
        
        private void updateTaskCompletionStatus(boolean isCompleted) {
            mDatabase.child("users").child(userId).child("tasks").child(task.getId()).child("completed")
                    .setValue(isCompleted)
                    .addOnSuccessListener(aVoid -> {
                        String message = isCompleted ? "Task marked as complete" : "Task marked as incomplete";
                        Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show();
                        // Refresh tasks in the parent activity
                        ((CalendarActivity)requireActivity()).loadUserTasks();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireActivity(), "Failed to update task status", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error updating task status", e);
                    });
        }
    }
} 