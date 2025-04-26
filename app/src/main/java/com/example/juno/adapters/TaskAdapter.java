package com.example.juno.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.juno.R;
import com.example.juno.models.Task;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    
    private List<Task> taskList;
    private Context context;
    private OnTaskActionListener listener;
    private SimpleDateFormat dateFormat;
    
    public interface OnTaskActionListener {
        void onTaskCheckedChanged(Task task, boolean isChecked);
        void onTaskClicked(Task task, int position);
    }
    
    public TaskAdapter(Context context, List<Task> taskList, OnTaskActionListener listener) {
        this.context = context;
        this.taskList = taskList;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }
    
    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        
        holder.taskTitle.setText(task.getTitle());
        holder.taskDescription.setText(task.getDescription());
        
        if (task.getDueDate() != null) {
            holder.taskDueDate.setText(dateFormat.format(task.getDueDate()));
            holder.taskDueDate.setVisibility(View.VISIBLE);
        } else {
            holder.taskDueDate.setVisibility(View.GONE);
        }
        
        // Set priority indicator drawable
        int priorityDrawable;
        switch (task.getPriority()) {
            case 3: // High
                priorityDrawable = R.drawable.priority_high_indicator;
                break;
            case 2: // Medium
                priorityDrawable = R.drawable.priority_medium_indicator;
                break;
            default: // Low
                priorityDrawable = R.drawable.priority_low_indicator;
                break;
        }
        holder.priorityIndicator.setBackground(
                ContextCompat.getDrawable(context, priorityDrawable));
        
        // Set checkbox without triggering listener
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(task.isCompleted());
        
        // Set listeners
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onTaskCheckedChanged(task, isChecked);
            }
        });
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClicked(task, holder.getAdapterPosition());
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }
    
    public void updateTasks(List<Task> newTasks) {
        this.taskList = newTasks;
        notifyDataSetChanged();
    }
    
    public Task getTaskAt(int position) {
        return taskList.get(position);
    }
    
    static class TaskViewHolder extends RecyclerView.ViewHolder {
        View priorityIndicator;
        TextView taskTitle;
        TextView taskDescription;
        TextView taskDueDate;
        CheckBox checkBox;
        
        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            priorityIndicator = itemView.findViewById(R.id.view_priority_indicator);
            taskTitle = itemView.findViewById(R.id.tv_task_title);
            taskDescription = itemView.findViewById(R.id.tv_task_description);
            taskDueDate = itemView.findViewById(R.id.tv_due_date);
            checkBox = itemView.findViewById(R.id.checkbox_complete);
        }
    }
} 