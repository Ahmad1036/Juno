package com.example.juno.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Utility for monitoring network connectivity changes
 */
public class NetworkManager {
    private static final String TAG = "NetworkManager";
    
    private final Context context;
    private final ConnectivityManager connectivityManager;
    private final MutableLiveData<Boolean> isNetworkAvailableLiveData = new MutableLiveData<>();
    
    private ConnectivityManager.NetworkCallback networkCallback;
    private BroadcastReceiver networkReceiver;
    
    private NetworkChangeListener networkChangeListener;
    
    public interface NetworkChangeListener {
        void onNetworkAvailable();
        void onNetworkUnavailable();
    }
    
    public NetworkManager(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        // Set initial state
        isNetworkAvailableLiveData.setValue(isNetworkAvailable());
        
        // Initialize network monitoring based on API level
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            setupNetworkCallbackForNewerAPIs();
        } else {
            setupBroadcastReceiverForOlderAPIs();
        }
    }
    
    public void setNetworkChangeListener(NetworkChangeListener listener) {
        this.networkChangeListener = listener;
    }
    
    /**
     * Check if network is available
     */
    public boolean isNetworkAvailable() {
        if (connectivityManager == null) {
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }
    
    /**
     * Get network state as LiveData
     */
    public LiveData<Boolean> getNetworkAvailability() {
        return isNetworkAvailableLiveData;
    }
    
    /**
     * Register network callbacks (for Android N and above)
     */
    private void setupNetworkCallbackForNewerAPIs() {
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Log.d(TAG, "Network available");
                isNetworkAvailableLiveData.postValue(true);
                if (networkChangeListener != null) {
                    networkChangeListener.onNetworkAvailable();
                }
            }
            
            @Override
            public void onLost(@NonNull Network network) {
                Log.d(TAG, "Network lost");
                isNetworkAvailableLiveData.postValue(false);
                if (networkChangeListener != null) {
                    networkChangeListener.onNetworkUnavailable();
                }
            }
        };
        
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }
    
    /**
     * Register broadcast receiver (for older Android versions)
     */
    private void setupBroadcastReceiverForOlderAPIs() {
        networkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean isNetworkAvailable = isNetworkAvailable();
                isNetworkAvailableLiveData.postValue(isNetworkAvailable);
                
                if (networkChangeListener != null) {
                    if (isNetworkAvailable) {
                        networkChangeListener.onNetworkAvailable();
                    } else {
                        networkChangeListener.onNetworkUnavailable();
                    }
                }
            }
        };
        
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(networkReceiver, filter);
    }
    
    /**
     * Unregister network callbacks
     */
    public void unregisterNetworkCallbacks() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } else if (networkReceiver != null) {
                context.unregisterReceiver(networkReceiver);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering network callbacks: " + e.getMessage());
        }
    }
} 