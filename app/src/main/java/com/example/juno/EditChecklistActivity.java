package com.example.juno;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class EditChecklistActivity extends AppCompatActivity {

    private String checklistId;
    private String userId;
    private DatabaseReference mDatabase;
    
    private EditText titleEditText;
    private Button saveButton;
    private Checklist currentChecklist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_checklist);
        
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
        titleEditText = findViewById(R.id.checklist_title_edit);
        saveButton = findViewById(R.id.save_checklist_button);
        
        // Setup back button
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        
        // Setup save button
        saveButton.setOnClickListener(v -> saveChecklist());
        
        // Load checklist data
        loadChecklistData();
    }
    
    private void loadChecklistData() {
        mDatabase.child("users").child(userId).child("checklists").child(checklistId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        currentChecklist = dataSnapshot.getValue(Checklist.class);
                        if (currentChecklist != null) {
                            titleEditText.setText(currentChecklist.getTitle());
                        } else {
                            Toast.makeText(EditChecklistActivity.this, "Checklist not found", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(EditChecklistActivity.this, "Failed to load checklist", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void saveChecklist() {
        String title = titleEditText.getText().toString().trim();
        
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentChecklist != null) {
            // Update title
            currentChecklist.setTitle(title);
            
            // Save to Firebase
            mDatabase.child("users").child(userId).child("checklists").child(checklistId)
                    .child("title").setValue(title)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(EditChecklistActivity.this, "Checklist updated", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> 
                            Toast.makeText(EditChecklistActivity.this, "Failed to update checklist", Toast.LENGTH_SHORT).show());
        }
    }
} 