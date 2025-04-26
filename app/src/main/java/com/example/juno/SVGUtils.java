package com.example.juno;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.Log;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

/**
 * Helper class for handling mood PNG images
 */
public class SVGUtils {
    private static final String TAG = "SVGUtils";

    /**
     * Load a mood PNG image to an ImageView
     * 
     * @param context The context
     * @param imageView The ImageView to set the image to
     * @param mood The mood name (Happy, Excited, Tired, Stressed, Bored, Neutral)
     * @return True if successful, false otherwise
     */
    public static boolean loadMoodImage(Context context, ImageView imageView, String mood) {
        try {
            // Get the resource ID for the mood PNG
            int resourceId = getMoodResourceId(context, mood);
            if (resourceId != 0) {
                // Set the background to the border shape
                imageView.setBackgroundResource(R.drawable.mood_image_border);
                // Set padding to ensure image doesn't overlap border
                int paddingPx = dpToPx(context, 1);
                imageView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
                
                // Set the image resource
                imageView.setImageResource(resourceId);
                return true;
            } else {
                Log.e(TAG, "No resource found for mood: " + mood);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading mood image: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get the resource ID for a mood PNG
     * 
     * @param context The context
     * @param mood The mood name (Happy, Excited, Tired, Stressed, Bored, Neutral)
     * @return The drawable resource ID, or 0 if not found
     */
    public static int getMoodResourceId(Context context, String mood) {
        String moodLower = mood.toLowerCase();
        return context.getResources().getIdentifier(
                moodLower, "drawable", context.getPackageName());
    }
    
    /**
     * Apply rounded corners to the ImageView
     * 
     * @param imageView The ImageView to set rounded corners on
     * @param cornerRadius The corner radius in pixels
     */
    public static void applyRoundedCorners(ImageView imageView, int cornerRadius) {
        imageView.setClipToOutline(true);
        imageView.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(android.view.View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
            }
        });
    }
    
    /**
     * Convert dp to pixels
     * 
     * @param context The context
     * @param dp The dp value to convert
     * @return The equivalent pixel value
     */
    private static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
} 