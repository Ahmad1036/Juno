package com.example.juno;

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
 * Helper class to generate concise task titles from descriptions using Google's Gemini API
 */
public class GeminiTaskTitleGenerator {
    private static final String TAG = "GeminiTaskTitleGen";
    private static final String API_KEY = "AIzaSyDERj-B3a6NSI6qOJ7GbhBIinf0gWsZmR8"; // Same key used in GeminiMoodAnalyzer
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-pro:generateContent?key=" + API_KEY;
    
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    /**
     * Generates a concise task title from a description
     * This must be called from a background thread, not the main thread
     * 
     * @param taskDescription The task description to generate a title from
     * @return A concise title for the task
     */
    public static String generateTitle(String taskDescription) {
        if (taskDescription == null || taskDescription.trim().isEmpty()) {
            Log.d(TAG, "Empty task description, returning generic title");
            return "New Task";
        }
        
        Log.d(TAG, "Starting title generation with Gemini API for description with length: " + taskDescription.length());
        
        try {
            String prompt = buildPrompt(taskDescription);
            String response = callGeminiAPI(prompt);
            String title = parseTitleFromResponse(response);
            
            if (title.isEmpty()) {
                Log.d(TAG, "Failed to generate title, returning first part of description");
                // Use first ~30 chars of description as fallback
                title = taskDescription.length() <= 30 ? 
                      taskDescription : 
                      taskDescription.substring(0, 27) + "...";
            }
            
            Log.d(TAG, "Generated title: " + title);
            return title;
        } catch (Exception e) {
            Log.e(TAG, "Error generating title with Gemini: " + e.getMessage(), e);
            // Use first ~30 chars of description as fallback
            String title = taskDescription.length() <= 30 ? 
                  taskDescription : 
                  taskDescription.substring(0, 27) + "...";
            return title;
        }
    }
    
    /**
     * Generates a task title asynchronously
     * This can be called from the main thread
     * 
     * @param taskDescription The task description to generate a title from
     * @param callback Callback to receive the title result
     */
    public static void generateTitleAsync(String taskDescription, TitleCallback callback) {
        if (taskDescription == null || taskDescription.trim().isEmpty()) {
            callback.onTitleGenerated("New Task");
            return;
        }
        
        new Thread(() -> {
            String title = generateTitle(taskDescription);
            callback.onTitleGenerated(title);
        }).start();
    }
    
    /**
     * Interface for receiving title generation results
     */
    public interface TitleCallback {
        void onTitleGenerated(String title);
    }
    
    private static String buildPrompt(String taskDescription) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a concise and clear task title based on the following task description. ");
        prompt.append("The title should be no more than 5-8 words, capturing the essence of the task. ");
        prompt.append("Make it direct and actionable. Your response should only include the title text.\n\n");
        prompt.append("Task description: \"").append(taskDescription).append("\"\n\n");
        prompt.append("Title: ");
        
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
    
    private static String parseTitleFromResponse(String jsonResponse) {
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
                            String titleText = part.getString("text").trim();
                            Log.d(TAG, "Extracted title text from response: '" + titleText + "'");
                            return titleText;
                        }
                    }
                }
            }
            
            // Return empty string if we couldn't extract a title
            Log.d(TAG, "Could not find title in response");
            return "";
            
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing Gemini API response: " + e.getMessage(), e);
            return "";
        }
    }
} 