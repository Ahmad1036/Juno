package com.example.juno.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.juno.R;
import com.example.juno.SettingsActivity;
import com.example.juno.model.Task;

public class NotificationUtils {
    
    private static final String CHANNEL_ID = "juno_notifications";
    private static final String CHANNEL_NAME = "Juno Notifications";
    private static final String CHANNEL_DESCRIPTION = "Task reminders and notifications";
    
    // Create the notification channel for Android 8.0 and above
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
            channel.setDescription(CHANNEL_DESCRIPTION);
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    // Build and display a notification for a task
    public static void showTaskNotification(Context context, Task task, Intent intent) {
        // Check if notifications are enabled
        SharedPreferences prefs = context.getSharedPreferences("JunoUserPrefs", Context.MODE_PRIVATE);
        boolean notificationsEnabled = SettingsActivity.areNotificationsEnabled(prefs);
        
        if (!notificationsEnabled) {
            return;
        }
        
        // Get notification style
        String notificationStyle = SettingsActivity.getNotificationStyle(prefs);
        
        // Create pending intent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Build notification based on style
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        
        switch (notificationStyle.toLowerCase()) {
            case "compact":
                // Compact style has just a title with no description
                builder.setContentTitle(task.getTitle());
                break;
                
            case "expanded":
                // Expanded style has a large image and a longer description
                builder.setContentTitle(task.getTitle())
                       .setContentText(task.getDescription())
                       .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.app_icon))
                       .setStyle(new NotificationCompat.BigTextStyle()
                               .bigText(task.getDescription()));
                break;
                
            case "minimal":
                // Minimal style has a title and priority indicator only
                String priorityText = "Priority: " + task.getPriority();
                builder.setContentTitle(task.getTitle())
                       .setContentText(priorityText);
                break;
                
            case "standard":
            default:
                // Standard style has title and description
                builder.setContentTitle(task.getTitle())
                       .setContentText(task.getDescription());
                break;
        }
        
        // Show notification
        NotificationManager notificationManager = (NotificationManager) 
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.notify(task.getId().hashCode(), builder.build());
        }
    }
    
    // Cancel a specific notification
    public static void cancelNotification(Context context, String taskId) {
        NotificationManager notificationManager = (NotificationManager) 
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.cancel(taskId.hashCode());
        }
    }
    
    // Cancel all notifications
    public static void cancelAllNotifications(Context context) {
        NotificationManager notificationManager = (NotificationManager) 
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
    }
} 