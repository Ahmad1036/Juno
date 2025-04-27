package com.example.juno.utils;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.juno.R;
import com.example.juno.model.Task;
import com.example.juno.receivers.ReminderReceiver;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Manages smart reminders and nudges for tasks based on due dates and user habits
 */
public class ReminderManager {
    private static final String TAG = "ReminderManager";
    
    // Notification channel IDs
    public static final String CHANNEL_DEADLINE = "deadline_reminders";
    public static final String CHANNEL_NUDGE = "smart_nudges";
    
    // Shared preferences keys
    private static final String PREFS_NAME = "JunoReminderPrefs";
    private static final String KEY_PRODUCTIVE_HOURS_START = "productive_hours_start";
    private static final String KEY_PRODUCTIVE_HOURS_END = "productive_hours_end";
    private static final String KEY_REMINDER_STYLE = "reminder_style";
    private static final String KEY_REMINDER_ADVANCE_TIME = "reminder_advance_time";
    
    // Default values
    private static final int DEFAULT_PRODUCTIVE_HOURS_START = 9; // 9 AM
    private static final int DEFAULT_PRODUCTIVE_HOURS_END = 18; // 6 PM
    private static final String DEFAULT_REMINDER_STYLE = "gentle"; // gentle, moderate, assertive
    private static final int DEFAULT_REMINDER_ADVANCE_TIME = 24; // hours before deadline
    
    // Random nudge messages
    private static final String[] GENTLE_NUDGES = {
            "Just a friendly reminder about your task",
            "When you have a moment, this task is coming up",
            "No pressure, but this task is on your horizon",
            "Gently reminding you about this upcoming task",
            "This might be a good time to look at your upcoming task"
    };
    
    private static final String[] MODERATE_NUDGES = {
            "Don't forget about this task coming up soon",
            "Your task deadline is approaching",
            "This task needs your attention soon",
            "Reminder: You have this on your to-do list",
            "Time to start thinking about this task"
    };
    
    private static final String[] ASSERTIVE_NUDGES = {
            "Your task deadline is coming up quickly!",
            "This task needs your attention now",
            "Important reminder: Task deadline approaching",
            "Don't delay - this task needs completion soon",
            "Priority alert: This task is due soon"
    };
    
    private Context context;
    private SharedPreferences preferences;
    private String userId;
    private DatabaseReference userTasksRef;
    private AlarmManager alarmManager;
    
    /**
     * Constructor - initialize the ReminderManager
     * @param context Application context
     * @param userId Current user ID
     */
    public ReminderManager(Context context, String userId) {
        this.context = context;
        this.userId = userId;
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.userTasksRef = FirebaseDatabase.getInstance().getReference().child("users").child(userId).child("tasks");
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        // Create notification channels on newer Android versions
        createNotificationChannels();
    }
    
    /**
     * Schedule reminders for all upcoming tasks
     */
    public void scheduleAllReminders() {
        Log.d(TAG, "Scheduling reminders for all upcoming tasks");
        
        userTasksRef.orderByChild("completed").equalTo(false)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                            Task task = taskSnapshot.getValue(Task.class);
                            if (task != null) {
                                task.setId(taskSnapshot.getKey());
                                scheduleReminder(task);
                            }
                        }
                        Log.d(TAG, "Finished scheduling reminders for all tasks");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error fetching tasks for scheduling reminders", databaseError.toException());
                    }
                });
    }
    
    /**
     * Schedule reminders for a specific task
     * @param task Task to schedule reminders for
     */
    public void scheduleReminder(Task task) {
        // Skip if no due date
        if (task.getDueDate() == 0) {
            Log.d(TAG, "Skipping reminder for task with no due date: " + task.getTitle());
            return;
        }
        
        // Skip if task is already completed
        if (task.isCompleted()) {
            Log.d(TAG, "Skipping reminder for completed task: " + task.getTitle());
            return;
        }
        
        // Skip if due date is in the past
        long now = System.currentTimeMillis();
        if (task.getDueDate() < now) {
            Log.d(TAG, "Skipping reminder for task with past due date: " + task.getTitle());
            return;
        }

        // Schedule deadline reminder
        scheduleDeadlineReminder(task);
        
        // Schedule smart nudge
        scheduleSmartNudge(task);
    }
    
    /**
     * Cancel all reminders for a task
     * @param taskId Task ID to cancel reminders for
     */
    public void cancelReminders(String taskId) {
        // Cancel deadline reminder
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ReminderReceiver.ACTION_DEADLINE_REMINDER);
        intent.putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
        
        // Cancel nudge reminder
        intent.setAction(ReminderReceiver.ACTION_SMART_NUDGE);
        pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.hashCode() + 1, // Different request code
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
        
        Log.d(TAG, "Cancelled reminders for task: " + taskId);
    }
    
    /**
     * Schedule a reminder for task deadline
     */
    private void scheduleDeadlineReminder(Task task) {
        long dueDate = task.getDueDate();
        int advanceHours = preferences.getInt(KEY_REMINDER_ADVANCE_TIME, DEFAULT_REMINDER_ADVANCE_TIME);
        
        // Calculate reminder time (default: 24 hours before deadline)
        long reminderTime = dueDate - (advanceHours * 60 * 60 * 1000);
        
        // If reminder time is in the past, set it to now + 15 minutes
        long now = System.currentTimeMillis();
        if (reminderTime < now) {
            reminderTime = now + (15 * 60 * 1000);
        }
        
        // Create intent for the deadline reminder
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ReminderReceiver.ACTION_DEADLINE_REMINDER);
        intent.putExtra(ReminderReceiver.EXTRA_TASK_ID, task.getId());
        intent.putExtra(ReminderReceiver.EXTRA_TASK_TITLE, task.getTitle());
        intent.putExtra(ReminderReceiver.EXTRA_DUE_DATE, dueDate);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                task.getId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Schedule the alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
        }
        
        Log.d(TAG, "Scheduled deadline reminder for task: " + task.getTitle() + 
                ", reminder time: " + new Date(reminderTime).toString());
    }
    
    /**
     * Schedule a smart nudge reminder at an optimal time
     */
    private void scheduleSmartNudge(Task task) {
        long dueDate = task.getDueDate();
        long now = System.currentTimeMillis();
        
        // Calculate a good time for a nudge (between now and the deadline)
        long nudgeTime = calculateOptimalNudgeTime(now, dueDate);
        
        // Create intent for the smart nudge
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ReminderReceiver.ACTION_SMART_NUDGE);
        intent.putExtra(ReminderReceiver.EXTRA_TASK_ID, task.getId());
        intent.putExtra(ReminderReceiver.EXTRA_TASK_TITLE, task.getTitle());
        intent.putExtra(ReminderReceiver.EXTRA_DUE_DATE, dueDate);
        
        // Use a different request code to avoid overriding the deadline reminder
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                task.getId().hashCode() + 1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Schedule the alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nudgeTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nudgeTime, pendingIntent);
        }
        
        Log.d(TAG, "Scheduled smart nudge for task: " + task.getTitle() + 
                ", nudge time: " + new Date(nudgeTime).toString());
    }
    
    /**
     * Calculate the optimal time for a nudge based on user habits
     */
    private long calculateOptimalNudgeTime(long now, long deadline) {
        // Get user's productive hours
        int productiveHoursStart = preferences.getInt(KEY_PRODUCTIVE_HOURS_START, DEFAULT_PRODUCTIVE_HOURS_START);
        int productiveHoursEnd = preferences.getInt(KEY_PRODUCTIVE_HOURS_END, DEFAULT_PRODUCTIVE_HOURS_END);
        
        // If deadline is more than 3 days away, schedule nudge for 2-3 days before deadline
        // If deadline is less than 3 days away, schedule nudge for halfway between now and deadline
        long threeDay = 3 * 24 * 60 * 60 * 1000L;
        long timeUntilDue = deadline - now;
        
        Calendar calendar = Calendar.getInstance();
        
        if (timeUntilDue > threeDay) {
            // Schedule 2-3 days before deadline
            long randomAdvance = 2 * 24 * 60 * 60 * 1000L + new Random().nextInt(24 * 60 * 60 * 1000);
            calendar.setTimeInMillis(deadline - randomAdvance);
        } else {
            // Schedule halfway between now and deadline
            calendar.setTimeInMillis(now + (timeUntilDue / 2));
        }
        
        // Adjust to be during productive hours
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        
        if (hour < productiveHoursStart) {
            // Too early, adjust to start of productive hours
            calendar.set(Calendar.HOUR_OF_DAY, productiveHoursStart);
            calendar.set(Calendar.MINUTE, 0);
        } else if (hour >= productiveHoursEnd) {
            // Too late, adjust to next day's start of productive hours
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            calendar.set(Calendar.HOUR_OF_DAY, productiveHoursStart);
            calendar.set(Calendar.MINUTE, 0);
        }
        
        // If adjusted time is after the deadline, set to 2 hours before deadline
        if (calendar.getTimeInMillis() >= deadline) {
            calendar.setTimeInMillis(deadline - (2 * 60 * 60 * 1000));
        }
        
        // If adjusted time is in the past, set to now + 30 minutes
        if (calendar.getTimeInMillis() <= now) {
            calendar.setTimeInMillis(now + (30 * 60 * 1000));
        }
        
        return calendar.getTimeInMillis();
    }
    
    /**
     * Get a random nudge message based on user's preferred reminder style
     */
    public String getRandomNudgeMessage() {
        String style = preferences.getString(KEY_REMINDER_STYLE, DEFAULT_REMINDER_STYLE);
        Random random = new Random();
        
        switch (style) {
            case "gentle":
                return GENTLE_NUDGES[random.nextInt(GENTLE_NUDGES.length)];
            case "assertive":
                return ASSERTIVE_NUDGES[random.nextInt(ASSERTIVE_NUDGES.length)];
            case "moderate":
            default:
                return MODERATE_NUDGES[random.nextInt(MODERATE_NUDGES.length)];
        }
    }
    
    /**
     * Create notification channels for Android O+
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            // Create deadline reminder channel
            NotificationChannel deadlineChannel = new NotificationChannel(
                    CHANNEL_DEADLINE,
                    "Deadline Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            deadlineChannel.setDescription("Reminders for upcoming task deadlines");
            
            // Create nudge channel
            NotificationChannel nudgeChannel = new NotificationChannel(
                    CHANNEL_NUDGE,
                    "Smart Nudges",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            nudgeChannel.setDescription("Gentle nudges for upcoming tasks");
            
            // Register the channels
            notificationManager.createNotificationChannel(deadlineChannel);
            notificationManager.createNotificationChannel(nudgeChannel);
        }
    }
    
    /**
     * Update user preferences for reminders
     */
    public void updatePreferences(int productiveHoursStart, int productiveHoursEnd, 
                                 String reminderStyle, int reminderAdvanceTime) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(KEY_PRODUCTIVE_HOURS_START, productiveHoursStart);
        editor.putInt(KEY_PRODUCTIVE_HOURS_END, productiveHoursEnd);
        editor.putString(KEY_REMINDER_STYLE, reminderStyle);
        editor.putInt(KEY_REMINDER_ADVANCE_TIME, reminderAdvanceTime);
        editor.apply();
        
        // Reschedule reminders with new preferences
        scheduleAllReminders();
    }
} 