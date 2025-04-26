package com.example.juno;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private Context context;
    private OnTaskClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public interface OnTaskClickListener {
        void onTaskClick(Task task, int position);
        void onCheckboxClicked(Task task, int position, boolean isChecked);
    }

    public TaskAdapter(Context context, List<Task> taskList, OnTaskClickListener listener) {
        this.context = context;
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        
        holder.titleTextView.setText(task.getTitle());
        holder.descriptionTextView.setText(task.getDescription());
        
        // Format and show due date if available
        if (task.getDueDate() > 0) {
            holder.dueDateTextView.setText(dateFormat.format(new Date(task.getDueDate())));
            holder.dueDateTextView.setVisibility(View.VISIBLE);
        } else {
            holder.dueDateTextView.setVisibility(View.GONE);
        }
        
        // Set priority indicator
        int priorityColor;
        switch (task.getPriority()) {
            case 1: // High
                priorityColor = R.color.priority_high;
                break;
            case 2: // Medium
                priorityColor = R.color.priority_medium;
                break;
            case 3: // Low
                priorityColor = R.color.priority_low;
                break;
            default:
                priorityColor = R.color.priority_low;
                break;
        }
        holder.priorityIndicator.setBackgroundResource(R.drawable.priority_low_indicator);
        
        // Set completion status
        holder.completedCheckBox.setChecked(task.isCompleted());
        
        // Apply strikethrough for completed tasks
        if (task.isCompleted()) {
            holder.titleTextView.setPaintFlags(holder.titleTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.titleTextView.setPaintFlags(holder.titleTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public void updateData(List<Task> newTasks) {
        this.taskList = newTasks;
        notifyDataSetChanged();
    }

    public class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView descriptionTextView;
        TextView dueDateTextView;
        View priorityIndicator;
        CheckBox completedCheckBox;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.tv_task_title);
            descriptionTextView = itemView.findViewById(R.id.tv_task_description);
            dueDateTextView = itemView.findViewById(R.id.tv_due_date);
            priorityIndicator = itemView.findViewById(R.id.view_priority_indicator);
            completedCheckBox = itemView.findViewById(R.id.checkbox_complete);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTaskClick(taskList.get(position), position);
                }
            });

            completedCheckBox.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    boolean isChecked = completedCheckBox.isChecked();
                    listener.onCheckboxClicked(taskList.get(position), position, isChecked);
                }
            });
        }
    }
} 