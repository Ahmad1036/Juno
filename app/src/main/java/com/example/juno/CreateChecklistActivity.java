package com.example.juno;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CreateChecklistActivity extends AppCompatActivity {

    private EditText titleEditText;
    private DatabaseReference mDatabase;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_checklist);

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
        Button createButton = findViewById(R.id.create_checklist_button);
        
        // Setup back button
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        
        // Setup create button
        createButton.setOnClickListener(v -> createChecklist());
    }
    
    private void createChecklist() {
        String title = titleEditText.getText().toString().trim();
        
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create new checklist
        Checklist checklist = new Checklist(title);
        
        // Save to Firebase
        String checklistId = mDatabase.child("users").child(userId).child("checklists").push().getKey();
        if (checklistId != null) {
            checklist.setId(checklistId);
            mDatabase.child("users").child(userId).child("checklists").child(checklistId)
                    .setValue(checklist)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(CreateChecklistActivity.this, "Checklist created", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> 
                            Toast.makeText(CreateChecklistActivity.this, "Failed to create checklist", Toast.LENGTH_SHORT).show());
        }
    }
} 