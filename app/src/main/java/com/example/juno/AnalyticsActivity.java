package com.example.juno;

import android.content.SharedPreferences;
import android.graphics.drawable.ClipDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AnalyticsActivity extends AppCompatActivity {

    private static final String TAG = "AnalyticsActivity";

    // UI components
    private TextView completedTasksCount;
    private TextView incompleteTasksCount;
    private TextView completionPercentage;
    private View completionProgressBar;
    
    private TextView onTimeTasksCount;
    private TextView overdueTasksCount;
    private TextView deadlinePercentage;
    private View deadlineProgressBar;
    
    private TextView highPriorityCount;
    private TextView mediumPriorityCount;
    private TextView lowPriorityCount;
    
    private TextView performanceSummary;
    
    // Task statistics
    private int totalTasks = 0;
    private int completedTasks = 0;
    private int onTimeTasks = 0;
    private int overdueTasks = 0;
    private int highPriority = 0;
    private int mediumPriority = 0;
    private int lowPriority = 0;
    
    // Firebase
    private DatabaseReference mDatabase;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance("https://juno-aa7d2-default-rtdb.firebaseio.com/").getReference();

        // Initialize UI components
        initializeViews();
        
        // Get user ID from shared preferences
        SharedPreferences prefs = getSharedPreferences("JunoUserPrefs", MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Load and display task analytics
        loadTaskStatistics();
    }
    
    private void initializeViews() {
        // Task completion statistics
        completedTasksCount = findViewById(R.id.completed_tasks_count);
        incompleteTasksCount = findViewById(R.id.incomplete_tasks_count);
        completionPercentage = findViewById(R.id.completion_percentage);
        completionProgressBar = findViewById(R.id.completion_progress_bar);
        
        // Deadline adherence
        onTimeTasksCount = findViewById(R.id.on_time_tasks_count);
        overdueTasksCount = findViewById(R.id.overdue_tasks_count);
        deadlinePercentage = findViewById(R.id.deadline_percentage);
        deadlineProgressBar = findViewById(R.id.deadline_progress_bar);
        
        // Priority distribution
        highPriorityCount = findViewById(R.id.high_priority_count);
        mediumPriorityCount = findViewById(R.id.medium_priority_count);
        lowPriorityCount = findViewById(R.id.low_priority_count);
        
        // Performance summary
        performanceSummary = findViewById(R.id.performance_summary);
        
        // Back button
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }
    
    private void loadTaskStatistics() {
        mDatabase.child("users").child(userId).child("tasks")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        // Reset counters
                        totalTasks = 0;
                        completedTasks = 0;
                        onTimeTasks = 0;
                        overdueTasks = 0;
                        highPriority = 0;
                        mediumPriority = 0;
                        lowPriority = 0;
                        
                        // Current date for deadline comparison
                        long currentTimeMillis = System.currentTimeMillis();
                        
                        // Process all tasks
                        for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                            Task task = taskSnapshot.getValue(Task.class);
                            if (task != null) {
                                totalTasks++;
                                
                                // Count completed tasks
                                if (task.isCompleted()) {
                                    completedTasks++;
                                    
                                    // Check if we have a completion timestamp
                                    long completedDate = task.getCompletedDate();
                                    if (completedDate > 0) {
                                        // We have a completion timestamp, so we can check accurately
                                        if (completedDate <= task.getDueDate()) {
                                            onTimeTasks++; // Completed before or on deadline
                                        } else {
                                            overdueTasks++; // Completed after deadline
                                        }
                                    } else {
                                        // For tasks without completion timestamp (legacy data)
                                        // If the due date is in the future, it was definitely completed on time
                                        if (task.getDueDate() > currentTimeMillis) {
                                            onTimeTasks++;
                                        } else {
                                            // For tasks with due dates in the past
                                            // We'll count tasks with a due date within the last 24 hours as on-time
                                            long oneDayMillis = 24 * 60 * 60 * 1000;
                                            if (currentTimeMillis - task.getDueDate() <= oneDayMillis) {
                                                onTimeTasks++;
                                            } else {
                                                overdueTasks++;
                                            }
                                        }
                                    }
                                } else {
                                    // For incomplete tasks, check if overdue
                                    if (task.getDueDate() < currentTimeMillis) {
                                        overdueTasks++;
                                    }
                                }
                                
                                // Count by priority
                                String priority = task.getPriority();
                                if (priority != null) {
                                    switch (priority.toLowerCase()) {
                                        case "high":
                                            highPriority++;
                                            break;
                                        case "medium":
                                            mediumPriority++;
                                            break;
                                        case "low":
                                            lowPriority++;
                                            break;
                                    }
                                }
                            }
                        }
                        
                        // Update UI with statistics
                        updateTaskCompletionUI();
                        updateDeadlineAdherenceUI();
                        updatePriorityDistributionUI();
                        generatePerformanceSummary();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error loading task statistics", databaseError.toException());
                        Toast.makeText(AnalyticsActivity.this, "Failed to load task data", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void updateTaskCompletionUI() {
        // Update task count text
        completedTasksCount.setText(String.valueOf(completedTasks));
        int incompleteTasks = totalTasks - completedTasks;
        incompleteTasksCount.setText(String.valueOf(incompleteTasks));
        
        // Calculate completion percentage
        int percentage = totalTasks > 0 ? (completedTasks * 100 / totalTasks) : 0;
        completionPercentage.setText(percentage + "% completion rate");
        
        // Update progress bar width based on percentage
        ViewUtils.setViewWidth(completionProgressBar, percentage);
    }
    
    private void updateDeadlineAdherenceUI() {
        // Update task count text
        onTimeTasksCount.setText(String.valueOf(onTimeTasks));
        overdueTasksCount.setText(String.valueOf(overdueTasks));
        
        // Calculate on-time percentage
        int totalTasksWithDeadline = onTimeTasks + overdueTasks;
        int percentage = totalTasksWithDeadline > 0 ? (onTimeTasks * 100 / totalTasksWithDeadline) : 0;
        deadlinePercentage.setText(percentage + "% on-time rate");
        
        // Update progress bar width based on percentage
        ViewUtils.setViewWidth(deadlineProgressBar, percentage);
    }
    
    private void updatePriorityDistributionUI() {
        // Update priority count text
        highPriorityCount.setText(String.valueOf(highPriority));
        mediumPriorityCount.setText(String.valueOf(mediumPriority));
        lowPriorityCount.setText(String.valueOf(lowPriority));
    }
    
    private void generatePerformanceSummary() {
        StringBuilder summary = new StringBuilder();
        
        // Overall completion rate assessment
        if (totalTasks == 0) {
            summary.append("You have no tasks yet. Create tasks to start tracking your productivity.");
        } else {
            // Completion rate assessment
            int completionRate = completedTasks * 100 / totalTasks;
            if (completionRate >= 75) {
                summary.append("Great work! Your task completion rate of ").append(completionRate)
                      .append("% shows excellent progress. ");
            } else if (completionRate >= 50) {
                summary.append("Good progress with a ").append(completionRate)
                      .append("% task completion rate. ");
            } else {
                summary.append("Your current task completion rate is ").append(completionRate)
                      .append("%. Try breaking down larger tasks into smaller steps for easier management. ");
            }
            
            // Deadline adherence assessment
            int totalTasksWithDeadline = onTimeTasks + overdueTasks;
            if (totalTasksWithDeadline > 0) {
                int onTimeRate = onTimeTasks * 100 / totalTasksWithDeadline;
                if (onTimeRate >= 80) {
                    summary.append("Excellent time management with ").append(onTimeRate)
                          .append("% of tasks completed on time. ");
                } else if (onTimeRate >= 60) {
                    summary.append("You complete ").append(onTimeRate)
                          .append("% of tasks on time. ");
                } else {
                    summary.append("Consider setting more realistic deadlines to improve your on-time completion rate of ")
                          .append(onTimeRate).append("%. ");
                }
            }
            
            // Priority management assessment
            if (highPriority > 0) {
                int highPriorityCompletion = 0; // Ideally calculate from actual data
                summary.append("Focus on completing high-priority tasks first to maximize productivity. ");
            }
            
            // Add date of analysis
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            summary.append("\n\nAnalysis as of ").append(dateFormat.format(new Date()));
        }
        
        performanceSummary.setText(summary.toString());
    }
    
    /**
     * Utility class for view operations
     */
    private static class ViewUtils {
        /**
         * Sets the width of a view based on a percentage (0-100)
         */
        public static void setViewWidth(View view, int percentage) {
            // Ensure percentage is between 0 and 100
            percentage = Math.max(0, Math.min(100, percentage));
            
            // Get the parent width
            View parent = (View) view.getParent();
            int parentWidth = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
            
            if (parentWidth <= 0) {
                // If parent width is not available yet, set using ClipDrawable level
                ClipDrawable clipDrawable = new ClipDrawable(
                        view.getBackground(), 
                        Gravity.START, 
                        ClipDrawable.HORIZONTAL);
                view.setBackground(clipDrawable);
                
                // ClipDrawable uses levels from 0 to 10000
                clipDrawable.setLevel(percentage * 100);
            } else {
                // Calculate new width based on percentage of parent width
                int newWidth = (parentWidth * percentage) / 100;
                
                // Update layout parameters
                android.view.ViewGroup.LayoutParams params = view.getLayoutParams();
                params.width = newWidth;
                view.setLayoutParams(params);
            }
        }
    }
} 