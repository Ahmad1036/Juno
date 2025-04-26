package com.example.juno;

import java.util.ArrayList;
import java.util.List;

public class Checklist {
    private String id;
    private String title;
    private List<ChecklistItem> items;
    private long createdAt;

    // Required for Firebase
    public Checklist() {
        items = new ArrayList<>();
        createdAt = System.currentTimeMillis();
    }

    public Checklist(String title) {
        this.title = title;
        this.items = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

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

    public List<ChecklistItem> getItems() {
        return items;
    }

    public void setItems(List<ChecklistItem> items) {
        this.items = items;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public int getCompletedItemCount() {
        int count = 0;
        for (ChecklistItem item : items) {
            if (item.isCompleted()) {
                count++;
            }
        }
        return count;
    }

    public int getTotalItemCount() {
        return items.size();
    }
} 