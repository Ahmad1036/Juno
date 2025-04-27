package com.example.juno.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.juno.R;
import com.example.juno.model.Task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskSelectionAdapter extends RecyclerView.Adapter<TaskSelectionAdapter.TaskViewHolder> {

    public interface OnTaskSelectionListener {
        void onTaskSelectionChanged(Task task, boolean isSelected);
    }

    private final Context context;
    private final List<Task> taskList;
    private final OnTaskSelectionListener selectionListener;
    private final Map<String, Boolean> selectedTasks;

    public TaskSelectionAdapter(Context context, List<Task> taskList, OnTaskSelectionListener selectionListener) {
        this.context = context;
        this.taskList = taskList;
        this.selectionListener = selectionListener;
        this.selectedTasks = new HashMap<>();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_selection, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        
        // Set task title and description
        holder.taskTitle.setText(task.getTitle());
        
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            holder.taskDescription.setText(task.getDescription());
            holder.taskDescription.setVisibility(View.VISIBLE);
        } else {
            holder.taskDescription.setVisibility(View.GONE);
        }
        
        // Set priority indicator color
        int priorityColor;
        switch (task.getPriority().toLowerCase()) {
            case "high":
                priorityColor = ContextCompat.getColor(context, R.color.high_priority);
                break;
            case "medium":
                priorityColor = ContextCompat.getColor(context, R.color.medium_priority);
                break;
            case "low":
                priorityColor = ContextCompat.getColor(context, R.color.low_priority);
                break;
            default:
                priorityColor = ContextCompat.getColor(context, R.color.colorPrimary);
                break;
        }
        holder.priorityIndicator.setBackgroundColor(priorityColor);
        
        // Set due date if available
        if (task.getDueDate() > 0) {
            String formattedDate = android.text.format.DateFormat.format("MMM dd, yyyy", task.getDueDate()).toString();
            holder.dueDate.setText(formattedDate);
            holder.dueDate.setVisibility(View.VISIBLE);
        } else {
            holder.dueDate.setVisibility(View.GONE);
        }
        
        // Set selection state
        boolean isSelected = selectedTasks.containsKey(task.getId()) && selectedTasks.get(task.getId());
        holder.selectionCheckbox.setChecked(isSelected);
        
        // Handle item selection
        holder.itemView.setOnClickListener(v -> {
            holder.selectionCheckbox.toggle();
        });
        
        holder.selectionCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            selectedTasks.put(task.getId(), isChecked);
            selectionListener.onTaskSelectionChanged(task, isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        public View priorityIndicator;
        public TextView taskTitle;
        public TextView taskDescription;
        public TextView dueDate;
        public CheckBox selectionCheckbox;
        public CardView cardView;

        public TaskViewHolder(View itemView) {
            super(itemView);
            priorityIndicator = itemView.findViewById(R.id.priority_indicator);
            taskTitle = itemView.findViewById(R.id.task_title);
            taskDescription = itemView.findViewById(R.id.task_description);
            dueDate = itemView.findViewById(R.id.due_date);
            selectionCheckbox = itemView.findViewById(R.id.selection_checkbox);
            cardView = itemView.findViewById(R.id.task_card);
        }
    }
} 