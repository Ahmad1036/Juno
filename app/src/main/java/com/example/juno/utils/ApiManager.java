package com.example.juno.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manager for API calls with offline support
 */
public class ApiManager {
    private static final String TAG = "ApiManager";
    
    // SharedPreferences keys for API responses
    private static final String API_RESPONSE_PREFIX = "api_response_";
    private static final String PENDING_API_REQUESTS = "pending_api_requests";
    
    private static ApiManager instance;
    private final RequestQueue requestQueue;
    private final DataManager dataManager;
    private final Context context;
    
    private ApiManager(Context context) {
        this.context = context.getApplicationContext();
        this.requestQueue = Volley.newRequestQueue(context);
        this.dataManager = DataManager.getInstance(context);
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized ApiManager getInstance(Context context) {
        if (instance == null) {
            instance = new ApiManager(context);
        }
        return instance;
    }
    
    /**
     * Make a GET request with offline support
     * @param url The API URL
     * @param cacheKey A unique key to cache the response
     * @param callback Callback for response handling
     */
    public void get(String url, String cacheKey, ApiCallback callback) {
        // If no network available, try to load from cache first
        if (!dataManager.isNetworkAvailable()) {
            String cachedResponse = dataManager.getData(API_RESPONSE_PREFIX + cacheKey, String.class);
            if (cachedResponse != null) {
                try {
                    JSONObject jsonResponse = new JSONObject(cachedResponse);
                    Toast.makeText(context, "Showing cached data (offline)", Toast.LENGTH_SHORT).show();
                    callback.onSuccess(jsonResponse);
                    return;
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing cached response: " + e.getMessage());
                }
            }
            Toast.makeText(context, "No cached data available (offline)", Toast.LENGTH_SHORT).show();
            callback.onError(new VolleyError("No network connection and no cached data available"));
            return;
        }
        
        // Make the actual request
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    // Cache the response
                    dataManager.saveData(API_RESPONSE_PREFIX + cacheKey, response.toString());
                    callback.onSuccess(response);
                },
                error -> {
                    // Try to get cached response on error
                    String cachedResponse = dataManager.getData(API_RESPONSE_PREFIX + cacheKey, String.class);
                    if (cachedResponse != null) {
                        try {
                            JSONObject jsonResponse = new JSONObject(cachedResponse);
                            Toast.makeText(context, "Network error - showing cached data", Toast.LENGTH_SHORT).show();
                            callback.onSuccess(jsonResponse);
                        } catch (Exception e) {
                            callback.onError(error);
                        }
                    } else {
                        callback.onError(error);
                    }
                }
        );
        
        // Set reasonable timeout
        request.setRetryPolicy(new DefaultRetryPolicy(
                15000,  // 15 seconds timeout
                1,      // Max retries
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        
        requestQueue.add(request);
    }
    
    /**
     * Make a POST request with offline support
     * @param url The API URL
     * @param requestBody Request body
     * @param callback Callback for response handling
     */
    public void post(String url, JSONObject requestBody, ApiCallback callback) {
        // If offline, save the request for later and return
        if (!dataManager.isNetworkAvailable()) {
            String requestId = UUID.randomUUID().toString();
            PendingApiRequest pendingRequest = new PendingApiRequest(
                    requestId,
                    url,
                    Request.Method.POST,
                    requestBody.toString()
            );
            
            // Store in pending requests
            savePendingRequest(pendingRequest);
            
            Toast.makeText(context, "Request saved for later (offline)", Toast.LENGTH_SHORT).show();
            callback.onOfflineRequestSaved();
            return;
        }
        
        // Make the actual request
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                requestBody,
                response -> callback.onSuccess(response),
                error -> callback.onError(error)
        );
        
        // Set reasonable timeout
        request.setRetryPolicy(new DefaultRetryPolicy(
                15000,  // 15 seconds timeout
                1,      // Max retries
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        
        requestQueue.add(request);
    }
    
    /**
     * Save pending request to DataManager
     */
    private void savePendingRequest(PendingApiRequest pendingRequest) {
        Map<String, PendingApiRequest> pendingRequests = getPendingRequests();
        pendingRequests.put(pendingRequest.getId(), pendingRequest);
        dataManager.saveData(PENDING_API_REQUESTS, pendingRequests);
        Log.d(TAG, "Saved pending API request for later: " + pendingRequest.getUrl());
    }
    
    /**
     * Get all pending requests
     */
    private Map<String, PendingApiRequest> getPendingRequests() {
        Map<String, PendingApiRequest> pendingRequests = dataManager.getData(
                PENDING_API_REQUESTS,
                HashMap.class
        );
        
        if (pendingRequests == null) {
            return new HashMap<>();
        }
        
        return pendingRequests;
    }
    
    /**
     * Sync all pending API requests
     */
    public void syncPendingRequests() {
        if (!dataManager.isNetworkAvailable()) {
            return;
        }
        
        Map<String, PendingApiRequest> pendingRequests = getPendingRequests();
        if (pendingRequests.isEmpty()) {
            return;
        }
        
        Log.d(TAG, "Syncing " + pendingRequests.size() + " pending API requests");
        Map<String, PendingApiRequest> remainingRequests = new HashMap<>();
        
        for (PendingApiRequest request : pendingRequests.values()) {
            try {
                switch (request.getMethod()) {
                    case Request.Method.POST:
                        executePendingPostRequest(request);
                        break;
                    case Request.Method.PUT:
                        executePendingPutRequest(request);
                        break;
                    case Request.Method.DELETE:
                        executePendingDeleteRequest(request);
                        break;
                    default:
                        // Skip unsupported methods
                        remainingRequests.put(request.getId(), request);
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error executing pending request: " + e.getMessage());
                remainingRequests.put(request.getId(), request);
            }
        }
        
        // Save any requests that failed to execute
        if (!remainingRequests.isEmpty()) {
            dataManager.saveData(PENDING_API_REQUESTS, remainingRequests);
        } else {
            dataManager.saveData(PENDING_API_REQUESTS, new HashMap<>());
        }
    }
    
    /**
     * Execute a pending POST request
     */
    private void executePendingPostRequest(PendingApiRequest pendingRequest) throws Exception {
        JSONObject requestBody = new JSONObject(pendingRequest.getBody());
        
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                pendingRequest.getUrl(),
                requestBody,
                response -> Log.d(TAG, "Successfully executed pending POST request to: " + pendingRequest.getUrl()),
                error -> Log.e(TAG, "Error executing pending POST request: " + error.getMessage())
        );
        
        requestQueue.add(request);
    }
    
    /**
     * Execute a pending PUT request
     */
    private void executePendingPutRequest(PendingApiRequest pendingRequest) throws Exception {
        JSONObject requestBody = new JSONObject(pendingRequest.getBody());
        
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT,
                pendingRequest.getUrl(),
                requestBody,
                response -> Log.d(TAG, "Successfully executed pending PUT request to: " + pendingRequest.getUrl()),
                error -> Log.e(TAG, "Error executing pending PUT request: " + error.getMessage())
        );
        
        requestQueue.add(request);
    }
    
    /**
     * Execute a pending DELETE request
     */
    private void executePendingDeleteRequest(PendingApiRequest pendingRequest) {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.DELETE,
                pendingRequest.getUrl(),
                null,
                response -> Log.d(TAG, "Successfully executed pending DELETE request to: " + pendingRequest.getUrl()),
                error -> Log.e(TAG, "Error executing pending DELETE request: " + error.getMessage())
        );
        
        requestQueue.add(request);
    }
    
    /**
     * Callback interface for API responses
     */
    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(VolleyError error);
        default void onOfflineRequestSaved() {}
    }
    
    /**
     * Class to represent a pending API request
     */
    private static class PendingApiRequest {
        private final String id;
        private final String url;
        private final int method;
        private final String body;
        
        public PendingApiRequest(String id, String url, int method, String body) {
            this.id = id;
            this.url = url;
            this.method = method;
            this.body = body;
        }
        
        public String getId() {
            return id;
        }
        
        public String getUrl() {
            return url;
        }
        
        public int getMethod() {
            return method;
        }
        
        public String getBody() {
            return body;
        }
    }
} 