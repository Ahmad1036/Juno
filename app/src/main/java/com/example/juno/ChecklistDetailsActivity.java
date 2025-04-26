package com.example.juno;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChecklistDetailsActivity extends AppCompatActivity {

    private String checklistId;
    private String userId;
    private DatabaseReference mDatabase;
    
    private TextView titleTextView;
    private TextView progressTextView;
    private RecyclerView itemsRecyclerView;
    private EditText newItemEditText;
    private Button addItemButton;
    private FloatingActionButton editFab;
    
    private List<ChecklistItem> itemsList = new ArrayList<>();
    private ChecklistItemAdapter adapter;
    private Checklist currentChecklist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checklist_details);
        
        // Get checklist ID from intent
        checklistId = getIntent().getStringExtra("checklistId");
        if (checklistId == null) {
            finish();
            return;
        }
        
        // Get user ID
        SharedPreferences prefs = getSharedPreferences("JunoUserPrefs", MODE_PRIVATE);
        userId = prefs.getString("userId", "");
        if (userId.isEmpty()) {
            finish();
            return;
        }
        
        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance("https://juno-aa7d2-default-rtdb.firebaseio.com/").getReference();
        
        // Initialize views
        initializeViews();
        
        // Setup RecyclerView
        setupRecyclerView();
        
        // Setup back button
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        
        // Setup add item button
        addItemButton.setOnClickListener(v -> addNewItem());
        
        // Setup edit button
        editFab.setOnClickListener(v -> {
            // In a real app, navigate to edit screen or show edit dialog
            Toast.makeText(this, "Edit functionality would be implemented here", Toast.LENGTH_SHORT).show();
        });
        
        // Load checklist data
        loadChecklistData();
    }
    
    private void initializeViews() {
        titleTextView = findViewById(R.id.checklist_title);
        progressTextView = findViewById(R.id.checklist_progress);
        itemsRecyclerView = findViewById(R.id.checklist_items_recycler_view);
        newItemEditText = findViewById(R.id.new_item_edit_text);
        addItemButton = findViewById(R.id.add_item_button);
        editFab = findViewById(R.id.edit_checklist_fab);
    }
    
    private void setupRecyclerView() {
        adapter = new ChecklistItemAdapter(this, itemsList, (position, isCompleted) -> {
            // Update item completion status in Firebase
            if (currentChecklist != null && position < itemsList.size()) {
                ChecklistItem item = itemsList.get(position);
                mDatabase.child("users").child(userId).child("checklists")
                        .child(checklistId).child("items").child(String.valueOf(position))
                        .child("completed").setValue(isCompleted);
            }
        });
        
        itemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        itemsRecyclerView.setAdapter(adapter);
    }
    
    private void loadChecklistData() {
        mDatabase.child("users").child(userId).child("checklists").child(checklistId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        currentChecklist = dataSnapshot.getValue(Checklist.class);
                        if (currentChecklist != null) {
                            updateUI(currentChecklist);
                        } else {
                            Toast.makeText(ChecklistDetailsActivity.this, "Checklist not found", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(ChecklistDetailsActivity.this, "Failed to load checklist", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void updateUI(Checklist checklist) {
        titleTextView.setText(checklist.getTitle());
        
        // Update items list
        itemsList.clear();
        if (checklist.getItems() != null) {
            itemsList.addAll(checklist.getItems());
        }
        adapter.notifyDataSetChanged();
        
        // Update progress
        int completed = checklist.getCompletedItemCount();
        int total = checklist.getTotalItemCount();
        String progressText = completed + " of " + total + " completed";
        progressTextView.setText(progressText);
    }
    
    private void addNewItem() {
        String itemText = newItemEditText.getText().toString().trim();
        if (itemText.isEmpty()) {
            Toast.makeText(this, "Please enter item text", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ChecklistItem newItem = new ChecklistItem(itemText);
        
        // Add to Firebase
        if (currentChecklist != null) {
            List<ChecklistItem> items = currentChecklist.getItems();
            if (items == null) {
                items = new ArrayList<>();
                currentChecklist.setItems(items);
            }
            
            items.add(newItem);
            
            mDatabase.child("users").child(userId).child("checklists")
                    .child(checklistId).child("items").setValue(items)
                    .addOnSuccessListener(aVoid -> {
                        newItemEditText.setText("");
                        Toast.makeText(ChecklistDetailsActivity.this, "Item added", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> 
                            Toast.makeText(ChecklistDetailsActivity.this, "Failed to add item", Toast.LENGTH_SHORT).show());
        }
    }
    
    // Inner class for the adapter
    private static class ChecklistItemAdapter extends RecyclerView.Adapter<ChecklistItemAdapter.ViewHolder> {
        
        private final Context context;
        private final List<ChecklistItem> items;
        private final ItemCheckListener listener;
        
        public interface ItemCheckListener {
            void onItemCheckedChanged(int position, boolean isCompleted);
        }
        
        public ChecklistItemAdapter(Context context, List<ChecklistItem> items, ItemCheckListener listener) {
            this.context = context;
            this.items = items;
            this.listener = listener;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_checklist_item, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChecklistItem item = items.get(position);
            
            holder.checkBox.setText(item.getText());
            holder.checkBox.setChecked(item.isCompleted());
            
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onItemCheckedChanged(position, isChecked);
                }
            });
        }
        
        @Override
        public int getItemCount() {
            return items.size();
        }
        
        public static class ViewHolder extends RecyclerView.ViewHolder {
            CheckBox checkBox;
            
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                checkBox = itemView.findViewById(R.id.item_checkbox);
            }
        }
    }
} 