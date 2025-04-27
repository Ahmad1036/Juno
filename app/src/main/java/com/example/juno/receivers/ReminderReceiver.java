package com.example.juno.receivers;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.juno.R;
import com.example.juno.TaskDetailActivity;
import com.example.juno.model.Task;
import com.example.juno.utils.ReminderManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * Receiver for task reminders and nudges
 */
public class ReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderReceiver";
    
    // Actions
    public static final String ACTION_DEADLINE_REMINDER = "com.example.juno.action.DEADLINE_REMINDER";
    public static final String ACTION_SMART_NUDGE = "com.example.juno.action.SMART_NUDGE";
    
    // Extras
    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_TASK_TITLE = "task_title";
    public static final String EXTRA_DUE_DATE = "due_date";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (action == null) {
            Log.e(TAG, "Received intent with null action");
            return;
        }
        
        String taskId = intent.getStringExtra(EXTRA_TASK_ID);
        if (taskId == null) {
            Log.e(TAG, "Received intent without task ID");
            return;
        }
        
        // First check if the task still exists and is not completed
        checkTask(context, taskId, intent);
    }
    
    private void checkTask(Context context, String taskId, Intent originalIntent) {
        DatabaseReference taskRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child("tasks")
                .child(taskId);
        
        taskRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Log.d(TAG, "Task no longer exists, skipping notification: " + taskId);
                    return;
                }
                
                Task task = dataSnapshot.getValue(Task.class);
                if (task == null) {
                    Log.e(TAG, "Failed to parse task from database: " + taskId);
                    return;
                }
                
                if (task.isCompleted()) {
                    Log.d(TAG, "Task is already completed, skipping notification: " + taskId);
                    return;
                }
                
                // Task exists and is not completed, show notification
                String action = originalIntent.getAction();
                if (ACTION_DEADLINE_REMINDER.equals(action)) {
                    showDeadlineNotification(context, task);
                } else if (ACTION_SMART_NUDGE.equals(action)) {
                    showSmartNudgeNotification(context, task);
                }
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error checking task: " + taskId, databaseError.toException());
            }
        });
    }
    
    private void showDeadlineNotification(Context context, Task task) {
        String taskId = task.getId();
        String taskTitle = task.getTitle();
        long dueDate = task.getDueDate();
        
        // Format the due date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        String formattedDate = sdf.format(new Date(dueDate));
        
        // Create an intent for when the notification is tapped
        Intent intent = new Intent(context, TaskDetailActivity.class);
        intent.putExtra("taskId", taskId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                taskId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Create the notification
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, ReminderManager.CHANNEL_DEADLINE)
                .setSmallIcon(R.drawable.checklist)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setContentTitle("Task Deadline Approaching")
                .setContentText(taskTitle)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(taskTitle + " is due on " + formattedDate))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(pendingIntent)
                .setSound(defaultSoundUri)
                .setAutoCancel(true);
        
        // Show the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(taskId.hashCode(), notificationBuilder.build());
        
        Log.d(TAG, "Showed deadline notification for task: " + taskTitle);
    }
    
    private void showSmartNudgeNotification(Context context, Task task) {
        String taskId = task.getId();
        String taskTitle = task.getTitle();
        
        // Get a random nudge message
        ReminderManager reminderManager = new ReminderManager(context, "");
        String message = reminderManager.getRandomNudgeMessage();
        
        // Create an intent for when the notification is tapped
        Intent intent = new Intent(context, TaskDetailActivity.class);
        intent.putExtra("taskId", taskId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                taskId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Create the notification (less intrusive than deadline notifications)
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, ReminderManager.CHANNEL_NUDGE)
                .setSmallIcon(R.drawable.checklist)
                .setContentTitle(message)
                .setContentText(taskTitle)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        
        // Show the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(taskId.hashCode() + 1000, notificationBuilder.build());
        
        Log.d(TAG, "Showed smart nudge notification for task: " + taskTitle);
    }
} 