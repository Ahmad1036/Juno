package com.example.juno;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarEventAdapter extends RecyclerView.Adapter<CalendarEventAdapter.EventViewHolder> {
    
    private List<CalendarEvent> eventList;
    private final Context context;
    private final OnEventClickListener clickListener;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dateFormat;
    
    // Interface for click events
    public interface OnEventClickListener {
        void onEventClick(CalendarEvent event, int position);
        void onEventLongClick(CalendarEvent event, int position);
    }
    
    public CalendarEventAdapter(Context context, OnEventClickListener listener) {
        this.context = context;
        this.clickListener = listener;
        this.eventList = new ArrayList<>();
        this.timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        this.dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());
    }
    
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_event, parent, false);
        return new EventViewHolder(itemView);
    }
    
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        CalendarEvent event = eventList.get(position);
        
        holder.titleTextView.setText(event.getTitle());
        
        // Set event time or "All day"
        if (event.isAllDay()) {
            holder.timeTextView.setText(R.string.all_day);
        } else {
            String timeText = timeFormat.format(new Date(event.getStartTime()));
            if (event.getEndTime() > 0) {
                timeText += " - " + timeFormat.format(new Date(event.getEndTime()));
            }
            holder.timeTextView.setText(timeText);
        }
        
        // Set event date if the view has this field
        if (holder.dateTextView != null) {
            holder.dateTextView.setText(dateFormat.format(new Date(event.getStartTime())));
        }
        
        // Set location if available and the view has this field
        if (holder.locationTextView != null) {
            if (event.getLocation() != null && !event.getLocation().isEmpty()) {
                holder.locationTextView.setText(event.getLocation());
                holder.locationTextView.setVisibility(View.VISIBLE);
            } else {
                holder.locationTextView.setVisibility(View.GONE);
            }
        }
        
        // Set emoji if available and the view has this field
        if (holder.emojiTextView != null) {
            if (event.getEmoji() != null && !event.getEmoji().isEmpty()) {
                holder.emojiTextView.setText(event.getEmoji());
                holder.emojiTextView.setVisibility(View.VISIBLE);
            } else {
                holder.emojiTextView.setVisibility(View.GONE);
            }
        }
        
        // Set completion status if the view has this field
        if (holder.statusImageView != null) {
            if (event.isCompleted()) {
                holder.statusImageView.setImageResource(R.drawable.ic_completed);
            } else {
                holder.statusImageView.setImageResource(R.drawable.ic_incomplete);
            }
        }
        
        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onEventClick(event, holder.getAdapterPosition());
            }
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            if (clickListener != null) {
                clickListener.onEventLongClick(event, holder.getAdapterPosition());
                return true;
            }
            return false;
        });
    }
    
    @Override
    public int getItemCount() {
        return eventList.size();
    }
    
    public void setEvents(List<CalendarEvent> events) {
        this.eventList = events;
        notifyDataSetChanged();
    }
    
    // Filter events for a specific date (timestamp in milliseconds)
    public void filterEventsByDate(long dateTimestamp) {
        // Implementation will be added later
    }
    
    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView timeTextView;
        TextView dateTextView;
        TextView locationTextView;
        TextView emojiTextView;
        ImageView statusImageView;
        
        EventViewHolder(View view) {
            super(view);
            titleTextView = view.findViewById(R.id.event_title);
            timeTextView = view.findViewById(R.id.event_time);
            
            // Try to find optional views, might not exist in all layouts
            try {
                dateTextView = view.findViewById(R.id.event_date);
                locationTextView = view.findViewById(R.id.event_location);
                emojiTextView = view.findViewById(R.id.event_emoji);
                statusImageView = view.findViewById(R.id.event_status);
            } catch (Exception e) {
                // These fields might not exist in all layouts
            }
        }
    }
} 