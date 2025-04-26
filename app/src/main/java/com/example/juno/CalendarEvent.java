package com.example.juno;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.google.firebase.database.Exclude;

/**
 * Model class representing a calendar event.
 */
public class CalendarEvent {
    private String id;
    private String userId;
    private String title;
    private String description;
    private long startTime; // Timestamp in milliseconds
    private long endTime; // Timestamp in milliseconds
    private String color;
    private String location;
    private boolean completed;
    private String emoji;
    private boolean allDay;

    /**
     * Default constructor for Firebase
     */
    public CalendarEvent() {
        // Required empty constructor for Firebase
    }

    /**
     * Full constructor for creating a new event.
     */
    public CalendarEvent(String id, String userId, String title, String description, 
                         long startTime, long endTime, String color, String location,
                         boolean completed, String emoji, boolean allDay) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.color = color;
        this.location = location;
        this.completed = completed;
        this.emoji = emoji;
        this.allDay = allDay;
    }

    /**
     * Converts this CalendarEvent to a Map for Firebase storage.
     * @return A Map representation of this event
     */
    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("userId", userId);
        result.put("title", title);
        result.put("description", description);
        result.put("startTime", startTime);
        result.put("endTime", endTime);
        result.put("color", color);
        result.put("location", location);
        result.put("completed", completed);
        result.put("emoji", emoji);
        result.put("allDay", allDay);
        return result;
    }

    /**
     * Creates a CalendarEvent from a Firebase data map.
     * @param map The Firebase data map
     * @return A new CalendarEvent
     */
    public static CalendarEvent fromMap(Map<String, Object> map) {
        String id = (String) map.get("id");
        String userId = (String) map.get("userId");
        String title = (String) map.get("title");
        String description = (String) map.get("description");
        
        long startTime = (long) map.get("startTime");
        long endTime = (long) map.get("endTime");
        
        String color = (String) map.get("color");
        String location = (String) map.get("location");
        
        boolean completed = false;
        if (map.get("completed") != null) {
            completed = (boolean) map.get("completed");
        }
        
        String emoji = (String) map.get("emoji");
        
        boolean allDay = false;
        if (map.get("allDay") != null) {
            allDay = (boolean) map.get("allDay");
        }
        
        return new CalendarEvent(id, userId, title, description, startTime, endTime, 
                                color, location, completed, emoji, allDay);
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    // Helper method to get Date for display
    @Exclude
    public Date getStartTimeAsDate() {
        return new Date(startTime);
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    // Helper method to get Date for display
    @Exclude
    public Date getEndTimeAsDate() {
        return new Date(endTime);
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
    
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public boolean isAllDay() {
        return allDay;
    }

    public void setAllDay(boolean allDay) {
        this.allDay = allDay;
    }
    
    // Helper method to get formatted time for display
    @Exclude
    public String getFormattedStartTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return formatter.format(new Date(startTime));
    }
    
    // Helper method to get formatted time for display
    @Exclude
    public String getFormattedEndTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return formatter.format(new Date(endTime));
    }
    
    // Helper method to get formatted date for display
    @Exclude
    public String getFormattedDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        return formatter.format(new Date(startTime));
    }

    @NonNull
    @Override
    public String toString() {
        return "CalendarEvent{" +
                "title='" + title + '\'' +
                ", startTime=" + startTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CalendarEvent event = (CalendarEvent) o;
        return Objects.equals(id, event.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
} 