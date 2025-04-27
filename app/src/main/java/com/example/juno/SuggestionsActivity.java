package com.example.juno;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SuggestionsActivity extends AppCompatActivity {
    private static final String TAG = "SuggestionsActivity";
    private static final String API_KEY = "AIzaSyDERj-B3a6NSI6qOJ7GbhBIinf0gWsZmR8";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-pro:generateContent?key=" + API_KEY;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private TextView currentMoodText;
    private TextView moodSuggestionsText;
    private TextView taskStatusText;
    private TextView taskSuggestionsText;
    private TextView personalizedTipsText;
    private DatabaseReference mDatabase;
    private String userId;
    private String currentMood = "Neutral";
    private int completedTasksCount = 0;
    private int totalTasksCount = 0;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggestions);

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance("https://juno-aa7d2-default-rtdb.firebaseio.com/").getReference();

        // Initialize views
        currentMoodText = findViewById(R.id.current_mood_text);
        moodSuggestionsText = findViewById(R.id.mood_suggestions_text);
        taskStatusText = findViewById(R.id.task_status_text);
        taskSuggestionsText = findViewById(R.id.task_suggestions_text);
        personalizedTipsText = findViewById(R.id.personalized_tips_text);

        // Set up back button
        ImageButton backButton = findViewById(R.id.suggestions_back_button);
        backButton.setOnClickListener(v -> finish());

        // Get user ID from shared preferences
        SharedPreferences prefs = getSharedPreferences("JunoUserPrefs", MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load current mood and tasks
        loadCurrentMood();
        loadTaskStatistics();
    }

    private void loadCurrentMood() {
        currentMoodText.setText("current mood: loading...");

        mDatabase.child("journals")
                .orderByChild("timestamp")
                .limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot journalSnapshot : dataSnapshot.getChildren()) {
                            Journal journal = journalSnapshot.getValue(Journal.class);
                            if (journal != null && userId.equals(journal.getUserId())) {
                                currentMood = journal.getMood();
                                currentMoodText.setText("current mood: " + currentMood.toLowerCase());
                                generateMoodSuggestions(currentMood);
                                return;
                            }
                        }
                        currentMoodText.setText("current mood: neutral (default)");
                        generateMoodSuggestions("Neutral");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error loading current mood", databaseError.toException());
                        currentMoodText.setText("current mood: neutral (default)");
                        generateMoodSuggestions("Neutral");
                    }
                });
    }

    private void loadTaskStatistics() {
        taskStatusText.setText("analyzing your task progress...");

        mDatabase.child("users").child(userId).child("tasks")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        totalTasksCount = 0;
                        completedTasksCount = 0;
                        
                        for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                            Task task = taskSnapshot.getValue(Task.class);
                            if (task != null) {
                                totalTasksCount++;
                                if (task.isCompleted()) {
                                    completedTasksCount++;
                                }
                            }
                        }
                        
                        String statusMessage = completedTasksCount + " of " + totalTasksCount + " tasks completed";
                        taskStatusText.setText(statusMessage);
                        
                        generateTaskSuggestions(completedTasksCount, totalTasksCount);
                        generatePersonalizedTips(completedTasksCount, totalTasksCount);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error loading task statistics", databaseError.toException());
                        taskStatusText.setText("unable to load task statistics");
                    }
                });
    }

    private void generateMoodSuggestions(String mood) {
        moodSuggestionsText.setText("generating suggestions based on your mood...");

        String prompt = String.format(
            "Based on the user's current mood (%s), provide 3 personalized suggestions for activities or self-care. " +
            "The suggestions should be brief, practical, and tailored to help someone feeling %s. " +
            "Format as bullet points starting with emoji that match the suggestion.",
            mood, mood
        );

        new Thread(() -> {
            try {
                String response = callGeminiAPI(prompt);
                String suggestions = parseGeminiResponse(response);
                runOnUiThread(() -> moodSuggestionsText.setText(suggestions));
            } catch (Exception e) {
                Log.e(TAG, "Error generating mood suggestions", e);
                runOnUiThread(() -> moodSuggestionsText.setText("Unable to generate suggestions at this time."));
            }
        }).start();
    }

    private void generateTaskSuggestions(int completedTasks, int totalTasks) {
        taskSuggestionsText.setText("generating suggestions based on your task progress...");

        String prompt = String.format(
            "The user has completed %d out of %d tasks. " +
            "Provide 2-3 encouraging suggestions or tips based on this progress. " +
            "If they've completed many tasks, recognize their achievement. " +
            "If they haven't completed many, provide motivational tips. " +
            "Keep it brief and positive. Format as bullet points starting with emoji.",
            completedTasks, totalTasks
        );

        new Thread(() -> {
            try {
                String response = callGeminiAPI(prompt);
                String suggestions = parseGeminiResponse(response);
                runOnUiThread(() -> taskSuggestionsText.setText(suggestions));
            } catch (Exception e) {
                Log.e(TAG, "Error generating task suggestions", e);
                runOnUiThread(() -> taskSuggestionsText.setText("Unable to generate suggestions at this time."));
            }
        }).start();
    }

    private void generatePersonalizedTips(int completedTasks, int totalTasks) {
        personalizedTipsText.setText("generating personalized productivity tips...");

        float completionRate = totalTasks > 0 ? (float) completedTasks / totalTasks : 0;
        String productivity = completionRate >= 0.7 ? "high" : completionRate >= 0.3 ? "moderate" : "could be improved";

        String prompt = String.format(
            "Based on the user's task completion rate (%d/%d tasks, which is %s productivity), " +
            "provide 2-3 personalized time management and productivity tips. " +
            "Make suggestions relevant to someone with this level of productivity. " +
            "Keep the tips actionable, specific, and brief. Format as bullet points starting with emoji.",
            completedTasks, totalTasks, productivity
        );

        new Thread(() -> {
            try {
                String response = callGeminiAPI(prompt);
                String tips = parseGeminiResponse(response);
                runOnUiThread(() -> personalizedTipsText.setText(tips));
            } catch (Exception e) {
                Log.e(TAG, "Error generating personalized tips", e);
                runOnUiThread(() -> personalizedTipsText.setText("Unable to generate personalized tips at this time."));
            }
        }).start();
    }

    private String callGeminiAPI(String prompt) throws IOException, JSONException {
        // Create JSON request payload
        String jsonBody = String.format(
            "{\"contents\":[{\"parts\":[{\"text\":\"%s\"}]}]}",
            prompt.replace("\"", "\\\"")  // Escape quotes to prevent JSON parsing errors
        );

        Log.d(TAG, "Gemini API request payload: " + jsonBody);
        
        // Create request
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .header("Content-Type", "application/json")
                .build();
        
        Log.d(TAG, "Sending request to Gemini API: " + API_URL);
        
        // Execute request
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorMsg = "Unexpected response code: " + response.code();
                if (response.body() != null) {
                    errorMsg += ", body: " + response.body().string();
                }
                Log.e(TAG, errorMsg);
                throw new IOException(errorMsg);
            }
            
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                Log.e(TAG, "Empty response from Gemini API");
                throw new IOException("Empty response from Gemini API");
            }
            
            String responseString = responseBody.string();
            Log.d(TAG, "Received response from Gemini API with length: " + responseString.length());
            return responseString;
        }
    }

    private String parseGeminiResponse(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            
            if (jsonObject.has("candidates") && jsonObject.getJSONArray("candidates").length() > 0) {
                JSONObject candidate = jsonObject.getJSONArray("candidates").getJSONObject(0);
                
                if (candidate.has("content")) {
                    JSONObject content = candidate.getJSONObject("content");
                    
                    if (content.has("parts") && content.getJSONArray("parts").length() > 0) {
                        JSONObject part = content.getJSONArray("parts").getJSONObject(0);
                        
                        if (part.has("text")) {
                            return part.getString("text").trim();
                        }
                    }
                }
            }
            
            return "No valid suggestions found in the response.";
            
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing Gemini API response: " + e.getMessage(), e);
            return "Error parsing suggestions.";
        }
    }
} 