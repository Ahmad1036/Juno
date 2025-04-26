package com.example.juno;

import com.google.firebase.database.Exclude;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Task {
    private String id;
    private String title;
    private String description;
    private boolean completed;
    private long dueDate;
    private long createdAt;
    private String priority; // "high", "medium", "low"
    private String imageUrl; // URL for task image (if any)

    // Required empty constructor for Firebase
    public Task() {
    }

    public Task(String title, String description, long dueDate, String priority) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.completed = false;
        this.createdAt = new Date().getTime();
        this.imageUrl = null;
    }

    @Exclude
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

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getDueDate() {
        return dueDate;
    }

    public void setDueDate(long dueDate) {
        this.dueDate = dueDate;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    /**
     * Get the priority as an integer for use with drawables
     * 1 = high, 2 = medium, 3 = low
     */
    @Exclude
    public int getPriorityInt() {
        if (priority == null) {
            return 2; // Default to medium
        }
        
        switch (priority.toLowerCase()) {
            case "high":
                return 1;
            case "low":
                return 3;
            default: // "medium" or any other value
                return 2;
        }
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("title", title);
        result.put("description", description);
        result.put("completed", completed);
        result.put("dueDate", dueDate);
        result.put("createdAt", createdAt);
        result.put("priority", priority);
        result.put("imageUrl", imageUrl);
        
        return result;
    }
} 