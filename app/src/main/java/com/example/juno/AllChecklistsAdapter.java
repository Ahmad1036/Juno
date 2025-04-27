package com.example.juno;

import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AllChecklistsAdapter extends RecyclerView.Adapter<AllChecklistsAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private List<Task> filteredList;
    private final Context context;
    private final OnTaskActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    
    // For multi-selection
    private final Set<String> selectedTaskIds = new HashSet<>();
    private boolean isSelectionMode = false;
    
    // Task list names (for display)
    private List<String> taskListNames;

    public interface OnTaskActionListener {
        void onTaskClick(Task task, int position);
        void onCheckboxClicked(Task task, int position, boolean isChecked);
        void onTaskSelected(int selectedCount);
        String getTaskListName(int listId);
    }

    public AllChecklistsAdapter(Context context, List<Task> taskList, OnTaskActionListener listener) {
        this.context = context;
        this.taskList = taskList;
        this.filteredList = new ArrayList<>(taskList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_checklist_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = filteredList.get(position);
        
        // Set task title
        holder.titleTextView.setText(task.getTitle());
        
        // Set task description
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            holder.descriptionTextView.setText(task.getDescription());
            holder.descriptionTextView.setVisibility(View.VISIBLE);
        } else {
            holder.descriptionTextView.setVisibility(View.GONE);
        }
        
        // Format and show task list name
        if (task.getListId() > 0 && listener != null) {
            String listName = listener.getTaskListName(task.getListId());
            if (listName != null && !listName.isEmpty()) {
                holder.listNameTextView.setText(listName);
                holder.listNameTextView.setVisibility(View.VISIBLE);
            } else {
                holder.listNameTextView.setVisibility(View.GONE);
            }
        } else {
            holder.listNameTextView.setVisibility(View.GONE);
        }
        
        // Format and show due date if available
        if (task.getDueDate() > 0) {
            holder.dueDateTextView.setText(dateFormat.format(new Date(task.getDueDate())));
            holder.dueDateTextView.setVisibility(View.VISIBLE);
            
            // Highlight overdue tasks
            if (task.getDueDate() < System.currentTimeMillis() && !task.isCompleted()) {
                holder.dueDateTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_light));
            } else {
                holder.dueDateTextView.setTextColor(ContextCompat.getColor(context, android.R.color.white));
            }
        } else {
            holder.dueDateTextView.setVisibility(View.GONE);
        }
        
        // Set priority indicator
        int priorityValue = task.getPriorityInt();
        switch (priorityValue) {
            case 1: // High
                holder.priorityIndicator.setBackgroundResource(R.drawable.priority_high_indicator);
                break;
            case 2: // Medium
                holder.priorityIndicator.setBackgroundResource(R.drawable.priority_medium_indicator);
                break;
            case 3: // Low
                holder.priorityIndicator.setBackgroundResource(R.drawable.priority_low_indicator);
                break;
            default:
                holder.priorityIndicator.setBackgroundResource(R.drawable.priority_low_indicator);
                break;
        }
        
        // Set completion status
        holder.completedCheckBox.setChecked(task.isCompleted());
        
        // Apply strikethrough for completed tasks
        if (task.isCompleted()) {
            holder.titleTextView.setPaintFlags(holder.titleTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.titleTextView.setAlpha(0.7f);
        } else {
            holder.titleTextView.setPaintFlags(holder.titleTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.titleTextView.setAlpha(1.0f);
        }

        // Show image thumbnail if available
        if (task.getImageUrl() != null && !task.getImageUrl().isEmpty()) {
            holder.imageIndicator.setVisibility(View.VISIBLE);
            Glide.with(context)
                 .load(task.getImageUrl())
                 .centerCrop()
                 .thumbnail(0.1f)
                 .into(holder.imageIndicator);
        } else {
            holder.imageIndicator.setVisibility(View.GONE);
        }
        
        // Set selection state
        boolean isSelected = selectedTaskIds.contains(task.getId());
        Log.d("AllChecklistsAdapter", "Setting selection state for task " + task.getId() + ": " + isSelected + " (selection mode: " + isSelectionMode + ")");
        
        // Update selection visuals
        holder.itemView.setActivated(isSelected);
        holder.selectionIndicator.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
        
        // In selection mode, make sure the selection indicator has a stronger visual cue
        if (isSelectionMode) {
            holder.itemView.setBackgroundColor(isSelected ? 
                ContextCompat.getColor(context, R.color.colorPrimaryDark) : 
                ContextCompat.getColor(context, android.R.color.transparent));
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        }
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }
    
    // Method to toggle selection mode
    public void toggleSelectionMode(boolean enabled) {
        Log.d("AllChecklistsAdapter", "Toggle selection mode: " + enabled + ", current mode: " + isSelectionMode);
        
        if (!enabled) {
            Log.d("AllChecklistsAdapter", "Clearing selection. Current selected count: " + selectedTaskIds.size());
            selectedTaskIds.clear();
        }
        
        this.isSelectionMode = enabled;
        notifyDataSetChanged();
        
        if (listener != null) {
            listener.onTaskSelected(selectedTaskIds.size());
        }
    }
    
    // Method to check if selection mode is active
    public boolean isInSelectionMode() {
        return isSelectionMode;
    }
    
    // Method to get selected task IDs
    public Set<String> getSelectedTaskIds() {
        return new HashSet<>(selectedTaskIds);
    }
    
    // Method to select all tasks
    public void selectAllTasks() {
        selectedTaskIds.clear();
        for (Task task : filteredList) {
            selectedTaskIds.add(task.getId());
        }
        notifyDataSetChanged();
        
        if (listener != null) {
            listener.onTaskSelected(selectedTaskIds.size());
        }
    }
    
    // Method to deselect all tasks
    public void clearSelection() {
        selectedTaskIds.clear();
        notifyDataSetChanged();
        
        if (listener != null) {
            listener.onTaskSelected(0);
        }
    }
    
    // Method to get the selected tasks
    public List<Task> getSelectedTasks() {
        List<Task> selectedTasks = new ArrayList<>();
        Log.d("AllChecklistsAdapter", "Getting selected tasks. Selection mode: " + isSelectionMode + ", Selected IDs: " + selectedTaskIds.size());
        
        for (Task task : filteredList) {
            if (selectedTaskIds.contains(task.getId())) {
                selectedTasks.add(task);
                Log.d("AllChecklistsAdapter", "Added selected task: " + task.getId() + " - " + task.getTitle());
            }
        }
        
        Log.d("AllChecklistsAdapter", "Total selected tasks: " + selectedTasks.size());
        return selectedTasks;
    }
    
    // Method to update dataset
    public void updateData(List<Task> newTasks) {
        this.taskList = newTasks;
        applyFilters();
    }
    
    // Apply filters based on search query, priority, etc.
    public void applyFilters(String query, Set<String> priorities, boolean showCompleted, boolean showIncomplete, long startDate, long endDate) {
        filteredList.clear();
        
        for (Task task : taskList) {
            // Check search query
            boolean matchesQuery = query.isEmpty() ||
                    (task.getTitle() != null && task.getTitle().toLowerCase().contains(query.toLowerCase())) ||
                    (task.getDescription() != null && task.getDescription().toLowerCase().contains(query.toLowerCase()));
            
            // Check priority
            boolean matchesPriority = priorities.isEmpty() || (task.getPriority() != null && priorities.contains(task.getPriority().toLowerCase()));
            
            // Check completion status
            boolean matchesStatus = (showCompleted && task.isCompleted()) || (showIncomplete && !task.isCompleted());
            
            // Check date range
            boolean matchesDateRange = (startDate <= 0 && endDate <= 0) || 
                    (task.getDueDate() > 0 && task.getDueDate() >= startDate && task.getDueDate() <= endDate);
            
            if (matchesQuery && matchesPriority && matchesStatus && matchesDateRange) {
                filteredList.add(task);
            }
        }
        
        // Notify that data has changed
        notifyDataSetChanged();
    }
    
    // Simple reset to show all tasks
    public void applyFilters() {
        filteredList.clear();
        filteredList.addAll(taskList);
        notifyDataSetChanged();
    }

    public class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView descriptionTextView;
        TextView dueDateTextView;
        TextView listNameTextView;
        View priorityIndicator;
        CheckBox completedCheckBox;
        ImageView imageIndicator;
        View selectionIndicator;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.tv_task_title);
            descriptionTextView = itemView.findViewById(R.id.tv_task_description);
            dueDateTextView = itemView.findViewById(R.id.tv_due_date);
            listNameTextView = itemView.findViewById(R.id.tv_list_name);
            priorityIndicator = itemView.findViewById(R.id.view_priority_indicator);
            completedCheckBox = itemView.findViewById(R.id.checkbox_complete);
            imageIndicator = itemView.findViewById(R.id.image_indicator);
            selectionIndicator = itemView.findViewById(R.id.selection_indicator);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Task task = filteredList.get(position);
                    
                    if (isSelectionMode) {
                        // Toggle selection
                        boolean wasSelected = selectedTaskIds.contains(task.getId());
                        Log.d("AllChecklistsAdapter", "Task click in selection mode - ID: " + task.getId() + ", was selected: " + wasSelected);
                        
                        if (wasSelected) {
                            selectedTaskIds.remove(task.getId());
                        } else {
                            selectedTaskIds.add(task.getId());
                        }
                        
                        Log.d("AllChecklistsAdapter", "After toggle - Selected count: " + selectedTaskIds.size());
                        notifyItemChanged(position);
                        
                        if (listener != null) {
                            listener.onTaskSelected(selectedTaskIds.size());
                        }
                    } else if (listener != null) {
                        // Normal click
                        Log.d("AllChecklistsAdapter", "Normal task click - ID: " + task.getId());
                        listener.onTaskClick(task, position);
                    }
                }
            });
            
            // Long press to enter selection mode
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && !isSelectionMode) {
                    Task task = filteredList.get(position);
                    Log.d("AllChecklistsAdapter", "Long press - entering selection mode for task: " + task.getId());
                    
                    isSelectionMode = true;
                    selectedTaskIds.add(task.getId());
                    Log.d("AllChecklistsAdapter", "Added to selection - count: " + selectedTaskIds.size());
                    
                    notifyDataSetChanged();
                    
                    if (listener != null) {
                        listener.onTaskSelected(selectedTaskIds.size());
                    }
                    return true;
                }
                return false;
            });

            completedCheckBox.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    Task task = filteredList.get(position);
                    task.setCompleted(completedCheckBox.isChecked());
                    notifyItemChanged(position);
                    listener.onCheckboxClicked(task, position, completedCheckBox.isChecked());
                }
            });
        }
    }
} 