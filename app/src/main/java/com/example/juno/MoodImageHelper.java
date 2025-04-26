package com.example.juno;

import android.util.Log;

import androidx.annotation.Nullable;

/**
 * Helper class for detecting mood from journal content
 */
public class MoodImageHelper {
    private static final String TAG = "MoodImageHelper";
    
    /**
     * Get a mood name based on journal content
     * 
     * @param content The journal content text
     * @return A mood name (Happy, Excited, Tired, Stressed, Bored, or Neutral)
     */
    @Nullable
    public static String detectMoodFromJournal(String content) {
        if (content == null || content.isEmpty()) {
            return "Neutral";
        }
        
        content = content.toLowerCase();
        
        // Check for happy/positive keywords
        if (content.contains("happy") || content.contains("excited") || 
            content.contains("great") || content.contains("amazing") ||
            content.contains("fantastic") || content.contains("wonderful") ||
            content.contains("joy") || content.contains("love")) {
            
            // Differentiate between excited and just happy
            if (content.contains("excited") || content.contains("thrilled") ||
                content.contains("can't wait") || content.contains("looking forward")) {
                return "Excited";
            }
            return "Happy";
        }
        
        // Check for tired/exhausted keywords
        if (content.contains("tired") || content.contains("exhausted") || 
            content.contains("sleepy") || content.contains("fatigue") ||
            content.contains("no energy") || content.contains("need rest") ||
            content.contains("need sleep")) {
            return "Tired";
        }
        
        // Check for stressed keywords
        if (content.contains("stress") || content.contains("anxiety") || 
            content.contains("worried") || content.contains("nervous") ||
            content.contains("overwhelm") || content.contains("pressure") ||
            content.contains("deadline")) {
            return "Stressed";
        }
        
        // Check for bored keywords
        if (content.contains("bored") || content.contains("boring") || 
            content.contains("nothing to do") || content.contains("wasting time") ||
            content.contains("uninteresting") || content.contains("monotonous")) {
            return "Bored";
        }
        
        // Default to Neutral if no clear mood is detected
        return "Neutral";
    }
} 