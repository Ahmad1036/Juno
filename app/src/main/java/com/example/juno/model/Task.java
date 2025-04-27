package com.example.juno.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Task implements Serializable {
    private String id;
    private String title;
    private String description;
    private long dueDate;
    private String time;
    private String priority; // HIGH, MEDIUM, LOW
    private boolean completed;
    private String userId;
    private int syncStatus; // 0=synced, 1=pending create, 2=pending update, 3=pending delete
    private String imageData; // Base64 encoded image string
    private String imageUrl; // Firebase Storage URL for the image
    private String label; // Label for categorizing tasks (e.g., "auto", "work", "personal")
    private String parentId; // ID of the parent task, if this is a subtask
    
    // Priority constants
    public static final String PRIORITY_HIGH = "HIGH";
    public static final String PRIORITY_MEDIUM = "MEDIUM";
    public static final String PRIORITY_LOW = "LOW";
    
    // Required for Firebase
    public Task() {
        this.syncStatus = 0; // Default to synced
    }
    
    // Constructor with all fields
    public Task(String id, String title, String description, long dueDate, String time, 
                String priority, boolean completed, String userId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.time = time;
        this.priority = priority;
        this.completed = completed;
        this.userId = userId;
        this.syncStatus = 0; // Default to synced
    }
    
    // Constructor with label
    public Task(String id, String title, String description, long dueDate, String time, 
                String priority, boolean completed, String userId, String label) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.time = time;
        this.priority = priority;
        this.completed = completed;
        this.userId = userId;
        this.label = label;
        this.syncStatus = 0; // Default to synced
    }
    
    // Convert to Map for Firebase
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("title", title);
        result.put("description", description);
        result.put("dueDate", dueDate);
        result.put("time", time);
        result.put("priority", priority);
        result.put("completed", completed);
        result.put("userId", userId);
        result.put("imageData", imageData);
        result.put("imageUrl", imageUrl);
        result.put("label", label); // Store label in Firebase
        result.put("parentId", parentId); // Store parentId in Firebase
        // We don't store syncStatus in Firebase as it's only for local tracking
        
        return result;
    }
    
    // Getters and Setters
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
    
    public long getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(long dueDate) {
        this.dueDate = dueDate;
    }
    
    public String getTime() {
        return time;
    }
    
    public void setTime(String time) {
        this.time = time;
    }
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public int getSyncStatus() {
        return syncStatus;
    }
    
    public void setSyncStatus(int syncStatus) {
        this.syncStatus = syncStatus;
    }
    
    public String getImageData() {
        return imageData;
    }
    
    public void setImageData(String imageData) {
        this.imageData = imageData;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public String getParentId() {
        return parentId;
    }
    
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
    
    // Helper methods for sync status
    public boolean isSynced() {
        return syncStatus == 0;
    }
    
    public boolean isPendingCreate() {
        return syncStatus == 1;
    }
    
    public boolean isPendingUpdate() {
        return syncStatus == 2;
    }
    
    public boolean isPendingDelete() {
        return syncStatus == 3;
    }
    
    // Helper method to check if label is "auto"
    public boolean isAutoLabel() {
        return "auto".equalsIgnoreCase(label);
    }
    
    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", dueDate=" + dueDate +
                ", time='" + time + '\'' +
                ", priority='" + priority + '\'' +
                ", completed=" + completed +
                ", userId='" + userId + '\'' +
                ", syncStatus=" + syncStatus +
                ", imageData='" + (imageData != null ? "[Base64 data]" : "null") + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", label='" + label + '\'' +
                ", parentId='" + parentId + '\'' +
                '}';
    }
} 