package com.example.juno.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.style.LineBackgroundSpan;

/**
 * A decorator that adds colored dots under dates in a calendar
 */
public class DotDecorator implements LineBackgroundSpan {
    private final int color;
    private final float radius;
    
    /**
     * Create a dot decorator with specified color and radius
     * 
     * @param color The color of the dot
     * @param radius The radius of the dot in pixels
     */
    public DotDecorator(int color, float radius) {
        this.color = color;
        this.radius = radius;
    }

    @Override
    public void drawBackground(Canvas canvas, Paint paint, int left, int right, int top, int baseline, int bottom,
                              CharSequence text, int start, int end, int lnum) {
        // Save the original paint color
        int originalColor = paint.getColor();
        float originalStrokeWidth = paint.getStrokeWidth();
        
        // Set the dot style
        paint.setColor(this.color);
        paint.setStyle(Paint.Style.FILL);
        
        // Calculate center position for the dot (centered horizontally, below text)
        float xPosition = (left + right) / 2f;
        float yPosition = bottom + radius * 2f; // Place dot below text with some spacing
        
        // Draw the dot
        canvas.drawCircle(xPosition, yPosition, radius, paint);
        
        // Restore original paint properties
        paint.setColor(originalColor);
        paint.setStrokeWidth(originalStrokeWidth);
    }
} 