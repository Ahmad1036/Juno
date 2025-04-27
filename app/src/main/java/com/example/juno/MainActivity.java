package com.example.juno;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.juno.utils.NotificationUtils;
import com.example.juno.utils.ThemeUtils;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Create notification channel
        NotificationUtils.createNotificationChannel(this);
        
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
        
        // Apply font size settings
        ThemeUtils.applyFontSize(titleText, this);
        ThemeUtils.applyFontSize(subtitleText, this);
        ThemeUtils.applyFontSize(contentText, this);
        
        // Apply layout style settings to the main container
        ViewGroup mainContainer = findViewById(R.id.main);
        ThemeUtils.applyLayoutStyle(mainContainer, this);
        
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
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            // Open settings activity
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}