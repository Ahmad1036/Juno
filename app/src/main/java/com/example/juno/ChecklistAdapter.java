package com.example.juno;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class ChecklistAdapter extends RecyclerView.Adapter<ChecklistAdapter.ViewHolder> {

    private final Context context;
    private final List<Checklist> checklistList;
    private final DatabaseReference mDatabase;

    public ChecklistAdapter(Context context, List<Checklist> checklistList) {
        this.context = context;
        this.checklistList = checklistList;
        this.mDatabase = FirebaseDatabase.getInstance("https://juno-aa7d2-default-rtdb.firebaseio.com/").getReference();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_checklist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Checklist checklist = checklistList.get(position);
        
        holder.titleTextView.setText(checklist.getTitle());
        
        // Set statistics
        int completedItems = checklist.getCompletedItemCount();
        int totalItems = checklist.getTotalItemCount();
        String statsText = completedItems + "/" + totalItems + " completed";
        holder.statsTextView.setText(statsText);
        
        // Set click listener for the entire item
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChecklistDetailsActivity.class);
            intent.putExtra("checklistId", checklist.getId());
            context.startActivity(intent);
        });
        
        // Set click listener for the menu button
        holder.menuButton.setOnClickListener(v -> {
            showPopupMenu(holder.menuButton, checklist);
        });
    }

    private void showPopupMenu(View view, Checklist checklist) {
        PopupMenu popupMenu = new PopupMenu(context, view);
        popupMenu.inflate(R.menu.checklist_menu);
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            String userId = context.getSharedPreferences("JunoUserPrefs", Context.MODE_PRIVATE)
                    .getString("userId", "");
            
            if (id == R.id.menu_edit) {
                Intent intent = new Intent(context, EditChecklistActivity.class);
                intent.putExtra("checklistId", checklist.getId());
                context.startActivity(intent);
                return true;
            } else if (id == R.id.menu_delete) {
                // Delete the checklist
                mDatabase.child("users").child(userId).child("checklists")
                        .child(checklist.getId()).removeValue()
                        .addOnSuccessListener(aVoid -> 
                            Toast.makeText(context, "Checklist deleted", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> 
                            Toast.makeText(context, "Failed to delete checklist", Toast.LENGTH_SHORT).show());
                return true;
            }
            return false;
        });
        
        popupMenu.show();
    }

    @Override
    public int getItemCount() {
        return checklistList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView statsTextView;
        ImageButton menuButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.checklist_title);
            statsTextView = itemView.findViewById(R.id.checklist_stats);
            menuButton = itemView.findViewById(R.id.checklist_menu);
        }
    }
} 