package com.example.juno;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Journal {
    private String id;
    private String content;
    private String gratitudeContent;
    private String imageData; // Base64 encoded image
    private long timestamp;
    private long lastUpdated;
    private String userId;

    // Required empty constructor for Firebase
    public Journal() {
    }

    public Journal(String content, String gratitudeContent, String imageData, long timestamp, String userId) {
        this.content = content;
        this.gratitudeContent = gratitudeContent;
        this.imageData = imageData;
        this.timestamp = timestamp;
        this.lastUpdated = timestamp; // Initially same as creation time
        this.userId = userId;
    }

    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getGratitudeContent() {
        return gratitudeContent;
    }

    public void setGratitudeContent(String gratitudeContent) {
        this.gratitudeContent = gratitudeContent;
    }

    public String getImageData() {
        return imageData;
    }

    public void setImageData(String imageData) {
        this.imageData = imageData;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Exclude
    public String getLastUpdatedFormatted() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());
        return sdf.format(new Date(lastUpdated));
    }

    @Exclude
    public boolean hasGratitude() {
        return gratitudeContent != null && !gratitudeContent.trim().isEmpty();
    }

    @Exclude
    public boolean hasImage() {
        return imageData != null && !imageData.isEmpty();
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("gratitudeContent", gratitudeContent);
        result.put("imageData", imageData);
        result.put("timestamp", timestamp);
        result.put("lastUpdated", lastUpdated);
        result.put("userId", userId);
        
        return result;
    }
    
    @Override
    public String toString() {
        return "Journal{" +
                "id='" + id + '\'' +
                ", content='" + (content != null ? content.substring(0, Math.min(content.length(), 20)) + "..." : "null") + '\'' +
                ", hasGratitude=" + hasGratitude() +
                ", hasImage=" + hasImage() +
                ", timestamp=" + timestamp +
                ", lastUpdated=" + lastUpdated +
                ", userId='" + userId + '\'' +
                '}';
    }
} 