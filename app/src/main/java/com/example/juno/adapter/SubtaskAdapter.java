package com.example.juno.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.juno.R;
import com.example.juno.model.Task;

import java.util.List;

public class SubtaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_PARENT = 0;
    private static final int TYPE_SUBTASK = 1;

    private final Context context;
    private final List<Task> taskList;

    public SubtaskAdapter(Context context, List<Task> taskList) {
        this.context = context;
        this.taskList = taskList;
    }

    @Override
    public int getItemViewType(int position) {
        Task task = taskList.get(position);
        if (task.getParentId() == null || task.getParentId().isEmpty()) {
            return TYPE_PARENT;
        } else {
            return TYPE_SUBTASK;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_PARENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_parent_task, parent, false);
            return new ParentTaskViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_subtask, parent, false);
            return new SubtaskViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Task task = taskList.get(position);
        
        if (holder.getItemViewType() == TYPE_PARENT) {
            ParentTaskViewHolder parentHolder = (ParentTaskViewHolder) holder;
            
            // Set parent task title
            parentHolder.taskTitle.setText(task.getTitle());
            
            // Set description if available
            if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                parentHolder.taskDescription.setText(task.getDescription());
                parentHolder.taskDescription.setVisibility(View.VISIBLE);
            } else {
                parentHolder.taskDescription.setVisibility(View.GONE);
            }
        } else {
            SubtaskViewHolder subtaskHolder = (SubtaskViewHolder) holder;
            
            // Set subtask title
            subtaskHolder.subtaskTitle.setText(task.getTitle());
        }
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    static class ParentTaskViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView taskTitle;
        TextView taskDescription;

        ParentTaskViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.parent_task_card);
            taskTitle = itemView.findViewById(R.id.parent_task_title);
            taskDescription = itemView.findViewById(R.id.parent_task_description);
        }
    }

    static class SubtaskViewHolder extends RecyclerView.ViewHolder {
        TextView subtaskTitle;

        SubtaskViewHolder(View itemView) {
            super(itemView);
            subtaskTitle = itemView.findViewById(R.id.subtask_title);
        }
    }
} 