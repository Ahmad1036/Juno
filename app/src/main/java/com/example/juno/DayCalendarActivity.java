package com.example.juno;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DayCalendarActivity extends AppCompatActivity implements DayCalendarAdapter.EventClickListener {

    private static final String TAG = "DayCalendarActivity";
    
    private RecyclerView recyclerView;
    private DayCalendarAdapter adapter;
    private TextView dateDisplay;
    private Calendar selectedDate;
    private String currentUserId;
    private CalendarEventManager eventManager;
    private List<CalendarEvent> eventsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day_calendar);
        
        // Initialize toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Day View");
        
        // Initialize Firebase event manager
        eventManager = new CalendarEventManager();
        
        // Get current user ID
        UserSessionManager sessionManager = new UserSessionManager(this);
        currentUserId = sessionManager.getUserId();
        
        // Initialize UI elements
        dateDisplay = findViewById(R.id.date_display);
        recyclerView = findViewById(R.id.day_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Initialize selected date
        selectedDate = Calendar.getInstance();
        
        // Get date from intent if available
        if (getIntent().hasExtra("SELECTED_DATE")) {
            long dateMillis = getIntent().getLongExtra("SELECTED_DATE", 0);
            if (dateMillis > 0) {
                selectedDate.setTimeInMillis(dateMillis);
            }
        }
        
        // Setup date display
        updateDateDisplay();
        
        // Initialize adapter
        adapter = new DayCalendarAdapter(this, selectedDate, eventsList, this);
        recyclerView.setAdapter(adapter);
        
        // Setup click listeners
        findViewById(R.id.previous_day).setOnClickListener(v -> {
            selectedDate.add(Calendar.DAY_OF_MONTH, -1);
            updateDateDisplay();
            loadEventsForSelectedDate();
        });
        
        findViewById(R.id.next_day).setOnClickListener(v -> {
            selectedDate.add(Calendar.DAY_OF_MONTH, 1);
            updateDateDisplay();
            loadEventsForSelectedDate();
        });
        
        dateDisplay.setOnClickListener(v -> showDatePickerDialog());
        
        FloatingActionButton fabAddEvent = findViewById(R.id.fab_add_event);
        fabAddEvent.setOnClickListener(v -> openAddEventActivity());
        
        // Load events for the selected date
        loadEventsForSelectedDate();
    }

    private void updateDateDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
        dateDisplay.setText(dateFormat.format(selectedDate.getTime()));
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateDisplay();
                    loadEventsForSelectedDate();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void loadEventsForSelectedDate() {
        if (currentUserId == null) {
            Log.e(TAG, "User ID is null, cannot load events");
            return;
        }
        
        // Show loading indicator
        findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
        
        // Clone selected date and set time to start of day
        Calendar startOfDay = (Calendar) selectedDate.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);
        
        // Clone selected date and set time to end of day
        Calendar endOfDay = (Calendar) selectedDate.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);
        
        // Get events for the selected date
        eventManager.getEventsForDate(currentUserId, startOfDay.getTimeInMillis(), endOfDay.getTimeInMillis(), new CalendarEventManager.EventsCallback() {
            @Override
            public void onEventsLoaded(List<CalendarEvent> events) {
                eventsList = events;
                runOnUiThread(() -> {
                    adapter.updateEvents(events);
                    findViewById(R.id.progress_bar).setVisibility(View.GONE);
                    findViewById(R.id.empty_view).setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading events", e);
                runOnUiThread(() -> {
                    Toast.makeText(DayCalendarActivity.this, "Error loading events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    findViewById(R.id.progress_bar).setVisibility(View.GONE);
                    findViewById(R.id.empty_view).setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void openAddEventActivity() {
        Intent intent = new Intent(this, AddEditEventActivity.class);
        intent.putExtra("SELECTED_DATE", selectedDate.getTimeInMillis());
        startActivity(intent);
    }

    @Override
    public void onEventClick(CalendarEvent event) {
        Intent intent = new Intent(this, AddEditEventActivity.class);
        intent.putExtra("EVENT_ID", event.getId());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEventsForSelectedDate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_calendar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_month_view) {
            Intent intent = new Intent(this, CalendarActivity.class);
            intent.putExtra("SELECTED_DATE", selectedDate.getTimeInMillis());
            startActivity(intent);
            finish();
            return true;
        } else if (id == R.id.action_today) {
            selectedDate = Calendar.getInstance();
            updateDateDisplay();
            loadEventsForSelectedDate();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
} 