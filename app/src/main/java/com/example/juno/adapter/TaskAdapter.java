package com.example.juno.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.juno.R;
import com.example.juno.model.Task;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    
    private List<Task> taskList;
    private OnTaskClickListener listener;
    private Context context;

    public interface OnTaskClickListener {
        void onTaskClick(int position);
        void onCheckBoxClick(int position, boolean isChecked);
    }

    public TaskAdapter(List<Task> taskList, OnTaskClickListener listener) {
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View itemView = LayoutInflater.from(context)
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.titleTextView.setText(task.getTitle());
        holder.descriptionTextView.setText(task.getDescription());
        holder.dueDateTextView.setText(task.getDueDate());
        holder.checkBox.setChecked(task.isCompleted());
        
        // Set appropriate indicator for priority
        switch (task.getPriority().toLowerCase()) {
            case "high":
                holder.priorityIndicator.setBackgroundResource(R.drawable.priority_high_indicator);
                break;
            case "medium":
                holder.priorityIndicator.setBackgroundResource(R.drawable.priority_medium_indicator);
                break;
            case "low":
                holder.priorityIndicator.setBackgroundResource(R.drawable.priority_low_indicator);
                break;
            default:
                holder.priorityIndicator.setBackgroundResource(R.drawable.priority_low_indicator);
                break;
        }
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
        CheckBox checkBox;
        TextView titleTextView;
        TextView descriptionTextView;
        TextView dueDateTextView;
        View priorityIndicator;

        TaskViewHolder(View view) {
            super(view);
            checkBox = view.findViewById(R.id.checkbox_complete);
            titleTextView = view.findViewById(R.id.tv_task_title);
            descriptionTextView = view.findViewById(R.id.tv_task_description);
            dueDateTextView = view.findViewById(R.id.tv_due_date);
            priorityIndicator = view.findViewById(R.id.view_priority_indicator);

            view.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTaskClick(position);
                }
            });

            checkBox.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCheckBoxClick(position, checkBox.isChecked());
                }
            });
        }
    }
} 