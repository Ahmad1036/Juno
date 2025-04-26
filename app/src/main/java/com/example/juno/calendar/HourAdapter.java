package com.example.juno.calendar;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.juno.CalendarEvent;
import com.example.juno.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HourAdapter extends RecyclerView.Adapter<HourAdapter.HourViewHolder> {

    private static final int HOURS_IN_DAY = 24;
    private final Context context;
    private final Map<Integer, List<CalendarEvent>> eventsByHour;
    private final List<CalendarEvent> allEvents;
    private final OnEventClickListener eventClickListener;
    private static final SimpleDateFormat hourFormatter = new SimpleDateFormat("h a", Locale.getDefault());
    private static final SimpleDateFormat timeFormatter = new SimpleDateFormat("h:mm a", Locale.getDefault());

    public interface OnEventClickListener {
        void onEventClick(CalendarEvent event);
    }

    public HourAdapter(Context context, List<CalendarEvent> events, OnEventClickListener listener) {
        this.context = context;
        this.eventClickListener = listener;
        this.allEvents = new ArrayList<>(events);
        this.eventsByHour = new HashMap<>();
        
        // Initialize hour slots
        for (int i = 0; i < HOURS_IN_DAY; i++) {
            eventsByHour.put(i, new ArrayList<>());
        }
        
        // Sort events into hour slots
        organizeEventsByHour();
    }

    private void organizeEventsByHour() {
        // Clear existing events
        for (int i = 0; i < HOURS_IN_DAY; i++) {
            eventsByHour.get(i).clear();
        }
        
        // Sort events into appropriate hour slots
        for (CalendarEvent event : allEvents) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(event.getStartTime());
            int startHour = cal.get(Calendar.HOUR_OF_DAY);
            eventsByHour.get(startHour).add(event);
        }
        
        notifyDataSetChanged();
    }
    
    public void updateEvents(List<CalendarEvent> events) {
        allEvents.clear();
        allEvents.addAll(events);
        organizeEventsByHour();
    }

    @NonNull
    @Override
    public HourViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_hour_view, parent, false);
        return new HourViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HourViewHolder holder, int position) {
        // Format the hour label (e.g., "9 AM")
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, position);
        cal.set(Calendar.MINUTE, 0);
        String hourLabel = hourFormatter.format(cal.getTime());
        holder.hourLabel.setText(hourLabel);
        
        // Clear existing events
        holder.eventsContainer.removeAllViews();
        
        // Add events for this hour
        List<CalendarEvent> eventsForHour = eventsByHour.get(position);
        if (eventsForHour != null && !eventsForHour.isEmpty()) {
            for (CalendarEvent event : eventsForHour) {
                View eventView = LayoutInflater.from(context).inflate(
                        R.layout.item_calendar_event, holder.eventsContainer, false);
                
                TextView eventTitle = eventView.findViewById(R.id.event_title);
                TextView eventTime = eventView.findViewById(R.id.event_time);
                
                // Check if the view has a location field, if not, we'll handle it gracefully
                TextView eventLocation = eventView.findViewById(R.id.event_location);
                
                // Set event details
                eventTitle.setText(event.getTitle());
                
                // Format event time (e.g., "9:00 AM - 10:00 AM")
                String timeText = formatEventTime(event);
                eventTime.setText(timeText);
                
                // Set location if available and the view has the field
                if (eventLocation != null && event.getLocation() != null && !event.getLocation().isEmpty()) {
                    eventLocation.setText(event.getLocation());
                    eventLocation.setVisibility(View.VISIBLE);
                } else if (eventLocation != null) {
                    eventLocation.setVisibility(View.GONE);
                }
                
                // Set click listener
                eventView.setOnClickListener(v -> {
                    if (eventClickListener != null) {
                        eventClickListener.onEventClick(event);
                    }
                });
                
                // Add event view to container
                holder.eventsContainer.addView(eventView);
            }
        }
    }

    private String formatEventTime(CalendarEvent event) {
        Date startTime = new Date(event.getStartTime());
        Date endTime = new Date(event.getEndTime());
        return timeFormatter.format(startTime) + " - " + timeFormatter.format(endTime);
    }

    @Override
    public int getItemCount() {
        return HOURS_IN_DAY;
    }

    static class HourViewHolder extends RecyclerView.ViewHolder {
        TextView hourLabel;
        LinearLayout eventsContainer;

        HourViewHolder(@NonNull View itemView) {
            super(itemView);
            hourLabel = itemView.findViewById(R.id.hour_label);
            eventsContainer = itemView.findViewById(R.id.events_container);
        }
    }
} 