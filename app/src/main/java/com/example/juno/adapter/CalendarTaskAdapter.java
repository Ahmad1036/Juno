package com.example.juno.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.juno.R;
import com.example.juno.model.Task;

import java.util.List;

public class CalendarTaskAdapter extends RecyclerView.Adapter<CalendarTaskAdapter.TaskViewHolder> {

    private Context context;
    private List<Task> taskList;
    private OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public CalendarTaskAdapter(Context context, List<Task> taskList, OnTaskClickListener listener) {
        this.context = context;
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_task, parent, false);
        return new TaskViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        
        holder.taskTitle.setText(task.getTitle());
        holder.taskDescription.setText(task.getDescription());
        holder.taskTime.setText(task.getTime());
        
        // Set priority color based on task priority
        int priorityColor;
        switch (task.getPriority().toUpperCase()) {
            case "HIGH":
                priorityColor = ContextCompat.getColor(context, R.color.high_priority);
                break;
            case "MEDIUM":
                priorityColor = ContextCompat.getColor(context, R.color.medium_priority);
                break;
            case "LOW":
                priorityColor = ContextCompat.getColor(context, R.color.low_priority);
                break;
            default:
                priorityColor = ContextCompat.getColor(context, R.color.primary);
                break;
        }
        
        holder.priorityIndicator.setBackgroundColor(priorityColor);
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    public void updateTasks(List<Task> tasks) {
        this.taskList = tasks;
        notifyDataSetChanged();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        View priorityIndicator;
        TextView taskTitle;
        TextView taskDescription;
        TextView taskTime;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            priorityIndicator = itemView.findViewById(R.id.priority_indicator);
            taskTitle = itemView.findViewById(R.id.task_title);
            taskDescription = itemView.findViewById(R.id.task_description);
            taskTime = itemView.findViewById(R.id.task_time);
        }
    }
} 