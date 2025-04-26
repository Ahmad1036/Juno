package com.example.juno;

import android.os.Bundle;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Configure edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Get reference to the text views
        TextView titleText = findViewById(R.id.title);
        TextView subtitleText = findViewById(R.id.subtitle);
        TextView contentText = findViewById(R.id.content_placeholder);
        
        // Set initial alpha to 0 (invisible)
        titleText.setAlpha(0f);
        subtitleText.setAlpha(0f);
        contentText.setAlpha(0f);
        
        // Create fade-in animations with Nothing UI style (minimal, clean)
        titleText.animate()
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(300)
                .start();
                
        subtitleText.animate()
                .alpha(1f)
                .setDuration(600)
                .setStartDelay(700)
                .start();
                
        contentText.animate()
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(1000)
                .start();
    }
}