package com.example.juno;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.juno.adapter.NotificationAdapter;
import com.example.juno.model.Notification;
import com.example.juno.service.NotificationService;
import com.example.juno.utils.NotificationUtils;

import java.util.List;

public class NotificationsActivity extends AppCompatActivity implements NotificationAdapter.NotificationClickListener {

    private RecyclerView recyclerView;
    private LinearLayout emptyStateLayout;
    private NotificationAdapter adapter;
    private NotificationService notificationService;
    private TextView clearAllButton;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Initialize components
        recyclerView = findViewById(R.id.notifications_recycler_view);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        clearAllButton = findViewById(R.id.clear_all_button);
        backButton = findViewById(R.id.back_button);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Initialize notification service
        notificationService = new NotificationService(this);
        
        // Load notifications
        loadNotifications();
        
        // Setup click listeners
        clearAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAllNotifications();
            }
        });
        
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh notifications in case they've changed
        loadNotifications();
    }
    
    private void loadNotifications() {
        List<Notification> notifications = notificationService.getAllNotifications();
        
        if (notifications.isEmpty()) {
            // Add a dummy notification for empty state
            Notification dummyNotification = new Notification(
                    "Welcome to Juno",
                    "Your notifications will appear here. You'll be notified about tasks, reminders, and important updates."
            );
            dummyNotification.setRead(true); // Mark as read by default
            notifications.add(dummyNotification);
            
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            clearAllButton.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            clearAllButton.setVisibility(View.VISIBLE);
        }
        
        if (adapter == null) {
            adapter = new NotificationAdapter(this, notifications, this);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateData(notifications);
        }
        
        // Mark all as read when viewed (except dummy notification)
        for (Notification notification : notifications) {
            if (!notification.isRead() && notification.getId() != null) {
                notificationService.markAsRead(notification.getId());
            }
        }
    }
    
    private void clearAllNotifications() {
        // Check if we only have the dummy notification
        List<Notification> notifications = notificationService.getAllNotifications();
        if (notifications.isEmpty()) {
            Toast.makeText(this, "No notifications to clear", Toast.LENGTH_SHORT).show();
            return;
        }
        
        notificationService.clearAllNotifications();
        NotificationUtils.cancelAllNotifications(this);
        loadNotifications();
        Toast.makeText(this, "All notifications cleared", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNotificationClick(Notification notification) {
        // Handle dummy notification click differently
        if (notification.getId() == null) {
            Toast.makeText(this, "This is a sample notification to show you how notifications appear", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Handle notification click (e.g., navigate to related content)
        if (notification.getRelatedTaskId() != null) {
            // Navigate to the related task
            // For example:
            // Intent intent = new Intent(this, TaskDetailActivity.class);
            // intent.putExtra("taskId", notification.getRelatedTaskId());
            // startActivity(intent);
            
            // For now, just show a toast
            Toast.makeText(this, "Opening related task: " + notification.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteClick(Notification notification) {
        // Don't delete dummy notification
        if (notification.getId() == null) {
            Toast.makeText(this, "This is a sample notification", Toast.LENGTH_SHORT).show();
            return;
        }
        
        notificationService.deleteNotification(notification.getId());
        loadNotifications();
        Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show();
    }
} 