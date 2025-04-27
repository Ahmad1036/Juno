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

public class AllChecklistsActivity extends AppCompatActivity implements AllChecklistsAdapter.OnTaskActionListener {

    private static final String TAG = "AllChecklistsActivity";

    // UI Components
    private RecyclerView tasksRecyclerView;
    private TextView emptyStateText;
    private EditText searchInput;
    private ImageButton clearSearchButton;
    private LinearLayout bulkActionsLayout;
    private TextView selectedCountText;
    private Button markCompleteButton;
    private Button deleteSelectedButton;
    private Chip dateFilterChip, highPriorityChip, mediumPriorityChip, lowPriorityChip, completedChip, incompleteChip, resetFiltersChip;
    
    // Data
    private AllChecklistsAdapter adapter;
    private List<Task> allTasks = new ArrayList<>();
    private String userId;
    private DatabaseReference mDatabase;
    private List<String> taskListNames = new ArrayList<>();
    
    // Filter state
    private String currentSearchQuery = "";
    private Set<String> selectedPriorities = new HashSet<>();
    private boolean filterShowCompleted = true;
    private boolean filterShowIncomplete = true;
    private long filterStartDate = 0;
    private long filterEndDate = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_checklists);
        
        // Check user login
        SharedPreferences prefs = getSharedPreferences("JunoUserPrefs", MODE_PRIVATE);
        userId = prefs.getString("userId", "");
        if (userId.isEmpty()) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }
        
        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance("https://juno-aa7d2-default-rtdb.firebaseio.com/").getReference();
        
        // Initialize UI components
        initializeViews();
        
        // Set up RecyclerView
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AllChecklistsAdapter(this, allTasks, this);
        tasksRecyclerView.setAdapter(adapter);
        
        // Set up listeners
        setupListeners();
        
        // Load task lists first (needed for task list names)
        loadTaskLists();
        
        // Load all tasks
        loadAllTasks();
    }
    
    private void initializeViews() {
        // Main views
        tasksRecyclerView = findViewById(R.id.tasks_recycler_view);
        emptyStateText = findViewById(R.id.empty_state_text);
        
        // Search
        searchInput = findViewById(R.id.search_input);
        clearSearchButton = findViewById(R.id.clear_search_button);
        
        // Bulk actions
        bulkActionsLayout = findViewById(R.id.bulk_actions_layout);
        selectedCountText = findViewById(R.id.selected_count_text);
        markCompleteButton = findViewById(R.id.mark_complete_button);
        deleteSelectedButton = findViewById(R.id.delete_selected_button);
        
        // Filter chips
        dateFilterChip = findViewById(R.id.date_filter_chip);
        highPriorityChip = findViewById(R.id.high_priority_chip);
        mediumPriorityChip = findViewById(R.id.medium_priority_chip);
        lowPriorityChip = findViewById(R.id.low_priority_chip);
        completedChip = findViewById(R.id.completed_chip);
        incompleteChip = findViewById(R.id.incomplete_chip);
        resetFiltersChip = findViewById(R.id.reset_filters_chip);
        
        // Initially check both completed and incomplete tasks
        completedChip.setChecked(true);
        incompleteChip.setChecked(true);
    }
    
    private void setupListeners() {
        // Back button
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        
        // FAB for adding task
        findViewById(R.id.add_task_fab).setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateTaskActivity.class);
            startActivity(intent);
        });
        
        // Search functionality
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                clearSearchButton.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilters();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        clearSearchButton.setOnClickListener(v -> {
            searchInput.setText("");
            clearSearchButton.setVisibility(View.GONE);
        });
        
        // Bulk action buttons
        markCompleteButton.setOnClickListener(v -> {
            Log.d(TAG, "Mark complete button clicked");
            List<Task> selectedTasks = adapter.getSelectedTasks();
            Log.d(TAG, "Selected tasks count: " + selectedTasks.size());
            if (!selectedTasks.isEmpty()) {
                showMarkCompletionDialog(selectedTasks);
            } else {
                Toast.makeText(this, "No tasks selected", Toast.LENGTH_SHORT).show();
            }
        });
        
        deleteSelectedButton.setOnClickListener(v -> {
            Log.d(TAG, "Delete selected button clicked");
            List<Task> selectedTasks = adapter.getSelectedTasks();
            Log.d(TAG, "Selected tasks count: " + selectedTasks.size());
            if (!selectedTasks.isEmpty()) {
                showDeleteConfirmationDialog(selectedTasks);
            } else {
                Toast.makeText(this, "No tasks selected", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Filter chips
        dateFilterChip.setOnClickListener(v -> showDateRangeDialog());
        
        highPriorityChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedPriorities.add("high");
            } else {
                selectedPriorities.remove("high");
            }
            applyFilters();
        });
        
        mediumPriorityChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedPriorities.add("medium");
            } else {
                selectedPriorities.remove("medium");
            }
            applyFilters();
        });
        
        lowPriorityChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedPriorities.add("low");
            } else {
                selectedPriorities.remove("low");
            }
            applyFilters();
        });
        
        completedChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            filterShowCompleted = isChecked;
            applyFilters();
        });
        
        incompleteChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            filterShowIncomplete = isChecked;
            applyFilters();
        });
        
        resetFiltersChip.setOnClickListener(v -> resetFilters());
    }
    
    private void resetFilters() {
        // Reset search
        searchInput.setText("");
        currentSearchQuery = "";
        clearSearchButton.setVisibility(View.GONE);
        
        // Reset priority filters
        highPriorityChip.setChecked(false);
        mediumPriorityChip.setChecked(false);
        lowPriorityChip.setChecked(false);
        selectedPriorities.clear();
        
        // Reset status filters
        completedChip.setChecked(true);
        incompleteChip.setChecked(true);
        filterShowCompleted = true;
        filterShowIncomplete = true;
        
        // Reset date filter
        dateFilterChip.setChecked(false);
        filterStartDate = 0;
        filterEndDate = 0;
        
        // Apply the reset filters
        applyFilters();
    }
    
    private void showDateRangeDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        
        // Variables to store selected dates
        final long[] startDate = {0};
        final long[] endDate = {0};
        
        // Create start date picker dialog
        DatePickerDialog startDateDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            Calendar startCal = Calendar.getInstance();
            startCal.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0);
            startDate[0] = startCal.getTimeInMillis();
            
            // Now show end date picker
            DatePickerDialog endDateDialog = new DatePickerDialog(this, (view2, selectedYear2, selectedMonth2, selectedDay2) -> {
                Calendar endCal = Calendar.getInstance();
                endCal.set(selectedYear2, selectedMonth2, selectedDay2, 23, 59, 59);
                endDate[0] = endCal.getTimeInMillis();
                
                // Apply date filter
                if (startDate[0] > 0 && endDate[0] > 0) {
                    filterStartDate = startDate[0];
                    filterEndDate = endDate[0];
                    dateFilterChip.setChecked(true);
                    
                    // Show date range on chip
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
                    String dateRange = sdf.format(new Date(filterStartDate)) + 
                            " - " + 
                            sdf.format(new Date(filterEndDate));
                    dateFilterChip.setText("Date: " + dateRange);
                    
                    applyFilters();
                }
            }, year, month, day);
            endDateDialog.setTitle("Select End Date");
            endDateDialog.show();
        }, year, month, day);
        
        startDateDialog.setTitle("Select Start Date");
        startDateDialog.show();
    }
    
    private void applyFilters() {
        adapter.applyFilters(
                currentSearchQuery,
                selectedPriorities,
                filterShowCompleted,
                filterShowIncomplete,
                filterStartDate,
                filterEndDate
        );
        
        // Update empty state visibility
        if (adapter.getItemCount() == 0) {
            emptyStateText.setVisibility(View.VISIBLE);
            tasksRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            tasksRecyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    private void loadTaskLists() {
        mDatabase.child("users").child(userId).child("task_lists").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                taskListNames.clear();
                
                // Add a default entry at position 0
                taskListNames.add("Default");
                
                for (DataSnapshot listSnapshot : dataSnapshot.getChildren()) {
                    String listName = listSnapshot.getValue(String.class);
                    if (listName != null) {
                        taskListNames.add(listName);
                    }
                }
                
                // We've loaded the list names, now refresh the adapter if it has tasks
                if (adapter != null && !allTasks.isEmpty()) {
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading task lists", databaseError.toException());
            }
        });
    }
    
    private void loadAllTasks() {
        mDatabase.child("users").child(userId).child("tasks").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allTasks.clear();
                
                for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                    Task task = taskSnapshot.getValue(Task.class);
                    if (task != null) {
                        task.setId(taskSnapshot.getKey());
                        allTasks.add(task);
                    }
                }
                
                adapter.updateData(allTasks);
                
                // Show/hide empty state
                if (allTasks.isEmpty()) {
                    emptyStateText.setVisibility(View.VISIBLE);
                    tasksRecyclerView.setVisibility(View.GONE);
                } else {
                    emptyStateText.setVisibility(View.GONE);
                    tasksRecyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading tasks", databaseError.toException());
                Toast.makeText(AllChecklistsActivity.this, "Failed to load tasks", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showMarkCompletionDialog(List<Task> selectedTasks) {
        // Count how many are already completed
        int completedCount = 0;
        for (Task task : selectedTasks) {
            if (task.isCompleted()) {
                completedCount++;
            }
        }
        
        boolean markAsComplete = completedCount < selectedTasks.size() / 2;
        String message = markAsComplete ?
                "Mark " + selectedTasks.size() + " tasks as complete?" :
                "Mark " + selectedTasks.size() + " tasks as incomplete?";
        
        new AlertDialog.Builder(this)
                .setTitle("Change Task Status")
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> {
                    batchUpdateTaskCompletion(selectedTasks, markAsComplete);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void batchUpdateTaskCompletion(List<Task> tasks, boolean markAsComplete) {
        if (tasks.isEmpty()) {
            Log.d(TAG, "No tasks to update");
            return;
        }
        
        Log.d(TAG, "Starting batch update of " + tasks.size() + " tasks to " + (markAsComplete ? "complete" : "incomplete"));
        
        // Immediately update the tasks in local list for immediate UI feedback
        for (Task task : tasks) {
            // Only mark tasks that need to be changed
            if (task.isCompleted() != markAsComplete) {
                task.setCompleted(markAsComplete);
                
                // Find and update this task in allTasks
                for (Task t : allTasks) {
                    if (t.getId().equals(task.getId())) {
                        t.setCompleted(markAsComplete);
                        break;
                    }
                }
            }
        }
        
        // Refresh adapter immediately so user sees changes
        adapter.updateData(allTasks);
        
        // Counter for how many updates have completed in the background
        final int[] completedUpdates = {0};
        final int totalUpdates = tasks.size();
        
        // Now update the database in the background
        for (Task task : tasks) {
            // Only update if the status is different from original
            if (task.isCompleted() == markAsComplete) {
                Log.d(TAG, "Updating task: " + task.getId() + " to " + (markAsComplete ? "complete" : "incomplete"));
                
                mDatabase.child("users").child(userId).child("tasks").child(task.getId())
                        .child("completed").setValue(markAsComplete)
                        .addOnCompleteListener(task1 -> {
                            completedUpdates[0]++;
                            Log.d(TAG, "Task updated: " + task.getId() + ", Progress: " + completedUpdates[0] + "/" + totalUpdates);
                            
                            // Check if all updates are done
                            if (completedUpdates[0] >= totalUpdates) {
                                Log.d(TAG, "All tasks updated successfully");
                                Toast.makeText(AllChecklistsActivity.this, 
                                        "Updated " + totalUpdates + " tasks", 
                                        Toast.LENGTH_SHORT).show();
                                
                                // Exit selection mode
                                if (adapter.isInSelectionMode()) {
                                    toggleSelectionMode(false);
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to update task: " + task.getId(), e);
                            Toast.makeText(AllChecklistsActivity.this,
                                    "Failed to update some tasks",
                                    Toast.LENGTH_SHORT).show();
                        });
            } else {
                // Count this as already completed
                completedUpdates[0]++;
                Log.d(TAG, "Task already in desired state: " + task.getId() + ", Progress: " + completedUpdates[0] + "/" + totalUpdates);
            }
        }
    }
    
    private void showDeleteConfirmationDialog(List<Task> selectedTasks) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Tasks")
                .setMessage("Are you sure you want to delete " + selectedTasks.size() + " tasks? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    batchDeleteTasks(selectedTasks);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void batchDeleteTasks(List<Task> tasks) {
        if (tasks.isEmpty()) {
            Log.d(TAG, "No tasks to delete");
            return;
        }
        
        Log.d(TAG, "Starting batch delete of " + tasks.size() + " tasks");
        
        // Create a list of task IDs to be deleted for tracking
        final List<String> taskIdsToDelete = new ArrayList<>();
        for (Task task : tasks) {
            taskIdsToDelete.add(task.getId());
            Log.d(TAG, "Will delete task: " + task.getId() + " - " + task.getTitle());
        }
        
        // Immediately remove tasks from local list for UI feedback
        allTasks.removeIf(t -> taskIdsToDelete.contains(t.getId()));
        adapter.updateData(allTasks);
        
        // Counter for how many deletes have completed in background
        final int[] completedDeletes = {0};
        final int totalDeletes = tasks.size();
        
        // Now delete from database in background
        for (Task task : tasks) {
            mDatabase.child("users").child(userId).child("tasks").child(task.getId())
                    .removeValue()
                    .addOnCompleteListener(task1 -> {
                        completedDeletes[0]++;
                        Log.d(TAG, "Task deleted: " + task.getId() + ", Progress: " + completedDeletes[0] + "/" + totalDeletes);
                        
                        // Check if all deletes are done
                        if (completedDeletes[0] >= totalDeletes) {
                            Log.d(TAG, "All tasks deleted successfully");
                            Toast.makeText(AllChecklistsActivity.this, 
                                    "Deleted " + totalDeletes + " tasks", 
                                    Toast.LENGTH_SHORT).show();
                            
                            // Exit selection mode
                            if (adapter.isInSelectionMode()) {
                                toggleSelectionMode(false);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to delete task: " + task.getId(), e);
                        Toast.makeText(AllChecklistsActivity.this,
                                "Failed to delete some tasks",
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }
    
    private void toggleSelectionMode(boolean enabled) {
        adapter.toggleSelectionMode(enabled);
        bulkActionsLayout.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }
    
    // Implement OnTaskActionListener methods
    
    @Override
    public void onTaskClick(Task task, int position) {
        // Navigate to task detail
        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra("taskId", task.getId());
        startActivity(intent);
    }
    
    @Override
    public void onCheckboxClicked(Task task, int position, boolean isChecked) {
        // Update task completion status in database
        mDatabase.child("users").child(userId).child("tasks").child(task.getId())
                .child("completed").setValue(isChecked)
                .addOnSuccessListener(aVoid -> {
                    String message = isChecked ? "Task marked as complete" : "Task marked as incomplete";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update task status", Toast.LENGTH_SHORT).show();
                    // Revert the change in the adapter
                    task.setCompleted(!isChecked);
                    adapter.notifyItemChanged(position);
                });
    }
    
    @Override
    public void onTaskSelected(int selectedCount) {
        Log.d(TAG, "onTaskSelected called with count: " + selectedCount + ", selection mode: " + adapter.isInSelectionMode());
        
        if (selectedCount > 0) {
            // Show bulk actions
            bulkActionsLayout.setVisibility(View.VISIBLE);
            selectedCountText.setText(selectedCount + " selected");
            
            // Make sure we're in selection mode if items are selected
            if (!adapter.isInSelectionMode()) {
                Log.d(TAG, "Forcing selection mode to true as items are selected");
                adapter.toggleSelectionMode(true);
            }
        } else {
            // Hide bulk actions if no tasks selected
            bulkActionsLayout.setVisibility(View.GONE);
            
            // Exit selection mode if count is 0
            if (adapter.isInSelectionMode()) {
                Log.d(TAG, "Exiting selection mode as count is 0");
                adapter.toggleSelectionMode(false);
            }
        }
    }
    
    @Override
    public String getTaskListName(int listId) {
        if (listId >= 0 && listId < taskListNames.size()) {
            return taskListNames.get(listId);
        }
        return null;
    }
    
    @Override
    public void onBackPressed() {
        // If in selection mode, exit it instead of going back
        if (adapter != null && adapter.isInSelectionMode()) {
            toggleSelectionMode(false);
        } else {
            super.onBackPressed();
        }
    }
} 