package com.example.juno;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AddEditEventActivity extends AppCompatActivity {

    private static final String TAG = "AddEditEventActivity";
    private static final String[] COLOR_OPTIONS = {
        "#4285F4", // Blue
        "#0F9D58", // Green
        "#DB4437", // Red
        "#F4B400", // Yellow
        "#9E9E9E"  // Gray
    };
    
    private EditText titleEditText;
    private EditText descriptionEditText;
    private TextView startDateTimeText;
    private TextView endDateTimeText;
    private Button colorButton;
    private Button saveButton;
    private Button deleteButton;
    private ProgressBar progressBar;
    
    private Calendar startDateTime;
    private Calendar endDateTime;
    private String selectedColor = COLOR_OPTIONS[0]; // Default blue
    private String currentUserId;
    private String eventId;
    private boolean isEditMode = false;
    
    private CalendarEventManager eventManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_event);
        
        // Initialize toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Initialize UI components
        titleEditText = findViewById(R.id.edit_event_title);
        descriptionEditText = findViewById(R.id.edit_event_description);
        startDateTimeText = findViewById(R.id.text_start_datetime);
        endDateTimeText = findViewById(R.id.text_end_datetime);
        colorButton = findViewById(R.id.button_color);
        saveButton = findViewById(R.id.button_save);
        deleteButton = findViewById(R.id.button_delete);
        progressBar = findViewById(R.id.progress_bar);
        
        // Initialize event manager
        eventManager = new CalendarEventManager();
        
        // Get user ID
        UserSessionManager sessionManager = new UserSessionManager(this);
        currentUserId = sessionManager.getUserId();
        
        // Initialize date/time values
        startDateTime = Calendar.getInstance();
        // Round to the nearest hour
        startDateTime.set(Calendar.MINUTE, 0);
        startDateTime.set(Calendar.SECOND, 0);
        startDateTime.set(Calendar.MILLISECOND, 0);
        startDateTime.add(Calendar.HOUR_OF_DAY, 1);
        
        endDateTime = (Calendar) startDateTime.clone();
        endDateTime.add(Calendar.HOUR_OF_DAY, 1);
        
        // Check if we're editing an existing event
        if (getIntent().hasExtra("EVENT_ID")) {
            eventId = getIntent().getStringExtra("EVENT_ID");
            isEditMode = true;
            getSupportActionBar().setTitle("Edit Event");
            loadEventDetails();
            deleteButton.setVisibility(View.VISIBLE);
        } else {
            // We're creating a new event
            getSupportActionBar().setTitle("Add Event");
            eventId = UUID.randomUUID().toString();
            deleteButton.setVisibility(View.GONE);
            
            // Check if we have a pre-selected date
            if (getIntent().hasExtra("SELECTED_DATE")) {
                long dateMillis = getIntent().getLongExtra("SELECTED_DATE", 0);
                if (dateMillis > 0) {
                    startDateTime.setTimeInMillis(dateMillis);
                    // Keep the hour/minute we set before
                    startDateTime.set(Calendar.MINUTE, 0);
                    startDateTime.set(Calendar.SECOND, 0);
                    startDateTime.set(Calendar.MILLISECOND, 0);
                    startDateTime.add(Calendar.HOUR_OF_DAY, 1);
                    
                    endDateTime = (Calendar) startDateTime.clone();
                    endDateTime.add(Calendar.HOUR_OF_DAY, 1);
                }
            }
        }
        
        // Update datetime displays
        updateDateTimeDisplays();
        
        // Set click listeners
        startDateTimeText.setOnClickListener(v -> showDateTimePicker(true));
        endDateTimeText.setOnClickListener(v -> showDateTimePicker(false));
        colorButton.setOnClickListener(v -> showColorPicker());
        saveButton.setOnClickListener(v -> saveEvent());
        deleteButton.setOnClickListener(v -> deleteEvent());
        
        // Set initial color button color
        updateColorButton();
    }

    private void loadEventDetails() {
        progressBar.setVisibility(View.VISIBLE);
        
        FirebaseDatabase.getInstance().getReference("events")
                .child(eventId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        progressBar.setVisibility(View.GONE);
                        
                        if (dataSnapshot.exists()) {
                            CalendarEvent event = dataSnapshot.getValue(CalendarEvent.class);
                            if (event != null) {
                                titleEditText.setText(event.getTitle());
                                descriptionEditText.setText(event.getDescription());
                                
                                startDateTime = Calendar.getInstance();
                                startDateTime.setTimeInMillis(event.getStartTime());
                                
                                endDateTime = Calendar.getInstance();
                                endDateTime.setTimeInMillis(event.getEndTime());
                                
                                selectedColor = event.getColor();
                                updateColorButton();
                                updateDateTimeDisplays();
                            }
                        } else {
                            Toast.makeText(AddEditEventActivity.this, "Event not found", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Error loading event", databaseError.toException());
                        Toast.makeText(AddEditEventActivity.this, "Error loading event: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void updateDateTimeDisplays() {
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("EEE, MMM d, yyyy h:mm a", Locale.getDefault());
        startDateTimeText.setText(dateTimeFormat.format(startDateTime.getTime()));
        endDateTimeText.setText(dateTimeFormat.format(endDateTime.getTime()));
    }

    private void showDateTimePicker(final boolean isStartTime) {
        final Calendar currentDateTime = isStartTime ? startDateTime : endDateTime;
        
        // Show date picker first
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    currentDateTime.set(Calendar.YEAR, year);
                    currentDateTime.set(Calendar.MONTH, month);
                    currentDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    
                    // After date is selected, show time picker
                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            this,
                            (view1, hourOfDay, minute) -> {
                                currentDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                currentDateTime.set(Calendar.MINUTE, minute);
                                
                                // If start time was changed, ensure end time is after start time
                                if (isStartTime && startDateTime.after(endDateTime)) {
                                    endDateTime = (Calendar) startDateTime.clone();
                                    endDateTime.add(Calendar.HOUR_OF_DAY, 1);
                                }
                                
                                updateDateTimeDisplays();
                            },
                            currentDateTime.get(Calendar.HOUR_OF_DAY),
                            currentDateTime.get(Calendar.MINUTE),
                            false
                    );
                    timePickerDialog.show();
                },
                currentDateTime.get(Calendar.YEAR),
                currentDateTime.get(Calendar.MONTH),
                currentDateTime.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showColorPicker() {
        // In a real app, we'd show a color picker dialog here
        // For simplicity, we'll just cycle through colors
        for (int i = 0; i < COLOR_OPTIONS.length; i++) {
            if (COLOR_OPTIONS[i].equals(selectedColor)) {
                selectedColor = COLOR_OPTIONS[(i + 1) % COLOR_OPTIONS.length];
                break;
            }
        }
        updateColorButton();
    }

    private void updateColorButton() {
        try {
            colorButton.setBackgroundColor(android.graphics.Color.parseColor(selectedColor));
        } catch (IllegalArgumentException e) {
            // Use default color if parsing fails
            colorButton.setBackgroundColor(android.graphics.Color.BLUE);
        }
    }

    private void saveEvent() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        
        if (TextUtils.isEmpty(title)) {
            titleEditText.setError("Title is required");
            titleEditText.requestFocus();
            return;
        }
        
        if (startDateTime.after(endDateTime)) {
            Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show();
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        
        CalendarEvent event = new CalendarEvent(
                eventId,
                currentUserId,
                title,
                description,
                startDateTime.getTimeInMillis(),
                endDateTime.getTimeInMillis(),
                selectedColor,
                null,    // location
                false,   // completed
                null,    // emoji
                false    // allDay
        );
        
        if (isEditMode) {
            eventManager.updateEvent(event, new CalendarEventManager.EventsCallback() {
                @Override
                public void onEventsLoaded(List<CalendarEvent> events) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AddEditEventActivity.this, "Event updated", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AddEditEventActivity.this, "Error updating event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            eventManager.addEvent(event, new CalendarEventManager.EventsCallback() {
                @Override
                public void onEventsLoaded(List<CalendarEvent> events) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AddEditEventActivity.this, "Event created", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AddEditEventActivity.this, "Error creating event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void deleteEvent() {
        if (!isEditMode) return;
        
        progressBar.setVisibility(View.VISIBLE);
        
        eventManager.deleteEvent(eventId, new CalendarEventManager.EventsCallback() {
            @Override
            public void onEventsLoaded(List<CalendarEvent> events) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AddEditEventActivity.this, "Event deleted", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AddEditEventActivity.this, "Error deleting event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 