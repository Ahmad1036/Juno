package com.example.juno;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AllTasksActivity extends AppCompatActivity {

    private static final String TAG = "AllTasksActivity";

    private RecyclerView tasksRecyclerView;
    private SelectableTaskAdapter taskAdapter;
    private List<Task> taskList;
    private FloatingActionButton addTaskFab;
    private ImageButton backButton;
    private TextView emptyStateText;
    private ImageButton btnBulkActions;
    private LinearLayout bulkActionPanel;
    private TextView selectedCountTextView;
    private TextView btnMarkComplete;
    private TextView btnDelete;
    private EditText searchInput;
    private ImageButton btnClearSearch;
    
    private Chip chipDate;
    private Chip chipPriorityHigh;
    private Chip chipPriorityMedium;
    private Chip chipPriorityLow;
    private Chip chipCompleted;
    private Chip chipIncomplete;
    
    private String userId;
    private DatabaseReference mDatabase;
    
    // Filter states
    private String searchQuery = "";
    private Date[] dateRangeFilter = null;
    private Set<String> priorityFilters = new HashSet<>();
    private Boolean completionFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_tasks);

        // Check if user is logged in
        SharedPreferences prefs = getSharedPreferences("JunoUserPrefs", MODE_PRIVATE);
        userId = prefs.getString("userId", "");
        if (userId.isEmpty()) {
            // User not logged in, redirect to SignInActivity
            startActivity(new Intent(AllTasksActivity.this, SignInActivity.class));
            finish();
            return;
        }

        // Initialize Firebase Database
        mDatabase = FirebaseDatabase.getInstance("https://juno-aa7d2-default-rtdb.firebaseio.com/").getReference();

        // Initialize UI components
        initializeViews();
        setupListeners();
        setupRecyclerView();

        // Load tasks
        loadAllTasks();
    }
    
    private void initializeViews() {
        tasksRecyclerView = findViewById(R.id.tasks_recycler_view);
        addTaskFab = findViewById(R.id.add_task_fab);
        backButton = findViewById(R.id.back_button);
        emptyStateText = findViewById(R.id.empty_state_text);
        btnBulkActions = findViewById(R.id.btn_bulk_actions);
        bulkActionPanel = findViewById(R.id.bulk_action_panel);
        selectedCountTextView = findViewById(R.id.selected_count);
        btnMarkComplete = findViewById(R.id.btn_mark_complete);
        btnDelete = findViewById(R.id.btn_delete);
        searchInput = findViewById(R.id.search_input);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        
        // Initialize filter chips
        chipDate = findViewById(R.id.chip_date);
        chipPriorityHigh = findViewById(R.id.chip_priority_high);
        chipPriorityMedium = findViewById(R.id.chip_priority_medium);
        chipPriorityLow = findViewById(R.id.chip_priority_low);
        chipCompleted = findViewById(R.id.chip_completed);
        chipIncomplete = findViewById(R.id.chip_incomplete);
    }
    
    private void setupListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());
        
        // Add task button
        addTaskFab.setOnClickListener(v -> {
            Intent intent = new Intent(AllTasksActivity.this, CreateTaskActivity.class);
            startActivity(intent);
        });
        
        // Bulk actions button
        btnBulkActions.setOnClickListener(v -> toggleSelectMode());
        
        // Bulk action buttons
        btnMarkComplete.setOnClickListener(v -> markSelectedTasksComplete());
        btnDelete.setOnClickListener(v -> deleteSelectedTasks());
        
        // Search input
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim();
                applyFilters();
                
                // Show/hide clear button
                btnClearSearch.setVisibility(searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Clear search button
        btnClearSearch.setOnClickListener(v -> {
            searchInput.setText("");
            btnClearSearch.setVisibility(View.GONE);
        });
        
        // Date filter chip
        chipDate.setOnClickListener(v -> showDateRangeDialog());
        
        // Priority filter chips
        chipPriorityHigh.setOnClickListener(v -> togglePriorityFilter("high"));
        chipPriorityMedium.setOnClickListener(v -> togglePriorityFilter("medium"));
        chipPriorityLow.setOnClickListener(v -> togglePriorityFilter("low"));
        
        // Completion status filter chips
        chipCompleted.setOnClickListener(v -> toggleCompletionFilter(true));
        chipIncomplete.setOnClickListener(v -> toggleCompletionFilter(false));
    }
    
    private void setupRecyclerView() {
        taskList = new ArrayList<>();
        taskAdapter = new SelectableTaskAdapter(this, taskList, new SelectableTaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(Task task, int position) {
                if (!taskAdapter.isSelectMode()) {
                    Intent intent = new Intent(AllTasksActivity.this, TaskDetailActivity.class);
                    intent.putExtra("taskId", task.getId());
                    startActivity(intent);
                }
            }

            @Override
            public void onCheckboxClicked(Task task, int position, boolean isChecked) {
                updateTaskCompletionStatus(task);
            }

            @Override
            public void onSelectionChanged(int count) {
                updateSelectionCount(count);
            }
        });
        
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tasksRecyclerView.setAdapter(taskAdapter);
    }
    
    private void toggleSelectMode() {
        boolean newSelectMode = !taskAdapter.isSelectMode();
        taskAdapter.setSelectMode(newSelectMode);
        
        // Update UI
        bulkActionPanel.setVisibility(newSelectMode ? View.VISIBLE : View.GONE);
        btnBulkActions.setImageResource(newSelectMode ? 
                android.R.drawable.ic_menu_close_clear_cancel : 
                R.drawable.ic_select_all_24);
    }
    
    private void updateSelectionCount(int count) {
        selectedCountTextView.setText(count + " selected");
    }
    
    private void togglePriorityFilter(String priority) {
        if (priorityFilters.contains(priority)) {
            priorityFilters.remove(priority);
        } else {
            priorityFilters.add(priority);
        }
        
        applyFilters();
    }
    
    private void toggleCompletionFilter(boolean completed) {
        Chip selectedChip = completed ? chipCompleted : chipIncomplete;
        Chip otherChip = completed ? chipIncomplete : chipCompleted;
        
        if (selectedChip.isChecked() && !otherChip.isChecked()) {
            // The selected chip was the only one checked, so clearing all filters
            completionFilter = null;
            selectedChip.setChecked(false);
        } else {
            // Either both were unchecked or the other chip was checked
            completionFilter = completed;
            otherChip.setChecked(false);
        }
        
        applyFilters();
    }
    
    private void showDateRangeDialog() {
        final Calendar calendar = Calendar.getInstance();
        final Date[] selectedDateRange = new Date[2];
        final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        
        // Initialize with current date range filter if exists
        if (dateRangeFilter != null) {
            calendar.setTime(dateRangeFilter[0]);
        }
        
        // Show start date picker
        DatePickerDialog startDatePicker = new DatePickerDialog(
                this,
                R.style.DarkDatePickerDialog,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    selectedDateRange[0] = calendar.getTime();
                    
                    // After selecting start date, show end date picker
                    DatePickerDialog endDatePicker = new DatePickerDialog(
                            AllTasksActivity.this,
                            R.style.DarkDatePickerDialog,
                            (view2, endYear, endMonth, endDayOfMonth) -> {
                                calendar.set(endYear, endMonth, endDayOfMonth);
                                selectedDateRange[1] = calendar.getTime();
                                
                                // Apply the date filter
                                if (selectedDateRange[0] != null && selectedDateRange[1] != null) {
                                    dateRangeFilter = selectedDateRange;
                                    chipDate.setText("Date: " + sdf.format(selectedDateRange[0]) + 
                                            " - " + sdf.format(selectedDateRange[1]));
                                    chipDate.setChecked(true);
                                    applyFilters();
                                }
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                    );
                    
                    // Show the end date picker
                    endDatePicker.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        
        // Clear button to remove date filter
        startDatePicker.setButton(DatePickerDialog.BUTTON_NEUTRAL, "Clear Filter", (dialog, which) -> {
            dateRangeFilter = null;
            chipDate.setText("Date");
            chipDate.setChecked(false);
            applyFilters();
        });
        
        startDatePicker.show();
    }
    
    private void applyFilters() {
        String priorityFilter = null;
        
        // Get single priority if only one selected
        if (priorityFilters.size() == 1) {
            priorityFilter = priorityFilters.iterator().next();
        }
        
        taskAdapter.applyFilters(searchQuery, priorityFilter, completionFilter, dateRangeFilter);
        
        // Show/hide empty state
        updateEmptyState();
    }
    
    private void updateEmptyState() {
        if (taskAdapter.getItemCount() == 0) {
            emptyStateText.setVisibility(View.VISIBLE);
            tasksRecyclerView.setVisibility(View.GONE);
            
            if (!searchQuery.isEmpty() || dateRangeFilter != null || 
                    !priorityFilters.isEmpty() || completionFilter != null) {
                emptyStateText.setText("No tasks match your filters.");
            } else {
                emptyStateText.setText("No tasks found. Add a task to get started!");
            }
        } else {
            emptyStateText.setVisibility(View.GONE);
            tasksRecyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    private void markSelectedTasksComplete() {
        List<Task> selectedTasks = taskAdapter.getSelectedTasks();
        
        if (selectedTasks.isEmpty()) {
            Toast.makeText(this, "No tasks selected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DarkAlertDialog);
        builder.setTitle("Mark Tasks as Complete");
        builder.setMessage("Are you sure you want to mark " + selectedTasks.size() + " task(s) as complete?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            for (Task task : selectedTasks) {
                task.setCompleted(true);
                updateTaskCompletionStatus(task);
            }
            
            // Exit selection mode after operation
            toggleSelectMode();
            
            Toast.makeText(this, selectedTasks.size() + " task(s) marked as complete", 
                    Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void deleteSelectedTasks() {
        List<Task> selectedTasks = taskAdapter.getSelectedTasks();
        
        if (selectedTasks.isEmpty()) {
            Toast.makeText(this, "No tasks selected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DarkAlertDialog);
        builder.setTitle("Delete Tasks");
        builder.setMessage("Are you sure you want to delete " + selectedTasks.size() + " task(s)? This action cannot be undone.");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            for (Task task : selectedTasks) {
                deleteTask(task.getId());
            }
            
            // Exit selection mode after operation
            toggleSelectMode();
            
            Toast.makeText(this, selectedTasks.size() + " task(s) deleted", 
                    Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void updateTaskCompletionStatus(Task task) {
        mDatabase.child("users").child(userId).child("tasks").child(task.getId()).child("completed")
                .setValue(task.isCompleted())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task completion status updated for: " + task.getId());
                    // Refresh the task list
                    refreshTaskList();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating task completion status", e);
                    Toast.makeText(AllTasksActivity.this, "Failed to update task status", 
                            Toast.LENGTH_SHORT).show();
                });
    }
    
    private void deleteTask(String taskId) {
        mDatabase.child("users").child(userId).child("tasks").child(taskId)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task deleted: " + taskId);
                    // Refresh the task list
                    refreshTaskList();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting task", e);
                    Toast.makeText(AllTasksActivity.this, "Failed to delete task", 
                            Toast.LENGTH_SHORT).show();
                });
    }
    
    /**
     * Refreshes the local task list from the current filtered list
     * without loading from Firebase again
     */
    private void refreshTaskList() {
        // Get the current filtered list
        List<Task> filteredList = new ArrayList<>(taskAdapter.getFilteredTaskList());
        
        // Update the adapter with the refreshed list
        taskAdapter.updateData(filteredList);
        
        // Apply filters
        applyFilters();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh tasks list when returning to this activity
        loadAllTasks();
    }

    private void loadAllTasks() {
        Log.d(TAG, "Loading all tasks for user ID: " + userId);
        
        if (userId.isEmpty()) {
            Log.e(TAG, "User ID is empty, cannot load tasks");
            Toast.makeText(this, "Authentication error: User ID not found", 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading state
        emptyStateText.setText("Loading tasks...");
        emptyStateText.setVisibility(View.VISIBLE);
        tasksRecyclerView.setVisibility(View.GONE);
        
        mDatabase.child("users").child(userId).child("tasks")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        taskList.clear();
                        
                        for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                            try {
                                Task task = taskSnapshot.getValue(Task.class);
                                if (task != null) {
                                    task.setId(taskSnapshot.getKey());
                                    
                                    // Skip the default task
                                    if (task.getTitle() != null && 
                                        !task.getTitle().toLowerCase().contains("i need to study")) {
                                        taskList.add(task);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing task", e);
                            }
                        }
                        
                        // Update adapter with new data
                        taskAdapter.updateData(taskList);
                        
                        // Apply any existing filters
                        applyFilters();
                        
                        // Update empty state visibility
                        updateEmptyState();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.w(TAG, "loadTasks:onCancelled", databaseError.toException());
                        Toast.makeText(AllTasksActivity.this, 
                                "Failed to load tasks: " + databaseError.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        
                        emptyStateText.setText("Failed to load tasks. Please try again.");
                        emptyStateText.setVisibility(View.VISIBLE);
                        tasksRecyclerView.setVisibility(View.GONE);
                    }
                });
    }
} 