package com.example.juno;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

public class DayCalendarAdapter extends RecyclerView.Adapter<DayCalendarAdapter.HourViewHolder> {

    private static final int HOURS_IN_DAY = 24;
    private final Context context;
    private final Calendar selectedDate;
    private final TreeMap<Integer, List<CalendarEvent>> eventsByHour;
    private final EventClickListener eventClickListener;

    public interface EventClickListener {
        void onEventClick(CalendarEvent event);
    }

    public DayCalendarAdapter(Context context, Calendar selectedDate, List<CalendarEvent> events, EventClickListener eventClickListener) {
        this.context = context;
        this.selectedDate = (Calendar) selectedDate.clone();
        this.eventClickListener = eventClickListener;
        this.eventsByHour = new TreeMap<>();
        
        // Initialize empty lists for all hours
        for (int i = 0; i < HOURS_IN_DAY; i++) {
            eventsByHour.put(i, new ArrayList<>());
        }
        
        // Sort events into hours
        if (events != null) {
            for (CalendarEvent event : events) {
                addEventToHourMap(event);
            }
        }
    }

    private void addEventToHourMap(CalendarEvent event) {
        Calendar eventStart = Calendar.getInstance();
        eventStart.setTimeInMillis(event.getStartTime());
        
        int hourOfDay = eventStart.get(Calendar.HOUR_OF_DAY);
        
        // Make sure we're only showing events for the selected date
        Calendar eventDate = (Calendar) eventStart.clone();
        eventDate.set(Calendar.HOUR_OF_DAY, 0);
        eventDate.set(Calendar.MINUTE, 0);
        eventDate.set(Calendar.SECOND, 0);
        eventDate.set(Calendar.MILLISECOND, 0);
        
        Calendar selectedDateStart = (Calendar) selectedDate.clone();
        selectedDateStart.set(Calendar.HOUR_OF_DAY, 0);
        selectedDateStart.set(Calendar.MINUTE, 0);
        selectedDateStart.set(Calendar.SECOND, 0);
        selectedDateStart.set(Calendar.MILLISECOND, 0);
        
        if (eventDate.getTimeInMillis() == selectedDateStart.getTimeInMillis()) {
            List<CalendarEvent> hourEvents = eventsByHour.get(hourOfDay);
            if (hourEvents != null) {
                hourEvents.add(event);
            }
        }
    }

    public void updateEvents(List<CalendarEvent> events) {
        // Clear existing events
        for (int i = 0; i < HOURS_IN_DAY; i++) {
            eventsByHour.get(i).clear();
        }
        
        // Add new events
        if (events != null) {
            for (CalendarEvent event : events) {
                addEventToHourMap(event);
            }
        }
        
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HourViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_hour_view, parent, false);
        return new HourViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HourViewHolder holder, int position) {
        // Format hour display (e.g., "8 AM", "12 PM", etc.)
        String hourText;
        if (position == 0) {
            hourText = "12 AM";
        } else if (position < 12) {
            hourText = position + " AM";
        } else if (position == 12) {
            hourText = "12 PM";
        } else {
            hourText = (position - 12) + " PM";
        }
        
        holder.hourLabel.setText(hourText);
        
        // Clear any existing event views
        holder.eventsContainer.removeAllViews();
        
        // Add event views for this hour
        List<CalendarEvent> eventsForHour = eventsByHour.get(position);
        if (eventsForHour != null && !eventsForHour.isEmpty()) {
            for (CalendarEvent event : eventsForHour) {
                View eventView = createEventView(event);
                holder.eventsContainer.addView(eventView);
            }
        }
    }

    private View createEventView(CalendarEvent event) {
        View eventView = LayoutInflater.from(context).inflate(R.layout.item_calendar_event, null);
        
        TextView timeTextView = eventView.findViewById(R.id.event_time);
        TextView titleTextView = eventView.findViewById(R.id.event_title);
        
        // Find description if it exists in the layout (handle gracefully if missing)
        TextView descriptionTextView = null;
        try {
            descriptionTextView = eventView.findViewById(R.id.event_description);
        } catch (Exception e) {
            // View might not exist in the layout
        }
        
        CardView cardView = (CardView) eventView;
        
        // Format event time
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String startTime = timeFormat.format(new Date(event.getStartTime()));
        String endTime = timeFormat.format(new Date(event.getEndTime()));
        timeTextView.setText(String.format("%s - %s", startTime, endTime));
        
        titleTextView.setText(event.getTitle());
        
        // Set description if the view exists and event has description
        if (descriptionTextView != null && event.getDescription() != null) {
            descriptionTextView.setText(event.getDescription());
        }
        
        // Set card color if available
        if (event.getColor() != null && !event.getColor().isEmpty()) {
            try {
                cardView.setCardBackgroundColor(android.graphics.Color.parseColor(event.getColor()));
                // Ensure text is visible on colored background
                titleTextView.setTextColor(android.graphics.Color.WHITE);
                timeTextView.setTextColor(android.graphics.Color.WHITE);
                if (descriptionTextView != null) {
                    descriptionTextView.setTextColor(android.graphics.Color.WHITE);
                }
            } catch (IllegalArgumentException e) {
                // Use default color if parsing fails
            }
        }
        
        // Set click listener
        eventView.setOnClickListener(v -> {
            if (eventClickListener != null) {
                eventClickListener.onEventClick(event);
            }
        });
        
        return eventView;
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