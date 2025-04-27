package com.example.juno.service;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.juno.model.Notification;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotificationService {
    private static final String PREFS_NAME = "JunoNotifications";
    private static final String NOTIFICATIONS_KEY = "notifications";

    private Context context;
    private Gson gson;

    public NotificationService(Context context) {
        this.context = context;
        this.gson = new Gson();
    }

    // Save a new notification
    public void saveNotification(Notification notification) {
        List<Notification> notifications = getAllNotifications();
        notifications.add(notification);
        saveAllNotifications(notifications);
    }

    // Get all notifications
    public List<Notification> getAllNotifications() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(NOTIFICATIONS_KEY, null);
        
        if (json == null) {
            return new ArrayList<>();
        }
        
        Type type = new TypeToken<ArrayList<Notification>>() {}.getType();
        List<Notification> notifications = gson.fromJson(json, type);
        
        // Sort by time (newest first)
        Collections.sort(notifications, new Comparator<Notification>() {
            @Override
            public int compare(Notification n1, Notification n2) {
                return n2.getTimestamp().compareTo(n1.getTimestamp());
            }
        });
        
        return notifications;
    }

    // Get unread notifications
    public List<Notification> getUnreadNotifications() {
        List<Notification> notifications = getAllNotifications();
        List<Notification> unread = new ArrayList<>();
        
        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                unread.add(notification);
            }
        }
        
        return unread;
    }

    // Mark notification as read
    public void markAsRead(String notificationId) {
        List<Notification> notifications = getAllNotifications();
        boolean updated = false;
        
        for (Notification notification : notifications) {
            if (notification.getId().equals(notificationId)) {
                notification.setRead(true);
                updated = true;
                break;
            }
        }
        
        if (updated) {
            saveAllNotifications(notifications);
        }
    }

    // Delete a notification
    public void deleteNotification(String notificationId) {
        List<Notification> notifications = getAllNotifications();
        List<Notification> updated = new ArrayList<>();
        
        for (Notification notification : notifications) {
            if (!notification.getId().equals(notificationId)) {
                updated.add(notification);
            }
        }
        
        saveAllNotifications(updated);
    }

    // Clear all notifications
    public void clearAllNotifications() {
        saveAllNotifications(new ArrayList<>());
    }

    // Private method to save notifications list
    private void saveAllNotifications(List<Notification> notifications) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String json = gson.toJson(notifications);
        editor.putString(NOTIFICATIONS_KEY, json);
        editor.apply();
    }
    
    // Get notification count
    public int getNotificationCount() {
        return getAllNotifications().size();
    }
    
    // Get unread notification count
    public int getUnreadNotificationCount() {
        return getUnreadNotifications().size();
    }
} 