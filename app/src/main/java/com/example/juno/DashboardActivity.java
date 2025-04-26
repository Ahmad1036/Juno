package com.example.juno;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";
    private static final int MAX_DASHBOARD_TASKS = 3;
    private static final int MOOD_IMAGE_CORNER_RADIUS = 20; // 20dp corner radius

    private TextView dateTimeText;
    private TextView greetingText;
    private TextView moodText;
    private CardView tasksCard;
    private CardView journalCard;
    private CardView calendarCard;
    private CardView suggestionsCard;
    private CardView notesSummarizerCard;
    private CardView dailyMotivationCard;
    private TextView dailyQuoteText;
    private ImageButton settingsButton;
    
    // Task-related views
    private RecyclerView dashboardTasksRecyclerView;
    private TextView dashboardTasksCount;
    private TextView dashboardNoTasksText;
    private DashboardTaskAdapter taskAdapter;
    private List<Task> taskList;
    
    // Firebase
    private DatabaseReference mDatabase;

    // User info
    private String userId;
    private String userName;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Check if user is logged in
        if (!checkUserSession()) {
            // User not logged in, redirect to SignInActivity
            startActivity(new Intent(DashboardActivity.this, SignInActivity.class));
            finish();
            return;
        }
        
        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance("https://juno-aa7d2-default-rtdb.firebaseio.com/").getReference();

        // Initialize UI components
        dateTimeText = findViewById(R.id.date_time_text);
        greetingText = findViewById(R.id.greeting_text);
        moodText = findViewById(R.id.mood_text);
        tasksCard = findViewById(R.id.tasks_card);
        journalCard = findViewById(R.id.journal_card);
        calendarCard = findViewById(R.id.calendar_card);
        suggestionsCard = findViewById(R.id.suggestions_card);
        notesSummarizerCard = findViewById(R.id.notes_summarizer_card);
        dailyMotivationCard = findViewById(R.id.daily_motivation_card);
        dailyQuoteText = findViewById(R.id.daily_quote_text);
        settingsButton = findViewById(R.id.settings_button);
        
        // Initialize task views
        dashboardTasksRecyclerView = findViewById(R.id.dashboard_tasks_recycler_view);
        dashboardTasksCount = findViewById(R.id.dashboard_tasks_count);
        dashboardNoTasksText = findViewById(R.id.dashboard_no_tasks_text);
        
        // Set up RecyclerView
        taskList = new ArrayList<>();
        taskAdapter = new DashboardTaskAdapter(this, taskList, this::openTaskDetail);
        dashboardTasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dashboardTasksRecyclerView.setAdapter(taskAdapter);

        // Set up the date and time
        updateDateTime();

        // Set up the greeting
        updateGreeting();

        // Set up click listeners
        setupClickListeners();

        // Set up animations
        setupAnimations();
        
        // Load tasks for dashboard
        loadDashboardTasks();
        
        // Load latest journal and analyze mood
        loadLatestJournal();
        
        // Load daily motivational quote
        loadDailyQuote();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh tasks when returning to dashboard
        loadDashboardTasks();
    }

    private boolean checkUserSession() {
        SharedPreferences prefs = getSharedPreferences("JunoUserPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        
        if (isLoggedIn) {
            userId = prefs.getString("userId", "");
            userName = prefs.getString("userName", "");
            userEmail = prefs.getString("userEmail", "");
            return true;
        }
        
        return false;
    }

    private void logout() {
        // Clear user session
        SharedPreferences prefs = getSharedPreferences("JunoUserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        
        // Redirect to SignInActivity
        startActivity(new Intent(DashboardActivity.this, SignInActivity.class));
        finish();
    }

    private void updateDateTime() {
        // Update the current date and time
        Date currentDate = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        String dateStr = dateFormat.format(currentDate);
        String timeStr = timeFormat.format(currentDate);
        
        dateTimeText.setText(dateStr + " • " + timeStr);
    }

    private void updateGreeting() {
        // Get the current hour of day to determine appropriate greeting
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (hourOfDay < 12) {
            greeting = "good morning";
        } else if (hourOfDay < 18) {
            greeting = "good afternoon";
        } else {
            greeting = "good evening";
        }

        greetingText.setText(greeting);
    }

    private void setupClickListeners() {
        // Tasks card click
        tasksCard.setOnClickListener(v -> {
            // Navigate to Tasks screen
            Intent intent = new Intent(DashboardActivity.this, AllTasksActivity.class);
            startActivity(intent);
        });

        // Journal card click
        journalCard.setOnClickListener(v -> {
            // Navigate to Journal screen
            Intent intent = new Intent(DashboardActivity.this, JournalListActivity.class);
            startActivity(intent);
        });

        // Calendar card click
        calendarCard.setOnClickListener(v -> {
            // Navigate to Calendar screen
            Intent intent = new Intent(DashboardActivity.this, CalendarActivity.class);
            startActivity(intent);
        });
        
        // Create Task card click (new)
        CardView createTaskCard = findViewById(R.id.create_task_card);
        if (createTaskCard != null) {
            createTaskCard.setOnClickListener(v -> {
                // Navigate to Task Creation screen
                Intent intent = new Intent(DashboardActivity.this, CreateTaskActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        // Suggestions card click
        suggestionsCard.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, SuggestionsActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        // Notes Summarizer card click
        notesSummarizerCard.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, NotesSummarizerActivity.class);
            startActivity(intent);
        });

        // Settings button click
        settingsButton.setOnClickListener(v -> {
            // Navigate to Settings screen
            Intent intent = new Intent(DashboardActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void setupAnimations() {
        // Animate UI elements to fade in sequentially
        View[] views = {
            dateTimeText, greetingText, moodText,
            tasksCard, journalCard, calendarCard, suggestionsCard, notesSummarizerCard, dailyMotivationCard,
            settingsButton
        };
        
        for (int i = 0; i < views.length; i++) {
            views[i].setAlpha(0f);
            views[i].animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setStartDelay(100 + (i * 100))
                    .start();
        }
    }
    
    private void loadDashboardTasks() {
        // Show loading state
        dashboardTasksCount.setText("loading tasks...");
        dashboardNoTasksText.setVisibility(View.GONE);
        
        // Query only high priority tasks, limited to 3 for the dashboard
        Query query = mDatabase.child("users").child(userId).child("tasks")
                .orderByChild("priority")
                .equalTo("high")
                .limitToFirst(MAX_DASHBOARD_TASKS);
        
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                taskList.clear();
                
                for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                    Task task = taskSnapshot.getValue(Task.class);
                    if (task != null) {
                        task.setId(taskSnapshot.getKey());
                        taskList.add(task);
                    }
                }
                
                if (taskList.isEmpty()) {
                    // No high-priority tasks, try to get any tasks
                    loadAnyTasks();
                } else {
                    // Update UI with high-priority tasks
                    updateTasksUI();
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading tasks", databaseError.toException());
                dashboardTasksCount.setText("failed to load tasks");
                dashboardNoTasksText.setVisibility(View.VISIBLE);
            }
        });
    }
    
    private void loadAnyTasks() {
        // If there are no high-priority tasks, just get any tasks
        mDatabase.child("users").child(userId).child("tasks")
                .limitToFirst(MAX_DASHBOARD_TASKS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        taskList.clear();
                        
                        for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                            Task task = taskSnapshot.getValue(Task.class);
                            if (task != null) {
                                task.setId(taskSnapshot.getKey());
                                taskList.add(task);
                            }
                        }
                        
                        updateTasksUI();
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error loading any tasks", databaseError.toException());
                        dashboardTasksCount.setText("failed to load tasks");
                        dashboardNoTasksText.setVisibility(View.VISIBLE);
                    }
                });
    }
    
    private void updateTasksUI() {
        if (taskList.isEmpty()) {
            dashboardTasksCount.setText("no tasks yet");
            dashboardNoTasksText.setVisibility(View.VISIBLE);
            dashboardTasksRecyclerView.setVisibility(View.GONE);
        } else {
            int taskCount = taskList.size();
            dashboardTasksCount.setText("you have " + taskCount + " task" + (taskCount > 1 ? "s" : ""));
            dashboardNoTasksText.setVisibility(View.GONE);
            dashboardTasksRecyclerView.setVisibility(View.VISIBLE);
            taskAdapter.updateData(taskList);
        }
    }
    
    private void openTaskDetail(Task task) {
        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra("taskId", task.getId());
        startActivity(intent);
    }

    private void loadLatestJournal() {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID is empty, cannot load latest journal");
            return;
        }
        
        Log.d(TAG, "Loading latest journal entry for user ID: " + userId);
        
        // Query journals and order by timestamp to get the latest one
        mDatabase.child("journals")
                .orderByChild("timestamp")
                .limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot journalSnapshot : dataSnapshot.getChildren()) {
                            Journal journal = journalSnapshot.getValue(Journal.class);
                            if (journal != null && userId.equals(journal.getUserId())) {
                                analyzeJournalMood(journal);
                                return;
                            }
                        }
                        Log.d(TAG, "No journal entries found for this user");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error loading latest journal entry", databaseError.toException());
                    }
                });
    }

    private void analyzeJournalMood(Journal journal) {
        String content = journal.getContent();
        if (content == null || content.isEmpty()) {
            Log.d(TAG, "Journal content is empty, cannot analyze mood");
            return;
        }
        
        // Use Gemini API to analyze mood
        GeminiMoodAnalyzer.analyzeMoodAsync(content, mood -> {
            // We need to run UI updates on the main thread
            runOnUiThread(() -> {
                updateMoodDisplay(mood);
                Log.d(TAG, "Analyzed journal mood with Gemini: " + mood);
            });
            
            // Save the mood to the journal in Firebase
            journal.setMood(mood);
            String journalId = journal.getId();
            if (journalId != null && !journalId.isEmpty()) {
                mDatabase.child("journals").child(journalId).child("mood").setValue(mood)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Journal mood updated successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error updating journal mood", e));
            }
        });
    }

    private void updateMoodDisplay(String mood) {
        // Find the LinearLayout containing the mood emojis
        LinearLayout moodLayout = findViewById(R.id.mood_selection_layout);
        if (moodLayout == null) {
            Log.e(TAG, "Mood selection layout not found");
            return;
        }
        
        // Clear existing mood display
        moodLayout.removeAllViews();
        
        // Create an ImageView for the mood
        ImageView moodImageView = new ImageView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                dpToPx(183)); // Set height to 183dp
        moodImageView.setLayoutParams(layoutParams);
        moodImageView.setAdjustViewBounds(true);
        moodImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        
        // Try to load the PNG image using the SVGUtils
        // This will also apply the border and rounded corners
        boolean success = SVGUtils.loadMoodImage(this, moodImageView, mood);
        if (success) {
            // Add to layout
            moodLayout.addView(moodImageView);
            
            // Update the mood text
            moodText.setText("your current mood based on journal");
            return;
        }
        
        // Fallback to colored shapes if PNG loading fails
        int colorResourceId = 0;
        switch (mood) {
            case "Happy":
                colorResourceId = R.drawable.mood_happy;
                break;
            case "Excited":
                colorResourceId = R.drawable.mood_excited;
                break;
            case "Tired":
                colorResourceId = R.drawable.mood_tired;
                break;
            case "Stressed":
                colorResourceId = R.drawable.mood_stressed;
                break;
            case "Bored":
                colorResourceId = R.drawable.mood_bored;
                break;
            case "Neutral":
            default:
                colorResourceId = R.drawable.mood_neutral;
                break;
        }
        
        try {
            moodImageView.setImageResource(colorResourceId);
            moodLayout.addView(moodImageView);
            // Update the mood text
            moodText.setText("your current mood based on journal");
        } catch (Exception e) {
            Log.e(TAG, "Error loading mood color: " + e.getMessage());
            displayFallbackMood(moodLayout, mood);
        }
    }

    private void displayFallbackMood(LinearLayout moodLayout, String mood) {
        // Display fallback text for the mood
        TextView fallbackText = new TextView(this);
        fallbackText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        fallbackText.setGravity(android.view.Gravity.CENTER);
        fallbackText.setTextSize(24);
        fallbackText.setText("Current mood: " + mood);
        fallbackText.setTextColor(getResources().getColor(android.R.color.white));
        
        moodLayout.addView(fallbackText);
    }

    // Convert dp to pixels
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * Loads and displays a daily motivational quote
     * Uses a predefined list of quotes and displays a different one based on the day of year
     */
    private void loadDailyQuote() {
        // List of inspirational quotes
        String[] quotes = {
            "'Success is not final, failure is not fatal: it is the courage to continue that counts.' - Winston Churchill",
            "'Believe you can and you're halfway there.' - Theodore Roosevelt",
            "'The only way to do great work is to love what you do.' - Steve Jobs",
            "'It does not matter how slowly you go as long as you do not stop.' - Confucius",
            "'Everything you've ever wanted is on the other side of fear.' - George Addair",
            "'The future belongs to those who believe in the beauty of their dreams.' - Eleanor Roosevelt",
            "'You are never too old to set another goal or to dream a new dream.' - C.S. Lewis",
            "'The secret of getting ahead is getting started.' - Mark Twain",
            "'Don't watch the clock; do what it does. Keep going.' - Sam Levenson",
            "'Quality is not an act, it is a habit.' - Aristotle",
            "'The only person you are destined to become is the person you decide to be.' - Ralph Waldo Emerson",
            "'Your time is limited, don't waste it living someone else's life.' - Steve Jobs",
            "'Life is 10% what happens to us and 90% how we react to it.' - Charles R. Swindoll",
            "'Strive not to be a success, but rather to be of value.' - Albert Einstein",
            "'The mind is everything. What you think you become.' - Buddha"
        };
        
        // Get the day of year to select a quote
        Calendar calendar = Calendar.getInstance();
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        
        // Select a quote based on the day of year
        int quoteIndex = dayOfYear % quotes.length;
        String dailyQuote = quotes[quoteIndex];
        
        // Set the quote text
        dailyQuoteText.setText(dailyQuote);
    }
} 