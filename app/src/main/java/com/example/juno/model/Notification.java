package com.example.juno.model;

import java.util.Date;
import java.util.UUID;

public class Notification {
    private String id;
    private String title;
    private String description;
    private Date timestamp;
    private String relatedTaskId; // Optional - if the notification is related to a task
    private boolean isRead;

    // Constructor for a new notification
    public Notification(String title, String description) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.timestamp = new Date();
        this.isRead = false;
    }

    // Constructor for a new notification related to a task
    public Notification(String title, String description, String relatedTaskId) {
        this(title, description);
        this.relatedTaskId = relatedTaskId;
    }

    // Constructor with all fields for Firebase or internal use
    public Notification(String id, String title, String description, Date timestamp, String relatedTaskId, boolean isRead) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
        this.relatedTaskId = relatedTaskId;
        this.isRead = isRead;
    }

    // Empty constructor for Firebase
    public Notification() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = new Date();
        this.isRead = false;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getRelatedTaskId() {
        return relatedTaskId;
    }

    public void setRelatedTaskId(String relatedTaskId) {
        this.relatedTaskId = relatedTaskId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
    
    public long getTimeAgo() {
        return System.currentTimeMillis() - timestamp.getTime();
    }
} 