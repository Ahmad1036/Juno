package com.example.juno.interfaces;

import com.example.juno.model.Task;

public interface TaskListener {
    void onTaskClick(Task task, int position);
    void onCheckboxClick(Task task, int position, boolean isChecked);
} 