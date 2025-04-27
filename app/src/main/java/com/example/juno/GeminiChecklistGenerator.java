package com.example.juno;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Helper class to generate detailed checklists from task descriptions using Google's Gemini API
 */
public class GeminiChecklistGenerator {
    private static final String TAG = "GeminiChecklistGen";
    private static final String API_KEY = "AIzaSyDERj-B3a6NSI6qOJ7GbhBIinf0gWsZmR8"; // Same API key used in other Gemini integrations
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-pro:generateContent?key=" + API_KEY;
    
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    // Configure OkHttp client with increased timeouts
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    /**
     * Generates a list of 5 subtasks for a given task
     * This must be called from a background thread, not the main thread
     * 
     * @param taskTitle The task title to break down into subtasks
     * @param taskDescription Optional task description for additional context
     * @return A list of 5 subtask titles
     */
    public static List<String> generateSubtasks(String taskTitle, String taskDescription) {
        if (taskTitle == null || taskTitle.trim().isEmpty()) {
            Log.e(TAG, "Task title is empty, cannot generate checklist");
            return generateFallbackSubtasks();
        }
        
        Log.d(TAG, "Starting checklist generation with Gemini API for task: " + taskTitle);
        
        try {
            String prompt = buildPrompt(taskTitle, taskDescription);
            String response = callGeminiAPI(prompt);
            List<String> subtasks = parseSubtasksFromResponse(response);
            
            // If we couldn't parse exactly 5 subtasks, use fallback
            if (subtasks.size() != 5) {
                Log.w(TAG, "Generated " + subtasks.size() + " subtasks, expected 5. Using fallback.");
                return generateFallbackSubtasks();
            }
            
            Log.d(TAG, "Successfully generated 5 subtasks for: " + taskTitle);
            return subtasks;
        } catch (Exception e) {
            Log.e(TAG, "Error generating subtasks with Gemini: " + e.getMessage(), e);
            return generateFallbackSubtasks();
        }
    }
    
    /**
     * Generates subtasks asynchronously
     * This can be called from the main thread
     * 
     * @param taskTitle The task title to break down into subtasks
     * @param taskDescription Optional task description for additional context
     * @param callback Callback to receive the subtasks result
     */
    public static void generateSubtasksAsync(String taskTitle, String taskDescription, SubtasksCallback callback) {
        new Thread(() -> {
            List<String> subtasks = generateSubtasks(taskTitle, taskDescription);
            callback.onSubtasksGenerated(subtasks);
        }).start();
    }
    
    /**
     * Callback interface for async subtask generation
     */
    public interface SubtasksCallback {
        void onSubtasksGenerated(List<String> subtasks);
    }
    
    private static String buildPrompt(String taskTitle, String taskDescription) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Break down the following task into EXACTLY 5 subtasks/steps to complete it. ");
        prompt.append("Each subtask should be clear, actionable, and specific. ");
        prompt.append("The response MUST contain EXACTLY 5 numbered points (1-5), with one subtask per point. ");
        prompt.append("Output ONLY the numbered list without any introduction or additional text.\n\n");
        
        prompt.append("Task Title: \"").append(taskTitle).append("\"\n");
        
        if (taskDescription != null && !taskDescription.trim().isEmpty()) {
            prompt.append("Task Description: \"").append(taskDescription).append("\"\n");
        }
        
        prompt.append("\nEXACTLY 5 subtasks:");
        
        String finalPrompt = prompt.toString();
        Log.d(TAG, "Generated Gemini prompt: " + finalPrompt);
        return finalPrompt;
    }
    
    private static String callGeminiAPI(String prompt) throws IOException, JSONException {
        // Create JSON request payload
        String jsonBody = String.format(
            "{\"contents\":[{\"parts\":[{\"text\":\"%s\"}]}]," +
            "\"generationConfig\":{\"temperature\":0.4,\"topK\":40,\"topP\":0.8,\"maxOutputTokens\":1024}}",
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
    
    private static List<String> parseSubtasksFromResponse(String jsonResponse) {
        List<String> subtasks = new ArrayList<>();
        
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
                            String subtaskText = part.getString("text").trim();
                            Log.d(TAG, "Raw Gemini response text: " + subtaskText);
                            
                            // Parse the numbered list (1. Task 1, 2. Task 2, etc.)
                            subtasks = parseNumberedList(subtaskText);
                        }
                    }
                }
            }
            
            return subtasks;
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing Gemini API response: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    private static List<String> parseNumberedList(String text) {
        List<String> items = new ArrayList<>();
        
        // Pattern to match numbered items (1. Item text)
        Pattern pattern = Pattern.compile("\\d+\\.\\s*(.+)(?:\\n|$)");
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            String item = matcher.group(1).trim();
            items.add(item);
        }
        
        // If pattern failed to capture exactly 5 items, try an alternative approach
        if (items.size() != 5) {
            Log.d(TAG, "Pattern matching found " + items.size() + " items instead of 5, trying alternative parsing");
            items.clear();
            
            // Split by lines and clean them up
            String[] lines = text.split("\\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // Remove numbering if present
                line = line.replaceAll("^\\d+\\.\\s*", "");
                items.add(line);
            }
            
            // Limit to 5 items if we got more
            if (items.size() > 5) {
                items = items.subList(0, 5);
            }
        }
        
        return items;
    }
    
    private static List<String> generateFallbackSubtasks() {
        // Generate generic subtasks when API call fails
        List<String> fallbackSubtasks = new ArrayList<>();
        fallbackSubtasks.add("Research and gather information");
        fallbackSubtasks.add("Create an outline or plan");
        fallbackSubtasks.add("Execute the first part of the task");
        fallbackSubtasks.add("Complete the remaining work");
        fallbackSubtasks.add("Review and finalize");
        
        return fallbackSubtasks;
    }
} 