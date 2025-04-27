package com.example.juno.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.juno.R;
import com.example.juno.model.Notification;
import com.example.juno.service.NotificationService;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private Context context;
    private List<Notification> notifications;
    private NotificationService notificationService;
    private NotificationClickListener listener;

    public interface NotificationClickListener {
        void onNotificationClick(Notification notification);
        void onDeleteClick(Notification notification);
    }

    public NotificationAdapter(Context context, List<Notification> notifications, NotificationClickListener listener) {
        this.context = context;
        this.notifications = notifications;
        this.listener = listener;
        this.notificationService = new NotificationService(context);
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        
        holder.titleTextView.setText(notification.getTitle());
        holder.descriptionTextView.setText(notification.getDescription());
        
        // Format time ago (e.g., "5m ago", "2h ago", "Yesterday")
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                notification.getTimestamp().getTime(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE);
        
        holder.timeTextView.setText(timeAgo);
        
        // Set alpha for read/unread
        float alpha = notification.isRead() ? 0.7f : 1.0f;
        holder.iconImageView.setAlpha(alpha);
        holder.titleTextView.setAlpha(alpha);
        
        // Set click listeners
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onNotificationClick(notification);
                }
                
                // Mark as read when clicked
                if (!notification.isRead()) {
                    notification.setRead(true);
                    notificationService.markAsRead(notification.getId());
                    notifyItemChanged(holder.getAdapterPosition());
                }
            }
        });
        
        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onDeleteClick(notification);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }
    
    public void updateData(List<Notification> newNotifications) {
        this.notifications = newNotifications;
        notifyDataSetChanged();
    }
    
    public void removeItem(int position) {
        if (position >= 0 && position < notifications.size()) {
            notifications.remove(position);
            notifyItemRemoved(position);
        }
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImageView;
        TextView titleTextView;
        TextView descriptionTextView;
        TextView timeTextView;
        ImageButton deleteButton;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.notification_icon);
            titleTextView = itemView.findViewById(R.id.notification_title);
            descriptionTextView = itemView.findViewById(R.id.notification_description);
            timeTextView = itemView.findViewById(R.id.notification_time);
            deleteButton = itemView.findViewById(R.id.delete_notification_button);
        }
    }
} 