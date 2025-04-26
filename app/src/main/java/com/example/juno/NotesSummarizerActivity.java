package com.example.juno;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class NotesSummarizerActivity extends AppCompatActivity {

    private static final String TAG = "NotesSummarizerActivity";
    private static final int REQUEST_CODE_SPEECH_INPUT = 100;
    private static final String API_KEY = "AIzaSyADvfoNCVD3_Bh4xJ6MyInGNXSYl_PGhHc";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-pro:generateContent?key=" + API_KEY;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private EditText notesInput;
    private CardView summarizeButton;
    private TextView summaryTitle;
    private CardView summaryCard;
    private TextView summaryText;
    private ProgressBar loadingIndicator;
    private ImageButton backButton;
    private ImageButton voiceInputButton;

    // For background processing
    private ExecutorService executorService;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_summarizer);

        // Initialize UI components
        notesInput = findViewById(R.id.notes_input);
        summarizeButton = findViewById(R.id.summarize_button);
        summaryTitle = findViewById(R.id.summary_title);
        summaryCard = findViewById(R.id.summary_card);
        summaryText = findViewById(R.id.summary_text);
        loadingIndicator = findViewById(R.id.loading_indicator);
        backButton = findViewById(R.id.back_button);
        voiceInputButton = findViewById(R.id.voice_input_button);

        // Initialize executor service for background processing
        executorService = Executors.newSingleThreadExecutor();

        // Set up click listeners
        backButton.setOnClickListener(v -> finish());
        
        summarizeButton.setOnClickListener(v -> {
            String notes = notesInput.getText().toString().trim();
            if (notes.isEmpty()) {
                Toast.makeText(this, "Please enter some notes to summarize", Toast.LENGTH_SHORT).show();
                return;
            }
            
            summarizeNotes(notes);
        });
        
        voiceInputButton.setOnClickListener(v -> {
            startVoiceInput();
        });
    }
    
    /**
     * Starts voice input for speech recognition
     */
    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to add to your notes");
        
        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            Toast.makeText(this, "Your device doesn't support speech input", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error starting voice input: " + e.getMessage());
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String spokenText = result.get(0);
                
                // Append the spoken text to the existing text with a space or new line
                String currentText = notesInput.getText().toString();
                if (currentText.isEmpty()) {
                    notesInput.setText(spokenText);
                } else if (currentText.endsWith("\n")) {
                    notesInput.setText(currentText + spokenText);
                } else {
                    notesInput.setText(currentText + " " + spokenText);
                }
                
                // Move cursor to the end
                notesInput.setSelection(notesInput.getText().length());
            }
        }
    }

    private void summarizeNotes(String notes) {
        // Show loading indicator and hide summary
        loadingIndicator.setVisibility(View.VISIBLE);
        summaryTitle.setVisibility(View.GONE);
        summaryCard.setVisibility(View.GONE);
        
        // Run summarization in background thread
        executorService.execute(() -> {
            // Simulate AI processing time (1-2 seconds)
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // Generate the summary
            String summary = generateSummary(notes);
            
            // Update UI on the main thread
            runOnUiThread(() -> {
                loadingIndicator.setVisibility(View.GONE);
                summaryTitle.setVisibility(View.VISIBLE);
                summaryCard.setVisibility(View.VISIBLE);
                summaryText.setText(summary);
                
                // Log this summarization activity
                logSummarizationActivity(notes.length(), summary.length());
            });
        });
    }

    private String generateSummary(String notes) {
        try {
            // Create the prompt for Gemini API
            String prompt = "Summarize the following notes into concise bullet points. " +
                    "Extract the key points, action items, and important details. " +
                    "Format as bullet points with emoji where appropriate. " +
                    "If there are deadlines or important dates, highlight them. " +
                    "Keep the summary concise but comprehensive.\n\n" +
                    "Notes to summarize:\n" + notes;
            
            // Call Gemini API to generate summary
            String response = callGeminiAPI(prompt);
            String summary = parseGeminiResponse(response);
            
            // Return the summary or a default message if empty
            if (summary == null || summary.trim().isEmpty()) {
                Log.w(TAG, "Empty summary returned from Gemini, using fallback summarization");
                return generateFallbackSummary(notes);
            }
            
            return summary;
        } catch (Exception e) {
            Log.e(TAG, "Error generating summary with Gemini: " + e.getMessage(), e);
            // Fallback to simple summarization if API call fails
            return generateFallbackSummary(notes);
        }
    }
    
    private String callGeminiAPI(String prompt) throws IOException, JSONException {
        // Create JSON request payload
        String jsonBody = String.format(
            "{\"contents\":[{\"parts\":[{\"text\":\"%s\"}]}]}",
            prompt.replace("\"", "\\\"")  // Escape quotes to prevent JSON parsing errors
        );

        Log.d(TAG, "Gemini API request payload length: " + jsonBody.length());
        
        // Create request
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .header("Content-Type", "application/json")
                .build();
        
        Log.d(TAG, "Sending request to Gemini API");
        
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
        } catch (Exception e) {
            Log.e(TAG, "Error calling Gemini API: " + e.getMessage(), e);
            throw e;
        }
    }
    
    private String parseGeminiResponse(String jsonResponse) {
        try {
            // Parse the JSON response
            JSONObject jsonObject = new JSONObject(jsonResponse);
            
            if (jsonObject.has("candidates") && jsonObject.getJSONArray("candidates").length() > 0) {
                JSONObject candidate = jsonObject.getJSONArray("candidates").getJSONObject(0);
                
                if (candidate.has("content")) {
                    JSONObject content = candidate.getJSONObject("content");
                    
                    if (content.has("parts") && content.getJSONArray("parts").length() > 0) {
                        JSONObject part = content.getJSONArray("parts").getJSONObject(0);
                        
                        if (part.has("text")) {
                            String summaryText = part.getString("text").trim();
                            Log.d(TAG, "Extracted summary from response with length: " + summaryText.length());
                            return summaryText;
                        }
                    }
                }
            }
            
            // If we couldn't extract the summary
            Log.w(TAG, "Could not extract summary from Gemini response");
            return null;
            
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing Gemini API response: " + e.getMessage(), e);
            return null;
        }
    }
    
    private String generateFallbackSummary(String notes) {
        // This is a simple fallback summarization algorithm that extracts key sentences
        StringBuilder summary = new StringBuilder();
        
        // Split notes into sentences
        String[] sentences = notes.split("[.!?]\\s*");
        
        // Check if there are enough sentences to summarize
        if (sentences.length <= 3) {
            return notes; // Return original notes if too short
        }
        
        // Extract bullet points
        Pattern bulletPattern = Pattern.compile("^\\s*[•\\-*]\\s*(.+)$", Pattern.MULTILINE);
        Matcher bulletMatcher = bulletPattern.matcher(notes);
        
        boolean foundBullets = false;
        while (bulletMatcher.find()) {
            foundBullets = true;
            summary.append("• ").append(bulletMatcher.group(1).trim()).append("\n\n");
        }
        
        // If we found bullet points, use those as our summary
        if (foundBullets) {
            return summary.toString().trim();
        }
        
        // Otherwise, extract key sentences
        // Find sentences with key indicators like "important", "key", "must", etc.
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) continue;
            
            String lowerSentence = sentence.toLowerCase();
            
            if (lowerSentence.contains("important") || 
                lowerSentence.contains("key") || 
                lowerSentence.contains("must") || 
                lowerSentence.contains("should") || 
                lowerSentence.contains("need to") ||
                lowerSentence.contains("deadline") ||
                lowerSentence.contains("action") ||
                lowerSentence.contains("focus") ||
                lowerSentence.contains("priority")) {
                
                summary.append("• ").append(sentence).append(".\n\n");
            }
        }
        
        // If we didn't find any key sentences, just take the first and last sentences
        if (summary.length() == 0) {
            if (sentences.length > 0) {
                summary.append("• ").append(sentences[0].trim()).append(".\n\n");
            }
            
            // Add a middle sentence if available
            if (sentences.length > 2) {
                int middleIndex = sentences.length / 2;
                summary.append("• ").append(sentences[middleIndex].trim()).append(".\n\n");
            }
            
            // Add the last sentence
            if (sentences.length > 1) {
                summary.append("• ").append(sentences[sentences.length - 1].trim()).append(".\n\n");
            }
        }
        
        return summary.toString().trim();
    }
    
    private void logSummarizationActivity(int inputLength, int outputLength) {
        try {
            // Get user ID from session
            UserSessionManager sessionManager = new UserSessionManager(this);
            String userId = sessionManager.getUserId();
            
            if (userId != null && !userId.isEmpty()) {
                // Log activity to Firebase
                DatabaseReference userActivityRef = FirebaseDatabase.getInstance()
                        .getReference("user_activity")
                        .child(userId)
                        .child("summarization");
                
                // Create a unique key for this activity
                String activityId = userActivityRef.push().getKey();
                
                if (activityId != null) {
                    // Create activity data
                    SummarizationActivity activity = new SummarizationActivity(
                            System.currentTimeMillis(),
                            inputLength,
                            outputLength
                    );
                    
                    // Save activity
                    userActivityRef.child(activityId).setValue(activity);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown executor service
        if (executorService != null) {
            executorService.shutdown();
        }
    }
    
    // Data class for summarization activity
    private static class SummarizationActivity {
        public long timestamp;
        public int inputLength;
        public int outputLength;
        
        public SummarizationActivity() {
            // Required empty constructor for Firebase
        }
        
        public SummarizationActivity(long timestamp, int inputLength, int outputLength) {
            this.timestamp = timestamp;
            this.inputLength = inputLength;
            this.outputLength = outputLength;
        }
    }
} 