package com.example.juno;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SelectableTaskAdapter extends RecyclerView.Adapter<SelectableTaskAdapter.TaskViewHolder> {

    private List<Task> allTaskList;
    private List<Task> filteredTaskList;
    private Context context;
    private OnTaskClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private boolean selectMode = false;
    private Set<String> selectedTaskIds = new HashSet<>();

    public interface OnTaskClickListener {
        void onTaskClick(Task task, int position);
        void onCheckboxClicked(Task task, int position, boolean isChecked);
        void onSelectionChanged(int count);
    }

    public SelectableTaskAdapter(Context context, List<Task> taskList, OnTaskClickListener listener) {
        this.context = context;
        this.allTaskList = new ArrayList<>(taskList);
        this.filteredTaskList = new ArrayList<>(taskList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task_selectable, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = filteredTaskList.get(position);
        
        holder.titleTextView.setText(task.getTitle());
        holder.descriptionTextView.setText(task.getDescription());
        
        // Show or hide selection checkbox
        holder.selectCheckBox.setVisibility(selectMode ? View.VISIBLE : View.GONE);
        holder.selectCheckBox.setChecked(selectedTaskIds.contains(task.getId()));
        
        // Format and show due date if available
        if (task.getDueDate() > 0) {
            holder.dueDateTextView.setText(dateFormat.format(new Date(task.getDueDate())));
            holder.dueDateTextView.setVisibility(View.VISIBLE);
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
        } else {
            holder.titleTextView.setPaintFlags(holder.titleTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
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
        
        // Set list indicator
        TextView listNameView = holder.listNameView;
        if (listNameView != null) {
            listNameView.setText("List " + task.getListId());
            listNameView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return filteredTaskList.size();
    }

    public void updateData(List<Task> newTasks) {
        this.allTaskList = new ArrayList<>();
        
        // Filter out the default task
        for (Task task : newTasks) {
            if (task.getTitle() == null || !task.getTitle().toLowerCase().contains("i need to study")) {
                this.allTaskList.add(task);
            }
        }
        
        this.filteredTaskList = new ArrayList<>(this.allTaskList);
        selectedTaskIds.clear();
        if (listener != null) {
            listener.onSelectionChanged(0);
        }
        notifyDataSetChanged();
    }
    
    public void setSelectMode(boolean selectMode) {
        if (this.selectMode != selectMode) {
            this.selectMode = selectMode;
            if (!selectMode) {
                selectedTaskIds.clear();
                if (listener != null) {
                    listener.onSelectionChanged(0);
                }
            }
            notifyDataSetChanged();
        }
    }
    
    public boolean isSelectMode() {
        return selectMode;
    }
    
    public Set<String> getSelectedTaskIds() {
        return new HashSet<>(selectedTaskIds);
    }
    
    public List<Task> getSelectedTasks() {
        List<Task> selectedTasks = new ArrayList<>();
        for (Task task : filteredTaskList) {
            if (selectedTaskIds.contains(task.getId())) {
                selectedTasks.add(task);
            }
        }
        return selectedTasks;
    }
    
    public void selectAll() {
        selectedTaskIds.clear();
        for (Task task : filteredTaskList) {
            selectedTaskIds.add(task.getId());
        }
        if (listener != null) {
            listener.onSelectionChanged(selectedTaskIds.size());
        }
        notifyDataSetChanged();
    }
    
    public void clearSelection() {
        selectedTaskIds.clear();
        if (listener != null) {
            listener.onSelectionChanged(0);
        }
        notifyDataSetChanged();
    }
    
    /**
     * Returns the current filtered list of tasks
     */
    public List<Task> getFilteredTaskList() {
        return new ArrayList<>(filteredTaskList);
    }
    
    public void filterBySearchQuery(String query) {
        applyFilters(query, null, null, null);
    }
    
    public void applyFilters(String searchQuery, String priorityFilter, Boolean completedFilter, Date[] dateRange) {
        filteredTaskList = new ArrayList<>();
        
        for (Task task : allTaskList) {
            // Skip the default task
            if (task.getTitle() != null && task.getTitle().toLowerCase().contains("i need to study")) {
                continue;
            }
            
            boolean matchesSearch = true;
            boolean matchesPriority = true;
            boolean matchesCompletion = true;
            boolean matchesDateRange = true;
            
            // Apply search filter - handle null values safely
            if (searchQuery != null && !searchQuery.isEmpty()) {
                String title = task.getTitle() != null ? task.getTitle().toLowerCase() : "";
                String description = task.getDescription() != null ? task.getDescription().toLowerCase() : "";
                String query = searchQuery.toLowerCase();
                matchesSearch = title.contains(query) || description.contains(query);
            }
            
            // Apply priority filter - handle null values safely
            if (priorityFilter != null && !priorityFilter.isEmpty()) {
                String taskPriority = task.getPriority();
                matchesPriority = taskPriority != null && taskPriority.equalsIgnoreCase(priorityFilter);
            }
            
            // Apply completion status filter
            if (completedFilter != null) {
                matchesCompletion = task.isCompleted() == completedFilter;
            }
            
            // Apply date range filter - handle null values safely
            if (dateRange != null && dateRange.length == 2 && dateRange[0] != null && dateRange[1] != null) {
                long startDate = dateRange[0].getTime();
                long endDate = dateRange[1].getTime();
                long taskDate = task.getDueDate();
                
                // Only apply date filter if task has a due date
                if (taskDate > 0) {
                    matchesDateRange = taskDate >= startDate && taskDate <= endDate;
                } else {
                    // If no due date is set and we're filtering by date, exclude this task
                    matchesDateRange = false;
                }
            }
            
            if (matchesSearch && matchesPriority && matchesCompletion && matchesDateRange) {
                filteredTaskList.add(task);
            }
        }
        
        // Clear selections when filters change
        selectedTaskIds.clear();
        if (listener != null) {
            listener.onSelectionChanged(0);
        }
        
        notifyDataSetChanged();
    }

    public class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView descriptionTextView;
        TextView dueDateTextView;
        TextView listNameView;
        View priorityIndicator;
        CheckBox completedCheckBox;
        CheckBox selectCheckBox;
        ImageView imageIndicator;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.tv_task_title);
            descriptionTextView = itemView.findViewById(R.id.tv_task_description);
            dueDateTextView = itemView.findViewById(R.id.tv_due_date);
            listNameView = itemView.findViewById(R.id.tv_list_name);
            priorityIndicator = itemView.findViewById(R.id.view_priority_indicator);
            completedCheckBox = itemView.findViewById(R.id.checkbox_complete);
            selectCheckBox = itemView.findViewById(R.id.checkbox_select);
            imageIndicator = itemView.findViewById(R.id.image_indicator);

            // Handle item click
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    Task task = filteredTaskList.get(position);
                    
                    if (selectMode) {
                        toggleSelection(task);
                    } else {
                        listener.onTaskClick(task, position);
                    }
                }
            });

            // Handle completion checkbox
            completedCheckBox.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    Task task = filteredTaskList.get(position);
                    task.setCompleted(completedCheckBox.isChecked());
                    listener.onCheckboxClicked(task, position, completedCheckBox.isChecked());
                    notifyItemChanged(position);
                }
            });
            
            // Handle selection checkbox
            selectCheckBox.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Task task = filteredTaskList.get(position);
                    toggleSelection(task);
                }
            });
        }
        
        private void toggleSelection(Task task) {
            String taskId = task.getId();
            if (selectedTaskIds.contains(taskId)) {
                selectedTaskIds.remove(taskId);
            } else {
                selectedTaskIds.add(taskId);
            }
            
            if (listener != null) {
                listener.onSelectionChanged(selectedTaskIds.size());
            }
            
            notifyItemChanged(getAdapterPosition());
        }
    }
} 