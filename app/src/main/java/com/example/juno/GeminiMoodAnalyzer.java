package com.example.juno;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Helper class to analyze mood from journal text using Google's Gemini API
 */
public class GeminiMoodAnalyzer {
    private static final String TAG = "GeminiMoodAnalyzer";
    private static final String API_KEY = "AIzaSyDERj-B3a6NSI6qOJ7GbhBIinf0gWsZmR8";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-pro:generateContent?key=" + API_KEY;
    
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    // List of possible moods that our app supports
    private static final String[] SUPPORTED_MOODS = {
        "Happy", "Excited", "Tired", "Stressed", "Bored", "Neutral"
    };

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    /**
     * Analyzes journal text and returns a mood
     * This must be called from a background thread, not the main thread
     * 
     * @param journalText The journal text to analyze
     * @return One of the supported moods or "Neutral" if analysis fails
     */
    public static String analyzeMood(String journalText) {
        if (journalText == null || journalText.trim().isEmpty()) {
            Log.d(TAG, "Empty journal text, returning Neutral mood");
            return "Neutral";
        }
        
        Log.d(TAG, "Starting mood analysis with Gemini API for journal text with length: " + journalText.length());
        
        try {
            String prompt = buildPrompt(journalText);
            String response = callGeminiAPI(prompt);
            String mood = parseMoodFromResponse(response);
            
            Log.d(TAG, "Completed mood analysis: " + mood);
            return mood;
        } catch (Exception e) {
            Log.e(TAG, "Error analyzing mood with Gemini: " + e.getMessage(), e);
            return "Neutral"; // Fallback to neutral mood on error
        }
    }
    
    /**
     * Analyzes journal text and returns a mood asynchronously
     * This can be called from the main thread
     * 
     * @param journalText The journal text to analyze
     * @param callback Callback to receive the mood result
     */
    public static void analyzeMoodAsync(String journalText, MoodCallback callback) {
        if (journalText == null || journalText.trim().isEmpty()) {
            callback.onMoodAnalyzed("Neutral");
            return;
        }
        
        new Thread(() -> {
            String mood = analyzeMood(journalText);
            callback.onMoodAnalyzed(mood);
        }).start();
    }
    
    /**
     * Interface for receiving mood analysis results
     */
    public interface MoodCallback {
        void onMoodAnalyzed(String mood);
    }
    
    private static String buildPrompt(String journalText) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Based on the following journal entry, determine the writer's mood.\n\n");
        prompt.append("Journal entry: \"").append(journalText).append("\"\n\n");
        prompt.append("Select ONLY ONE of the following moods: ");
        for (int i = 0; i < SUPPORTED_MOODS.length; i++) {
            prompt.append(SUPPORTED_MOODS[i]);
            if (i < SUPPORTED_MOODS.length - 1) {
                prompt.append(", ");
            }
        }
        prompt.append(".\n\n");
        prompt.append("IMPORTANT: Your response should ONLY contain the mood name and nothing else. For example, if the mood is Happy, just respond with: Happy");
        
        String finalPrompt = prompt.toString();
        Log.d(TAG, "Generated Gemini prompt: " + finalPrompt);
        return finalPrompt;
    }
    
    private static String callGeminiAPI(String prompt) throws IOException, JSONException {
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
        } catch (Exception e) {
            Log.e(TAG, "Error calling Gemini API: " + e.getMessage(), e);
            throw e;
        }
    }
    
    private static String parseMoodFromResponse(String jsonResponse) {
        try {
            // Log the full response for debugging
            Log.d(TAG, "Raw Gemini API response: " + jsonResponse);
            
            JSONObject jsonObject = new JSONObject(jsonResponse);
            
            if (jsonObject.has("candidates") && jsonObject.getJSONArray("candidates").length() > 0) {
                JSONObject candidate = jsonObject.getJSONArray("candidates").getJSONObject(0);
                
                if (candidate.has("content")) {
                    JSONObject content = candidate.getJSONObject("content");
                    
                    if (content.has("parts") && content.getJSONArray("parts").length() > 0) {
                        JSONObject part = content.getJSONArray("parts").getJSONObject(0);
                        
                        if (part.has("text")) {
                            String moodText = part.getString("text").trim();
                            Log.d(TAG, "Extracted mood text from response: '" + moodText + "'");
                            
                            // Check if the response contains any of our supported moods
                            for (String mood : SUPPORTED_MOODS) {
                                if (moodText.contains(mood)) {
                                    Log.d(TAG, "Detected mood: " + mood);
                                    return mood;
                                }
                            }
                        }
                    }
                }
            }
            
            // Default to Neutral if we couldn't extract a mood
            Log.d(TAG, "Could not find a supported mood in response, defaulting to Neutral");
            return "Neutral";
            
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing Gemini API response: " + e.getMessage(), e);
            return "Neutral";
        }
    }
} 