package com.example.juno;

public class ChecklistItem {
    private String id;
    private String text;
    private boolean completed;
    private long createdAt;

    // Required for Firebase
    public ChecklistItem() {
        this.createdAt = System.currentTimeMillis();
    }

    public ChecklistItem(String text) {
        this.text = text;
        this.completed = false;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
} 