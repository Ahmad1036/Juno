package com.example.juno;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity implements CalendarEventAdapter.OnEventClickListener {
    
    // UI Components
    private TextView calendarTitle;
    private TextView currentDateText;
    private ImageButton addEventButton;
    private ImageButton previousDateButton;
    private ImageButton nextDateButton;
    private ImageButton backButton;
    private TextView toggleDayView;
    private TextView toggleWeekView;
    private TextView toggleMonthView;
    private View dayView;
    private View weekView;
    private View monthView;
    private RecyclerView eventsRecyclerView;
    private LinearLayout dayViewContainer;
    
    // Data
    private CalendarEventAdapter eventAdapter;
    private CalendarEventManager eventManager;
    private Calendar currentCalendar;
    private int currentViewMode = 0; // 0 = day, 1 = week, 2 = month
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat monthYearFormat;
    private SimpleDateFormat dayOfWeekFormat;
    private String userId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        
        // Get the user ID from shared preferences
        UserSessionManager sessionManager = new UserSessionManager(this);
        userId = sessionManager.getUserId();
        
        if (userId.isEmpty()) {
            // Handle case where user ID is not available
            Toast.makeText(this, "User information not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize components
        initializeViews();
        initializeCalendar();
        setupClickListeners();
        
        // Set up RecyclerView for events
        eventsRecyclerView = findViewById(R.id.events_recycler_view);
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventAdapter = new CalendarEventAdapter(this, this);
        eventsRecyclerView.setAdapter(eventAdapter);
        
        // Initialize event manager
        eventManager = new CalendarEventManager();
        
        // Check if we have a date passed from another activity
        if (getIntent().hasExtra("SELECTED_DATE")) {
            long dateMillis = getIntent().getLongExtra("SELECTED_DATE", 0);
            if (dateMillis > 0) {
                currentCalendar.setTimeInMillis(dateMillis);
            }
        }
        
        // Default to day view
        switchToView(0);
        loadEventsForCurrentView();
    }
    
    private void initializeViews() {
        calendarTitle = findViewById(R.id.calendar_title);
        currentDateText = findViewById(R.id.current_date_text);
        addEventButton = findViewById(R.id.add_event_button);
        previousDateButton = findViewById(R.id.previous_date);
        nextDateButton = findViewById(R.id.next_date);
        backButton = findViewById(R.id.calendar_back_button);
        toggleDayView = findViewById(R.id.toggle_day);
        toggleWeekView = findViewById(R.id.toggle_week);
        toggleMonthView = findViewById(R.id.toggle_month);
        dayView = findViewById(R.id.day_view);
        weekView = findViewById(R.id.week_view);
        monthView = findViewById(R.id.month_view);
        dayViewContainer = findViewById(R.id.day_events_container);
    }
    
    private void initializeCalendar() {
        currentCalendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        dayOfWeekFormat = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
        updateCurrentDateDisplay();
    }
    
    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());
        
        // Toggle view buttons
        toggleDayView.setOnClickListener(v -> openDayView());
        toggleWeekView.setOnClickListener(v -> switchToView(1));
        toggleMonthView.setOnClickListener(v -> switchToView(2));
        
        // Date navigation buttons
        previousDateButton.setOnClickListener(v -> navigateToPrevious());
        nextDateButton.setOnClickListener(v -> navigateToNext());
        
        // Date selection - when currentDateText is clicked, show date picker
        currentDateText.setOnClickListener(v -> showDatePicker());
        
        // Add event button
        addEventButton.setOnClickListener(v -> openAddEventActivity());
    }
    
    private void openDayView() {
        Intent intent = new Intent(this, DayCalendarActivity.class);
        intent.putExtra("SELECTED_DATE", currentCalendar.getTimeInMillis());
        startActivity(intent);
    }
    
    private void openAddEventActivity() {
        Intent intent = new Intent(this, AddEditEventActivity.class);
        intent.putExtra("SELECTED_DATE", currentCalendar.getTimeInMillis());
        startActivity(intent);
    }
    private void switchToView(int viewMode) {
        currentViewMode = viewMode;
        
        // Update UI for selected view
        toggleDayView.setBackgroundResource(viewMode == 0 ? R.drawable.calendar_toggle_selected : 0);
        toggleDayView.setTextColor(getResources().getColor(viewMode == 0 ? android.R.color.white : R.color.dark_gray));
        
        toggleWeekView.setBackgroundResource(viewMode == 1 ? R.drawable.calendar_toggle_selected : 0);
        toggleWeekView.setTextColor(getResources().getColor(viewMode == 1 ? android.R.color.white : R.color.dark_gray));
        
        toggleMonthView.setBackgroundResource(viewMode == 2 ? R.drawable.calendar_toggle_selected : 0);
        toggleMonthView.setTextColor(getResources().getColor(viewMode == 2 ? android.R.color.white : R.color.dark_gray));
        
        // Show the correct view
        dayView.setVisibility(viewMode == 0 ? View.VISIBLE : View.GONE);
        weekView.setVisibility(viewMode == 1 ? View.VISIBLE : View.GONE);
        monthView.setVisibility(viewMode == 2 ? View.VISIBLE : View.GONE);
        
        // Update date display
        updateCurrentDateDisplay();
        
        // Load events for current view
        loadEventsForCurrentView();
    }
    
    private void navigateToPrevious() {
        switch (currentViewMode) {
            case 0: // Day view
                currentCalendar.add(Calendar.DAY_OF_MONTH, -1);
                break;
            case 1: // Week view
                currentCalendar.add(Calendar.WEEK_OF_YEAR, -1);
                break;
            case 2: // Month view
                currentCalendar.add(Calendar.MONTH, -1);
                break;
        }
        updateCurrentDateDisplay();
        loadEventsForCurrentView();
    }
    
    private void navigateToNext() {
        switch (currentViewMode) {
            case 0: // Day view
                currentCalendar.add(Calendar.DAY_OF_MONTH, 1);
                break;
            case 1: // Week view
                currentCalendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case 2: // Month view
                currentCalendar.add(Calendar.MONTH, 1);
                break;
        }
        updateCurrentDateDisplay();
        loadEventsForCurrentView();
    }
    
    private void updateCurrentDateDisplay() {
        switch (currentViewMode) {
            case 0: // Day view
                currentDateText.setText(dateFormat.format(currentCalendar.getTime()));
                break;
            case 1: // Week view
                // Get first day of week
                Calendar firstDayOfWeek = (Calendar) currentCalendar.clone();
                firstDayOfWeek.set(Calendar.DAY_OF_WEEK, firstDayOfWeek.getFirstDayOfWeek());
                Calendar lastDayOfWeek = (Calendar) firstDayOfWeek.clone();
                lastDayOfWeek.add(Calendar.DAY_OF_MONTH, 6);
                
                currentDateText.setText(dayOfWeekFormat.format(firstDayOfWeek.getTime()) + 
                                       " - " + 
                                       dayOfWeekFormat.format(lastDayOfWeek.getTime()));
                break;
            case 2: // Month view
                currentDateText.setText(monthYearFormat.format(currentCalendar.getTime()));
                break;
        }
    }
    
    private void loadEventsForCurrentView() {
        switch (currentViewMode) {
            case 0: // Day view
                loadEventsForDay();
                break;
            case 1: // Week view
                loadEventsForWeek();
                break;
            case 2: // Month view
                loadEventsForMonth();
                break;
        }
    }
    
    private void loadEventsForDay() {
        // We'll use our new detailed day view instead
        openDayView();
    }
    
    private void loadEventsForWeek() {
        // Clone current calendar to avoid modifying it
        Calendar startOfWeek = (Calendar) currentCalendar.clone();
        // Set to first day of week
        startOfWeek.set(Calendar.DAY_OF_WEEK, startOfWeek.getFirstDayOfWeek());
        
        // Get start and end of week
        Calendar weekStart = (Calendar) startOfWeek.clone();
        weekStart.set(Calendar.HOUR_OF_DAY, 0);
        weekStart.set(Calendar.MINUTE, 0);
        weekStart.set(Calendar.SECOND, 0);
        weekStart.set(Calendar.MILLISECOND, 0);
        
        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.DAY_OF_MONTH, 7);
        
        // We'll now load user's tasks for the whole week
        loadUserTasksForDateRange(weekStart.getTime(), weekEnd.getTime());
    }
    
    private void loadEventsForMonth() {
        // Get start and end of month
        Calendar monthStart = (Calendar) currentCalendar.clone();
        monthStart.set(Calendar.DAY_OF_MONTH, 1);
        monthStart.set(Calendar.HOUR_OF_DAY, 0);
        monthStart.set(Calendar.MINUTE, 0);
        monthStart.set(Calendar.SECOND, 0);
        monthStart.set(Calendar.MILLISECOND, 0);
        
        Calendar monthEnd = (Calendar) monthStart.clone();
        monthEnd.add(Calendar.MONTH, 1);
        
        // We'll now load user's tasks for the whole month
        loadUserTasksForDateRange(monthStart.getTime(), monthEnd.getTime());
    }
    
    private void loadUserTasksForDate(Date date) {
        Calendar startOfDay = Calendar.getInstance();
        startOfDay.setTime(date);
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);
        
        Calendar endOfDay = Calendar.getInstance();
        endOfDay.setTime(date);
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);
        
        loadUserTasksForDateRange(startOfDay.getTime(), endOfDay.getTime());
    }
    
    private void loadUserTasksForDateRange(Date startDate, Date endDate) {
        // Reference to Firebase Realtime Database tasks for this user
        DatabaseReference tasksRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("tasks");
        
        // Show a loading message
        Toast.makeText(this, "Loading tasks...", Toast.LENGTH_SHORT).show();
        
        // Query tasks from the database
        tasksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<CalendarEvent> events = new ArrayList<>();
                
                for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                    try {
                        Task task = taskSnapshot.getValue(Task.class);
                        if (task != null) {
                            task.setId(taskSnapshot.getKey());
                            
                            // Skip the default task if it exists
                            if (task.getTitle() != null && 
                                task.getTitle().toLowerCase().contains("i need to study")) {
                                continue;
                            }
                            
                            // Process only tasks with due dates
                            if (task.getDueDate() > 0) {
                                Date taskDueDate = new Date(task.getDueDate());
                                
                                // Check if within the date range for the current view
                                if ((taskDueDate.after(startDate) || taskDueDate.equals(startDate)) && 
                                    taskDueDate.before(endDate)) {
                                    
                                    // Convert the task to a calendar event
                                    CalendarEvent event = createEventFromTask(task, taskDueDate);
                                    events.add(event);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("CalendarActivity", "Error processing task", e);
                    }
                }
                
                runOnUiThread(() -> {
                    // Update adapter with events
                    if (events.isEmpty()) {
                        Toast.makeText(CalendarActivity.this, 
                                      "No tasks found for this time period", 
                                      Toast.LENGTH_SHORT).show();
                    }
                    
                    eventAdapter.setEvents(events);
                    
                    // Populate the appropriate view based on current mode
                    switch (currentViewMode) {
                        case 0: // Day view
                            populateDayView(events);
                            break;
                        case 1: // Week view
                            populateWeekView(events);
                            break;
                        case 2: // Month view
                            populateMonthView(events);
                            break;
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                runOnUiThread(() -> 
                    Toast.makeText(CalendarActivity.this, 
                                  "Error loading tasks: " + databaseError.getMessage(), 
                                  Toast.LENGTH_SHORT).show());
            }
        });
    }
    
    private CalendarEvent createEventFromTask(Task task, Date dueDate) {
        // Create a calendar event from a task
        CalendarEvent event = new CalendarEvent();
        event.setId(task.getId());
        event.setUserId(userId);
        event.setTitle(task.getTitle());
        event.setDescription(task.getDescription());
        
        // Set the start time as the due date (at 9:00 AM)
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dueDate);
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        event.setStartTime(calendar.getTimeInMillis());
        
        // Set the end time as 1 hour after start time
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        event.setEndTime(calendar.getTimeInMillis());
        
        // Set completed status based on task completion
        event.setCompleted(task.isCompleted());
        
        // Set emoji based on task priority - using your task's priority format
        if (task.getPriority() != null) {
            switch (task.getPriority().toLowerCase()) {
                case "high":
                    event.setEmoji("🔴");
                    break;
                case "medium":
                    event.setEmoji("🟠");
                    break;
                case "low":
                    event.setEmoji("🟢");
                    break;
                default:
                    event.setEmoji("📝");
                    break;
            }
        } else {
            event.setEmoji("📝");
        }
        
        return event;
    }
    
    private void populateDayView(List<CalendarEvent> events) {
        // Clear previous views
        dayViewContainer.removeAllViews();
        
        // Sort events by start time
        Collections.sort(events, (e1, e2) -> Long.compare(e1.getStartTime(), e2.getStartTime()));
        
        // Create time slots for the day (8am to 8pm for example)
        for (int hour = 8; hour <= 20; hour++) {
            // Create a container for this hour
            LinearLayout hourContainer = new LinearLayout(this);
            hourContainer.setOrientation(LinearLayout.HORIZONTAL);
            hourContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            
            // Create the time label (left side)
            TextView timeLabel = new TextView(this);
            String timeText = hour > 12 ? (hour - 12) + ":00 PM" : hour + ":00 AM";
            timeLabel.setText(timeText);
            timeLabel.setTextSize(12);
            timeLabel.setPadding(16, 16, 16, 16);
            timeLabel.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));
            hourContainer.addView(timeLabel);
            
            // Create a vertical line divider
            View divider = new View(this);
            divider.setBackgroundColor(Color.LTGRAY);
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(2, 
                    LinearLayout.LayoutParams.MATCH_PARENT);
            divider.setLayoutParams(dividerParams);
            hourContainer.addView(divider);
            
            // Create a container for events in this hour
            LinearLayout eventsContainer = new LinearLayout(this);
            eventsContainer.setOrientation(LinearLayout.VERTICAL);
            eventsContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f));
            hourContainer.addView(eventsContainer);
            
            // Filter events for this hour
            List<CalendarEvent> hourEvents = filterEventsForHour(events, hour);
            
            if (hourEvents.isEmpty()) {
                // Add a placeholder or leave blank
                TextView emptySlot = new TextView(this);
                emptySlot.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        convertDpToPx(30)));
                eventsContainer.addView(emptySlot);
            } else {
                // Add event cards for this hour
                for (CalendarEvent event : hourEvents) {
                    addEventCardToView(eventsContainer, event);
                }
            }
            
            // Add a horizontal line below the hour
            View bottomDivider = new View(this);
            bottomDivider.setBackgroundColor(Color.LTGRAY);
            LinearLayout.LayoutParams bottomDividerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            bottomDivider.setLayoutParams(bottomDividerParams);
            
            // Add the hour container and divider to the main container
            dayViewContainer.addView(hourContainer);
            dayViewContainer.addView(bottomDivider);
        }
    }
    
    private void addEventCardToView(LinearLayout container, CalendarEvent event) {
        // Inflate the card layout - we'll create a layout similar to your task items
        View eventCard = getLayoutInflater().inflate(R.layout.item_calendar_event, container, false);
        
        // Set up the card
        TextView titleText = eventCard.findViewById(R.id.event_title);
        TextView timeText = eventCard.findViewById(R.id.event_time);
        TextView descriptionText = eventCard.findViewById(R.id.event_description);
        
        // Set the values
        titleText.setText(event.getTitle());
        
        // Format the time
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String eventTimeText = timeFormat.format(new Date(event.getStartTime()));
        if (event.getEndTime() > 0) {
            eventTimeText += " - " + timeFormat.format(new Date(event.getEndTime()));
        }
        timeText.setText(eventTimeText);
        
        // Set description (if any)
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            descriptionText.setText(event.getDescription());
            descriptionText.setVisibility(View.VISIBLE);
        } else {
            descriptionText.setVisibility(View.GONE);
        }
        
        // Add click handlers
        eventCard.setOnClickListener(v -> openTaskDetails(event.getId()));
        
        // Add the card to the container
        container.addView(eventCard);
    }
    
    private List<CalendarEvent> filterEventsForHour(List<CalendarEvent> events, int hour) {
        List<CalendarEvent> hourEvents = new ArrayList<>();
        
        for (CalendarEvent event : events) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(event.getStartTime());
            int eventHour = cal.get(Calendar.HOUR_OF_DAY);
            
            if (eventHour == hour) {
                hourEvents.add(event);
            }
        }
        
        return hourEvents;
    }
    
    private void openTaskDetails(String taskId) {
        // Open task details similar to what you do in AllTasksActivity
        DatabaseReference taskRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("tasks").child(taskId);
                
        taskRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Task task = dataSnapshot.getValue(Task.class);
                if (task != null) {
                    task.setId(dataSnapshot.getKey());
                    
                    // Launch EditTaskActivity with this task
                    Intent intent = new Intent(CalendarActivity.this, CreateTaskActivity.class);
                    intent.putExtra("TASK_ID", task.getId());
                    intent.putExtra("TASK_TITLE", task.getTitle());
                    intent.putExtra("TASK_DESCRIPTION", task.getDescription());
                    intent.putExtra("TASK_DUE_DATE", task.getDueDate());
                    intent.putExtra("TASK_PRIORITY", task.getPriority());
                    intent.putExtra("TASK_COMPLETED", task.isCompleted());
                    // Add image URL if available
                    if (task.getImageUrl() != null) {
                        intent.putExtra("TASK_IMAGE_URL", task.getImageUrl());
                    }
                    startActivity(intent);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(CalendarActivity.this, 
                              "Error loading task: " + databaseError.getMessage(), 
                              Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateTaskCompletionStatus(String taskId, boolean isCompleted) {
        // Update task completion status in Firebase
        DatabaseReference taskRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("tasks").child(taskId);
                
        taskRef.child("completed").setValue(isCompleted)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CalendarActivity.this, 
                                  isCompleted ? "Task marked as completed" : "Task marked as incomplete", 
                                  Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CalendarActivity.this, 
                                  "Failed to update task status: " + e.getMessage(), 
                                  Toast.LENGTH_SHORT).show();
                });
    }
    
    private int convertDpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
    
    private void populateWeekView(List<CalendarEvent> events) {
        // For simplicity, we'll just show a Toast message for now
        // In a real implementation, you would populate the week view with events
        // organized by day of the week
        Toast.makeText(this, "Week view: Found " + events.size() + " events", Toast.LENGTH_SHORT).show();
    }
    
    private void populateMonthView(List<CalendarEvent> events) {
        // For simplicity, we'll just show a Toast message for now
        // In a real implementation, you would populate the GridView with day cells
        // and mark days that have events
        Toast.makeText(this, "Month view: Found " + events.size() + " events", Toast.LENGTH_SHORT).show();
    }
    
    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                currentCalendar.set(year, month, dayOfMonth);
                updateCurrentDateDisplay();
                loadEventsForCurrentView();
            },
            currentCalendar.get(Calendar.YEAR),
            currentCalendar.get(Calendar.MONTH),
            currentCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    
    private void showAddEventDialog() {
        // Implement add event dialog
        // This would open a dialog to add a new event
        Toast.makeText(this, "Add event functionality coming soon", Toast.LENGTH_SHORT).show();
    }
    
    // CalendarEventAdapter.OnEventClickListener implementation
    @Override
    public void onEventClick(CalendarEvent event, int position) {
        // Handle event click
        // This would open event details
        Toast.makeText(this, "Clicked: " + event.getTitle(), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onEventLongClick(CalendarEvent event, int position) {
        // Handle event long click
        // This could show options like edit, delete, etc.
        Toast.makeText(this, "Long clicked: " + event.getTitle(), Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Convert dp to pixels
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
} 