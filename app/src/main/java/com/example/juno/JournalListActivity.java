package com.example.juno;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class JournalListActivity extends AppCompatActivity implements JournalListAdapter.OnJournalClickListener {

    private static final String TAG = "JournalListActivity";

    // UI Components
    private RecyclerView journalRecyclerView;
    private LinearLayout emptyState;
    private ProgressBar loadingIndicator;
    private FloatingActionButton addJournalFab;
    private ImageButton backButton;

    // Data
    private List<Journal> journalList;
    private JournalListAdapter adapter;
    private String userId;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal_list);

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance("https://juno-aa7d2-default-rtdb.firebaseio.com/").getReference();

        // Get user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("JunoUserPrefs", MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        if (userId == null || userId.isEmpty()) {
            // User not logged in, redirect to SignInActivity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }

        // Initialize UI components
        initializeViews();

        // Set up RecyclerView
        journalList = new ArrayList<>();
        adapter = new JournalListAdapter(this, journalList, this);
        journalRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        journalRecyclerView.setAdapter(adapter);

        // Set up click listeners
        setupClickListeners();

        // Load journal entries
        loadJournalEntries();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh journal entries when returning to this activity
        loadJournalEntries();
    }

    private void initializeViews() {
        journalRecyclerView = findViewById(R.id.journal_entries_recycler_view);
        emptyState = findViewById(R.id.empty_state);
        loadingIndicator = findViewById(R.id.loading_indicator);
        addJournalFab = findViewById(R.id.add_journal_fab);
        backButton = findViewById(R.id.journal_list_back_button);
    }

    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());

        // Add journal entry button
        addJournalFab.setOnClickListener(v -> {
            Intent intent = new Intent(this, JournalActivity.class);
            startActivity(intent);
        });
    }

    private void loadJournalEntries() {
        // Show loading indicator
        loadingIndicator.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        journalRecyclerView.setVisibility(View.GONE);

        Log.d(TAG, "Loading journal entries for userId: " + userId);

        // Query journals directly without orderByChild since we're using equalTo
        Query query = mDatabase.child("journals");

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                journalList.clear();
                Log.d(TAG, "Retrieved total journals: " + dataSnapshot.getChildrenCount());

                // Process results
                for (DataSnapshot journalSnapshot : dataSnapshot.getChildren()) {
                    Journal journal = journalSnapshot.getValue(Journal.class);
                    if (journal != null) {
                        String journalUserId = journal.getUserId();
                        Log.d(TAG, "Journal ID: " + journalSnapshot.getKey() + 
                                " | Journal userId: " + journalUserId + 
                                " | Current userId: " + userId);
                        
                        // Only add journals belonging to this user
                        if (userId.equals(journalUserId)) {
                            journal.setId(journalSnapshot.getKey());
                            journalList.add(journal);
                            Log.d(TAG, "Added journal: " + journal.getContent());
                        }
                    }
                }

                // Sort the list by timestamp in descending order (newest first)
                journalList.sort((j1, j2) -> Long.compare(j2.getTimestamp(), j1.getTimestamp()));

                // Update UI
                loadingIndicator.setVisibility(View.GONE);
                
                Log.d(TAG, "Final journal list size: " + journalList.size());
                if (journalList.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    journalRecyclerView.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    journalRecyclerView.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading journal entries", databaseError.toException());
                loadingIndicator.setVisibility(View.GONE);
                emptyState.setVisibility(View.VISIBLE);
                journalRecyclerView.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onJournalClick(Journal journal) {
        Intent intent = new Intent(this, JournalActivity.class);
        intent.putExtra("journalId", journal.getId());
        startActivity(intent);
    }
} 