package com.example.juno.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.style.LineBackgroundSpan;
import android.util.TypedValue;

import com.example.juno.R;

/**
 * A decorator for calendar days that shows priority colors as dots under the date number
 */
public class CalendarDayDecorator {

    public static class PriorityDotSpan implements LineBackgroundSpan {
        private final int color;
        private final float radius;
        private final Context context;

        public PriorityDotSpan(Context context, int color) {
            this.context = context;
            this.color = color;
            // Convert 3dp to pixels
            this.radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3,
                    context.getResources().getDisplayMetrics());
        }

        @Override
        public void drawBackground(Canvas canvas, Paint paint, int left, int right, int top, int baseline, int bottom,
                                  CharSequence text, int start, int end, int lnum) {
            // Save the original paint color
            int originalColor = paint.getColor();
            
            // Set the dot color and style
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
            
            // Calculate position for the dot (centered horizontally, below text)
            float xPosition = (left + right) / 2;
            float yPosition = bottom + radius * 2; // Bottom of text + some padding
            
            // Draw the dot
            canvas.drawCircle(xPosition, yPosition, radius, paint);
            
            // Restore original paint color
            paint.setColor(originalColor);
        }
    }

    /**
     * Get priority color based on the highest priority task for a date
     * @param context Application context
     * @param highPriority If there's a high priority task
     * @param mediumPriority If there's a medium priority task
     * @param lowPriority If there's a low priority task
     * @return The appropriate priority color
     */
    public static int getPriorityColor(Context context, boolean highPriority, boolean mediumPriority, boolean lowPriority) {
        if (highPriority) {
            return context.getResources().getColor(R.color.high_priority);
        } else if (mediumPriority) {
            return context.getResources().getColor(R.color.medium_priority);
        } else if (lowPriority) {
            return context.getResources().getColor(R.color.low_priority);
        }
        
        // Default color for tasks without priority
        return context.getResources().getColor(R.color.primary);
    }
} 