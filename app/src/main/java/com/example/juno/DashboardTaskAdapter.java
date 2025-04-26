package com.example.juno;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardTaskAdapter extends RecyclerView.Adapter<DashboardTaskAdapter.TaskViewHolder> {

    private List<Task> tasks;
    private final Context context;
    private final OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public DashboardTaskAdapter(Context context, List<Task> tasks, OnTaskClickListener listener) {
        this.context = context;
        this.tasks = tasks;
        this.listener = listener;
    }

    public void updateData(List<Task> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_dashboard_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.bindTask(task);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private final TextView taskTitleText;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitleText = itemView.findViewById(R.id.dashboard_task_title);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTaskClick(tasks.get(position));
                }
            });
        }

        void bindTask(Task task) {
            String bulletedTitle = "• " + task.getTitle();
            taskTitleText.setText(bulletedTitle);
        }
    }
} 