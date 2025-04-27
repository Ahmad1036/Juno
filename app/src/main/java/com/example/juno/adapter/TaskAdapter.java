package com.example.juno.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.juno.R;
import com.example.juno.SettingsActivity;
import com.example.juno.model.Task;
import com.example.juno.utils.ThemeUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    
    private List<Task> taskList;
    private OnTaskClickListener listener;
    private Context context;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private SharedPreferences prefs;
    private float completedTaskAlpha = 0.5f; // Default alpha value for completed tasks

    public interface OnTaskClickListener {
        void onTaskClick(int position);
        void onCheckBoxClick(int position, boolean isChecked);
        void onTaskCheckboxClicked(Task task);
    }

    public TaskAdapter(List<Task> taskList, OnTaskClickListener listener) {
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        prefs = context.getSharedPreferences("JunoUserPrefs", Context.MODE_PRIVATE);
        
        // Get the completed task alpha from settings or use default
        String layoutStyle = SettingsActivity.getLayoutStyle(prefs);
        completedTaskAlpha = layoutStyle.equals("compact") ? 0.7f : 0.5f;
        
        View itemView = LayoutInflater.from(context)
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        
        // Set task title
        holder.taskTitle.setText(task.getTitle());
        
        // Set task description if available
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            holder.taskDescription.setVisibility(View.VISIBLE);
            holder.taskDescription.setText(task.getDescription());
        } else {
            holder.taskDescription.setVisibility(View.GONE);
        }
        
        // Format and set due date
        if (task.getDueDate() != 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            String formattedDate = "Due: " + sdf.format(new Date(task.getDueDate()));
            holder.dueDate.setText(formattedDate);
            holder.dueDate.setVisibility(View.VISIBLE);
        } else {
            holder.dueDate.setVisibility(View.GONE);
        }
        
        // Set priority indicator color
        int priorityColor;
        switch (task.getPriority()) {
            case Task.PRIORITY_HIGH:
                priorityColor = Color.parseColor("#FF5252");  // Red for high priority
                break;
            case Task.PRIORITY_MEDIUM:
                priorityColor = Color.parseColor("#FFB300");  // Orange/Amber for medium priority
                break;
            default:
                priorityColor = Color.parseColor("#66BB6A");  // Green for low priority
                break;
        }
        holder.priorityIndicator.setBackgroundColor(priorityColor);
        
        // Show image indicator if task has an image (check both imageUrl and imageData)
        if ((task.getImageUrl() != null && !task.getImageUrl().isEmpty()) || 
            (task.getImageData() != null && !task.getImageData().isEmpty())) {
            holder.imageIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.imageIndicator.setVisibility(View.GONE);
        }
        
        // Set checkbox state without triggering listener
        holder.checkboxComplete.setOnCheckedChangeListener(null);
        holder.checkboxComplete.setChecked(task.isCompleted());
        
        // Apply styling based on completion status
        if (task.isCompleted()) {
            // Apply strikethrough to title and description
            holder.taskTitle.setPaintFlags(holder.taskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.taskDescription.setPaintFlags(holder.taskDescription.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            
            // Dim text for completed tasks
            holder.taskTitle.setAlpha(0.6f);
            holder.taskDescription.setAlpha(0.6f);
            holder.dueDate.setAlpha(0.6f);
        } else {
            // Remove strikethrough
            holder.taskTitle.setPaintFlags(holder.taskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.taskDescription.setPaintFlags(holder.taskDescription.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            
            // Restore full opacity
            holder.taskTitle.setAlpha(1.0f);
            holder.taskDescription.setAlpha(1.0f);
            holder.dueDate.setAlpha(1.0f);
        }
        
        // Setup checkbox listener
        holder.checkboxComplete.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setCompleted(isChecked);
            notifyItemChanged(position);
            if (listener != null) {
                listener.onTaskCheckboxClicked(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public void updateTasks(List<Task> tasks) {
        this.taskList = tasks;
        notifyDataSetChanged();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskTitle;
        TextView taskDescription;
        TextView dueDate;
        View priorityIndicator;
        View imageIndicator;
        CheckBox checkboxComplete;

        TaskViewHolder(View view) {
            super(view);
            // Initialize views
            taskTitle = view.findViewById(R.id.tv_task_title);
            taskDescription = view.findViewById(R.id.tv_task_description);
            dueDate = view.findViewById(R.id.tv_due_date);
            priorityIndicator = view.findViewById(R.id.view_priority_indicator);
            imageIndicator = view.findViewById(R.id.view_image_indicator);
            checkboxComplete = view.findViewById(R.id.checkbox_complete);

            view.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTaskClick(position);
                }
            });

            // Add checkbox click listener
            checkboxComplete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCheckBoxClick(position, checkboxComplete.isChecked());
                }
            });
        }
    }
} 