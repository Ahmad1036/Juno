package com.example.juno;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.juno.utils.BaseActivity;
import com.example.juno.utils.ReminderManager;

/**
 * Activity for configuring smart reminder settings
 */
public class ReminderSettingsActivity extends BaseActivity {
    
    private static final String TAG = "ReminderSettings";
    private static final String PREFS_NAME = "JunoReminderPrefs";
    
    // Keys for preferences
    private static final String KEY_PRODUCTIVE_HOURS_START = "productive_hours_start";
    private static final String KEY_PRODUCTIVE_HOURS_END = "productive_hours_end";
    private static final String KEY_REMINDER_STYLE = "reminder_style";
    private static final String KEY_REMINDER_ADVANCE_TIME = "reminder_advance_time";
    
    // Default values
    private static final int DEFAULT_PRODUCTIVE_HOURS_START = 9; // 9 AM
    private static final int DEFAULT_PRODUCTIVE_HOURS_END = 18; // 6 PM
    private static final String DEFAULT_REMINDER_STYLE = "gentle"; // gentle, moderate, assertive
    private static final int DEFAULT_REMINDER_ADVANCE_TIME = 24; // hours before deadline
    
    // UI elements
    private ImageView backButton;
    private TextView titleText;
    private TextView productiveHoursStartText;
    private TextView productiveHoursEndText;
    private SeekBar productiveHoursStartSeekBar;
    private SeekBar productiveHoursEndSeekBar;
    private RadioGroup reminderStyleRadioGroup;
    private RadioButton gentleStyleRadio;
    private RadioButton moderateStyleRadio;
    private RadioButton assertiveStyleRadio;
    private TextView advanceTimeText;
    private SeekBar advanceTimeSeekBar;
    private Button saveButton;
    
    // Settings values
    private int productiveHoursStart;
    private int productiveHoursEnd;
    private String reminderStyle;
    private int reminderAdvanceTime;
    
    // Preference access
    private SharedPreferences preferences;
    private ReminderManager reminderManager;
    private String userId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_settings);
        
        // Get user ID from shared preferences
        SharedPreferences userPrefs = getSharedPreferences("JunoUserPrefs", MODE_PRIVATE);
        userId = userPrefs.getString("userId", "");
        if (userId.isEmpty()) {
            finish(); // No user ID, close the activity
            return;
        }
        
        // Initialize reminder preferences
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        reminderManager = new ReminderManager(this, userId);
        
        // Initialize UI elements
        initializeViews();
        
        // Load current settings
        loadSettings();
        
        // Set up listeners
        setupListeners();
    }
    
    private void initializeViews() {
        backButton = findViewById(R.id.back_button);
        titleText = findViewById(R.id.title_text);
        
        productiveHoursStartText = findViewById(R.id.productive_hours_start_text);
        productiveHoursEndText = findViewById(R.id.productive_hours_end_text);
        productiveHoursStartSeekBar = findViewById(R.id.productive_hours_start_seekbar);
        productiveHoursEndSeekBar = findViewById(R.id.productive_hours_end_seekbar);
        
        reminderStyleRadioGroup = findViewById(R.id.reminder_style_radio_group);
        gentleStyleRadio = findViewById(R.id.gentle_style_radio);
        moderateStyleRadio = findViewById(R.id.moderate_style_radio);
        assertiveStyleRadio = findViewById(R.id.assertive_style_radio);
        
        advanceTimeText = findViewById(R.id.advance_time_text);
        advanceTimeSeekBar = findViewById(R.id.advance_time_seekbar);
        
        saveButton = findViewById(R.id.save_button);
        
        // Configure seek bars
        productiveHoursStartSeekBar.setMax(23); // 0-23 hours
        productiveHoursEndSeekBar.setMax(23);
        advanceTimeSeekBar.setMax(72); // Up to 72 hours (3 days) in advance
    }
    
    private void loadSettings() {
        // Load values from SharedPreferences
        productiveHoursStart = preferences.getInt(KEY_PRODUCTIVE_HOURS_START, DEFAULT_PRODUCTIVE_HOURS_START);
        productiveHoursEnd = preferences.getInt(KEY_PRODUCTIVE_HOURS_END, DEFAULT_PRODUCTIVE_HOURS_END);
        reminderStyle = preferences.getString(KEY_REMINDER_STYLE, DEFAULT_REMINDER_STYLE);
        reminderAdvanceTime = preferences.getInt(KEY_REMINDER_ADVANCE_TIME, DEFAULT_REMINDER_ADVANCE_TIME);
        
        // Update UI with loaded values
        productiveHoursStartSeekBar.setProgress(productiveHoursStart);
        productiveHoursEndSeekBar.setProgress(productiveHoursEnd);
        updateProductiveHoursText();
        
        // Set radio button based on reminder style
        switch (reminderStyle) {
            case "gentle":
                gentleStyleRadio.setChecked(true);
                break;
            case "moderate":
                moderateStyleRadio.setChecked(true);
                break;
            case "assertive":
                assertiveStyleRadio.setChecked(true);
                break;
        }
        
        advanceTimeSeekBar.setProgress(reminderAdvanceTime);
        updateAdvanceTimeText();
    }
    
    private void setupListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());
        
        // Productive hours start seek bar
        productiveHoursStartSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                productiveHoursStart = progress;
                
                // Ensure start is before end
                if (productiveHoursStart >= productiveHoursEnd) {
                    productiveHoursEnd = Math.min(productiveHoursStart + 1, 23);
                    productiveHoursEndSeekBar.setProgress(productiveHoursEnd);
                }
                
                updateProductiveHoursText();
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Productive hours end seek bar
        productiveHoursEndSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                productiveHoursEnd = progress;
                
                // Ensure end is after start
                if (productiveHoursEnd <= productiveHoursStart) {
                    productiveHoursStart = Math.max(productiveHoursEnd - 1, 0);
                    productiveHoursStartSeekBar.setProgress(productiveHoursStart);
                }
                
                updateProductiveHoursText();
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Reminder style radio group
        reminderStyleRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.gentle_style_radio) {
                reminderStyle = "gentle";
            } else if (checkedId == R.id.moderate_style_radio) {
                reminderStyle = "moderate";
            } else if (checkedId == R.id.assertive_style_radio) {
                reminderStyle = "assertive";
            }
        });
        
        // Advance time seek bar
        advanceTimeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Ensure at least 1 hour advance notice
                reminderAdvanceTime = Math.max(1, progress);
                updateAdvanceTimeText();
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Save button
        saveButton.setOnClickListener(v -> saveSettings());
    }
    
    private void updateProductiveHoursText() {
        String startTime = formatHour(productiveHoursStart);
        String endTime = formatHour(productiveHoursEnd);
        
        productiveHoursStartText.setText("Start: " + startTime);
        productiveHoursEndText.setText("End: " + endTime);
    }
    
    private void updateAdvanceTimeText() {
        String timeText;
        
        if (reminderAdvanceTime < 24) {
            timeText = reminderAdvanceTime + " hour" + (reminderAdvanceTime > 1 ? "s" : "");
        } else {
            int days = reminderAdvanceTime / 24;
            int hours = reminderAdvanceTime % 24;
            
            if (hours == 0) {
                timeText = days + " day" + (days > 1 ? "s" : "");
            } else {
                timeText = days + " day" + (days > 1 ? "s" : "") + ", " + 
                        hours + " hour" + (hours > 1 ? "s" : "");
            }
        }
        
        advanceTimeText.setText("Remind me " + timeText + " before deadline");
    }
    
    private String formatHour(int hour) {
        String amPm = hour < 12 ? "AM" : "PM";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        
        return displayHour + ":00 " + amPm;
    }
    
    private void saveSettings() {
        // Save settings to preferences
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(KEY_PRODUCTIVE_HOURS_START, productiveHoursStart);
        editor.putInt(KEY_PRODUCTIVE_HOURS_END, productiveHoursEnd);
        editor.putString(KEY_REMINDER_STYLE, reminderStyle);
        editor.putInt(KEY_REMINDER_ADVANCE_TIME, reminderAdvanceTime);
        editor.apply();
        
        // Update reminder manager with new settings
        reminderManager.updatePreferences(
                productiveHoursStart,
                productiveHoursEnd,
                reminderStyle,
                reminderAdvanceTime
        );
        
        Toast.makeText(this, "Reminder settings saved", Toast.LENGTH_SHORT).show();
        finish();
    }
} 