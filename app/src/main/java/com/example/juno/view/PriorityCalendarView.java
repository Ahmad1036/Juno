package com.example.juno.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.juno.R;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * A custom CalendarView that can highlight dates based on task priorities
 */
public class PriorityCalendarView extends CalendarView {

    private static final String TAG = "PriorityCalendarView";
    
    // Map to store date priorities: key = year+month+day, value = priority color
    private Map<String, Integer> datePriorityColors = new HashMap<>();
    private Context mContext;
    
    // Paint for drawing indicators
    private Paint indicatorPaint;

    public PriorityCalendarView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public PriorityCalendarView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PriorityCalendarView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        
        // Initialize paint for indicators
        indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        indicatorPaint.setStyle(Paint.Style.FILL);
        
        // Set up basic CalendarView properties
        setFirstDayOfWeek(Calendar.MONDAY);
        
        // Set up enhanced date change listener
        setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // Log selections to help with debugging
            Log.d(TAG, "Date selected: " + year + "-" + month + "-" + dayOfMonth);
            
            // The default behavior will handle selection
        });
        
        // Initial drawing of indicators
        post(() -> {
            tryApplyDateIndicators();
        });
        
        // Watch for layout changes to reapply indicators
        addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            // When layout changes, try to apply indicators again
            post(() -> {
                tryApplyDateIndicators();
            });
        });
    }

    /**
     * Add a priority marker to a specific date
     * 
     * @param year Year of the date
     * @param month Month of the date (0-11)
     * @param day Day of the month
     * @param priorityColor Color to use for the priority indicator
     */
    public void addPriorityToDate(int year, int month, int day, int priorityColor) {
        String dateKey = formatDateKey(year, month, day);
        datePriorityColors.put(dateKey, priorityColor);
        
        // Log for debugging
        Log.d(TAG, "Added priority for date: " + dateKey + " with color: " + priorityColor);
        
        // Force refresh with a slight delay to ensure calendar is ready
        postDelayed(this::refreshCalendarDisplay, 50);
    }

    /**
     * Clears all priority markers
     */
    public void clearAllPriorities() {
        datePriorityColors.clear();
        refreshCalendarDisplay();
    }

    /**
     * Forces a refresh of the calendar display
     */
    private void refreshCalendarDisplay() {
        // Log refresh attempt
        Log.d(TAG, "Refreshing calendar display");
        
        // Standard view refreshing
        invalidate();
        requestLayout();
        
        // Start by trying to apply directly
        tryApplyDateIndicators();
        
        // Then use the date change trick as a backup
        // Store current date before manipulating
        long currentDate = getDate();
        
        // First jump forward a day
        setDate(currentDate + 86400000);
        
        // Then back to original date
        postDelayed(() -> {
            setDate(currentDate);
            
            // After restoring date, try one more time
            postDelayed(this::tryApplyDateIndicators, 100);
        }, 50);
    }
    
    /**
     * Generate a consistent date key format
     */
    private String formatDateKey(int year, int month, int day) {
        return String.format("%d-%d-%d", year, month, day);
    }
    
    /**
     * Check if a date has a priority indicator
     */
    public boolean dateHasPriority(int year, int month, int day) {
        String dateKey = formatDateKey(year, month, day);
        return datePriorityColors.containsKey(dateKey);
    }
    
    /**
     * Get priority color for a date
     */
    public int getDatePriorityColor(int year, int month, int day) {
        String dateKey = formatDateKey(year, month, day);
        Integer color = datePriorityColors.get(dateKey);
        return color != null ? color : Color.TRANSPARENT;
    }
    
    /**
     * Try to locate and modify date cells to show indicators
     */
    private void tryApplyDateIndicators() {
        Log.d(TAG, "Attempting to apply date indicators");
        
        // Start from this view and search for date cells
        findAndUpdateDateCells(this);
    }
    
    /**
     * Recursively search view hierarchy for date cells
     */
    private void findAndUpdateDateCells(View view) {
        // If this is a text view, check if it's a date cell
        if (view instanceof TextView) {
            tryUpdateDateCell((TextView) view);
        }
        
        // For view groups, search through children
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                findAndUpdateDateCells(group.getChildAt(i));
            }
        }
    }
    
    /**
     * Check if a TextView is a date cell and update it if needed
     */
    private void tryUpdateDateCell(TextView textView) {
        // Date cells typically contain just a number
        String text = textView.getText().toString();
        
        try {
            // Try to parse as a day number
            int day = Integer.parseInt(text);
            
            // If in range for a day of month
            if (day >= 1 && day <= 31) {
                // Get current month and year from calendar
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(getDate());
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH);
                
                // Check if this date has a priority
                String dateKey = formatDateKey(year, month, day);
                
                if (datePriorityColors.containsKey(dateKey)) {
                    // Get the priority color
                    int color = datePriorityColors.get(dateKey);
                    
                    // Create a custom dot drawable
                    ShapeDrawable dotDrawable = new ShapeDrawable(new OvalShape());
                    dotDrawable.getPaint().setColor(color);
                    
                    // Set size for the dot
                    int dotSizeInDp = 8;
                    int dotSizeInPx = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, dotSizeInDp, 
                            getResources().getDisplayMetrics());
                    
                    dotDrawable.setIntrinsicWidth(dotSizeInPx);
                    dotDrawable.setIntrinsicHeight(dotSizeInPx);
                    
                    // Apply the dot as a bottom compound drawable
                    textView.setCompoundDrawablePadding(8);
                    textView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, dotDrawable);
                    
                    // Log successful application
                    Log.d(TAG, "Applied indicator to date: " + dateKey);
                } else {
                    // Clear any existing indicators
                    textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                }
            }
        } catch (NumberFormatException e) {
            // Not a number, so not a date cell
        }
    }
} 