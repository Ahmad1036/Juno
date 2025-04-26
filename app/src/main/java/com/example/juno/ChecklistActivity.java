package com.example.juno;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
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

public class ChecklistActivity extends AppCompatActivity {

    private static final String TAG = "ChecklistActivity";

    // UI Components
    private RecyclerView checklistRecyclerView;
    private TextView emptyStateText;
    private FloatingActionButton addChecklistFab;
    
    // Firebase
    private DatabaseReference mDatabase;
    private String userId;
    
    // Data
    private List<Checklist> checklistList = new ArrayList<>();
    private ChecklistAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checklist);

        // Get user ID
        SharedPreferences prefs = getSharedPreferences("JunoUserPrefs", MODE_PRIVATE);
        userId = prefs.getString("userId", "");
        if (userId.isEmpty()) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance("https://juno-aa7d2-default-rtdb.firebaseio.com/").getReference();
        
        // Initialize views
        initializeViews();
        
        // Setup UI
        setupRecyclerView();
        setupClickListeners();
        
        // Load data
        loadChecklists();
    }

    private void initializeViews() {
        checklistRecyclerView = findViewById(R.id.checklist_recycler_view);
        emptyStateText = findViewById(R.id.empty_state_text);
        addChecklistFab = findViewById(R.id.add_checklist_fab);
    }

    private void setupRecyclerView() {
        adapter = new ChecklistAdapter(this, checklistList);
        checklistRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        checklistRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        // Back button
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        
        // Add checklist FAB
        addChecklistFab.setOnClickListener(v -> {
            // Navigate to create checklist screen
            Intent intent = new Intent(ChecklistActivity.this, CreateChecklistActivity.class);
            startActivity(intent);
        });
    }

    private void loadChecklists() {
        mDatabase.child("users").child(userId).child("checklists")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        checklistList.clear();
                        
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Checklist checklist = snapshot.getValue(Checklist.class);
                            if (checklist != null) {
                                checklist.setId(snapshot.getKey());
                                checklistList.add(checklist);
                            }
                        }
                        
                        adapter.notifyDataSetChanged();
                        
                        // Update empty state visibility
                        if (checklistList.isEmpty()) {
                            emptyStateText.setVisibility(View.VISIBLE);
                            checklistRecyclerView.setVisibility(View.GONE);
                        } else {
                            emptyStateText.setVisibility(View.GONE);
                            checklistRecyclerView.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(ChecklistActivity.this, "Failed to load checklists", Toast.LENGTH_SHORT).show();
                    }
                });
    }
} 