package com.example.juno;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Class for managing calendar events in Firebase.
 */
public class CalendarEventManager {
    private static final String TAG = "CalendarEventManager";
    private static final String EVENTS_REF = "calendar_events";
    
    private final DatabaseReference eventsRef;
    
    public interface EventsCallback {
        void onEventsLoaded(List<CalendarEvent> events);
        void onError(Exception e);
    }

    public CalendarEventManager() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        eventsRef = database.getReference(EVENTS_REF);
    }
    
    /**
     * Add a new event to Firebase
     */
    public void addEvent(CalendarEvent event, EventsCallback callback) {
        String eventId = eventsRef.push().getKey();
        if (eventId != null) {
            event.setId(eventId);
            eventsRef.child(eventId).setValue(event)
                    .addOnSuccessListener(aVoid -> {
                        List<CalendarEvent> eventList = new ArrayList<>();
                        eventList.add(event);
                        callback.onEventsLoaded(eventList);
                    })
                    .addOnFailureListener(callback::onError);
        } else {
            Log.e(TAG, "Failed to generate event key");
            callback.onError(new Exception("Failed to generate event key"));
        }
    }
    
    /**
     * Update an existing event
     */
    public void updateEvent(CalendarEvent event, EventsCallback callback) {
        if (event.getId() != null) {
            eventsRef.child(event.getId()).setValue(event)
                    .addOnSuccessListener(aVoid -> {
                        List<CalendarEvent> eventList = new ArrayList<>();
                        eventList.add(event);
                        callback.onEventsLoaded(eventList);
                    })
                    .addOnFailureListener(callback::onError);
        } else {
            Log.e(TAG, "Cannot update event without id");
            callback.onError(new Exception("Cannot update event without id"));
        }
    }
    
    /**
     * Delete an event
     */
    public void deleteEvent(String eventId, EventsCallback callback) {
        eventsRef.child(eventId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    callback.onEventsLoaded(new ArrayList<>());
                })
                .addOnFailureListener(callback::onError);
    }
    
    /**
     * Get all events for a user
     */
    public void getUserEvents(String userId, EventsCallback callback) {
        Query query = eventsRef.orderByChild("userId").equalTo(userId);
        
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<CalendarEvent> events = new ArrayList<>();
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    CalendarEvent event = eventSnapshot.getValue(CalendarEvent.class);
                    if (event != null) {
                        events.add(event);
                    }
                }
                callback.onEventsLoaded(events);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading events: " + error.getMessage());
                callback.onError(error.toException());
            }
        });
    }
    
    /**
     * Get events for a specific date
     */
    public void getEventsForDate(String userId, long startTimestamp, long endTimestamp, EventsCallback callback) {
        // First get all user events
        getUserEvents(userId, new EventsCallback() {
            @Override
            public void onEventsLoaded(List<CalendarEvent> allEvents) {
                // Filter events for the specific date
                List<CalendarEvent> eventsForDate = new ArrayList<>();
                for (CalendarEvent event : allEvents) {
                    long eventStart = event.getStartTime();
                    if ((eventStart >= startTimestamp && eventStart <= endTimestamp)) {
                        eventsForDate.add(event);
                    }
                }
                callback.onEventsLoaded(eventsForDate);
            }

            @Override
            public void onError(Exception error) {
                callback.onError(error);
            }
        });
    }
    
    /**
     * Get events for the current month
     */
    public void getEventsForMonth(String userId, int year, int month, EventsCallback callback) {
        // Get the start and end of the month
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, 1, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfMonth = calendar.getTimeInMillis();
        
        calendar.add(Calendar.MONTH, 1);
        long startOfNextMonth = calendar.getTimeInMillis();
        
        // First get all user events
        getUserEvents(userId, new EventsCallback() {
            @Override
            public void onEventsLoaded(List<CalendarEvent> allEvents) {
                // Filter events for the specific month
                List<CalendarEvent> eventsForMonth = new ArrayList<>();
                for (CalendarEvent event : allEvents) {
                    long eventStart = event.getStartTime();
                    if (eventStart >= startOfMonth && eventStart < startOfNextMonth) {
                        eventsForMonth.add(event);
                    }
                }
                callback.onEventsLoaded(eventsForMonth);
            }

            @Override
            public void onError(Exception error) {
                callback.onError(error);
            }
        });
    }
} 