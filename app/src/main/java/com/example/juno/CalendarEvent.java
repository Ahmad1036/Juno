package com.example.juno;

import java.io.Serializable;
import java.util.Date;

/**
 * Model class for calendar events.
 */
public class CalendarEvent implements Serializable {
    private String id;
    private String userId;
    private String title;
    private String description;
    private Date startTime;
    private Date endTime;
    private String location;
    private boolean isAllDay;
    private long timestamp;

    /**
     * Empty constructor required for Firebase
     */
    public CalendarEvent() {
        // Required empty constructor for Firebase
    }

    /**
     * Constructor with essential fields
     */
    public CalendarEvent(String userId, String title, Date startTime, Date endTime) {
        this.userId = userId;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Full constructor with all fields
     */
    public CalendarEvent(String userId, String title, String description, Date startTime, 
                        Date endTime, String location, boolean isAllDay) {
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.isAllDay = isAllDay;
        this.timestamp = System.currentTimeMillis();
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

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isAllDay() {
        return isAllDay;
    }

    public void setAllDay(boolean allDay) {
        isAllDay = allDay;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return title;
    }
} 