package com.example.juno;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class JournalActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = "JournalActivity";
    private static final int REQUEST_VOICE_INPUT = 100;
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private static final int REQUEST_CAMERA_IMAGE = 102;
    private static final int REQUEST_GALLERY_IMAGE = 103;

    // UI Components
    private ImageButton backButton;
    private ImageButton saveButton;
    private ImageButton deleteButton;
    private TextView dateText;
    private TextView lastUpdatedText;
    private EditText journalEntry;
    private TextView gratitudePrompt;
    private EditText gratitudeEntry;
    private CardView imageCard;
    private ImageView journalImage;
    private ImageButton removeImageButton;
    private ImageButton voiceInputButton;
    private ImageButton textToSpeechButton;
    private ImageButton attachImageButton;

    // Data
    private String userId;
    private String journalId;
    private boolean isEditing = false;
    private Uri selectedImageUri;
    private Bitmap capturedImageBitmap;
    private String existingImageData; // To store existing image data when editing
    private DatabaseReference mDatabase;
    private TextToSpeech textToSpeech;
    private boolean isTtsReady = false;
    private EditText activeEditText; // The currently active EditText for voice input

    // Gratitude prompts
    private final String[] gratitudePrompts = {
            "what are three things you're grateful for today?",
            "who is someone that made a positive impact on your life recently?",
            "what made you smile today?",
            "what's something you're looking forward to?",
            "what's something you appreciate about yourself?",
            "what's a small thing that brought you joy today?",
            "what's something beautiful you noticed today?",
            "what's a challenge you're grateful to have overcome?"
    };

    // Store the original timestamp when loading an existing journal
    private long originalTimestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal);

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance("https://juno-aa7d2-default-rtdb.firebaseio.com/").getReference();

        // Get user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("JunoUserPrefs", MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(this, this);

        // Initialize UI components
        initializeViews();

        // Reset image-related variables
        selectedImageUri = null;
        capturedImageBitmap = null;
        existingImageData = null;
        
        // Hide delete button by default, only show when editing
        deleteButton.setVisibility(View.GONE);

        // Check if we're editing an existing entry
        journalId = getIntent().getStringExtra("journalId");
        isEditing = journalId != null && !journalId.isEmpty();
        
        Log.d(TAG, "Journal ID from intent: " + journalId);
        Log.d(TAG, "Is editing existing journal: " + isEditing);

        if (isEditing) {
            // Load the existing journal entry
            loadJournalEntry();
        } else {
            // Set today's date
            updateDateText();
            
            // Set a random gratitude prompt
            setRandomGratitudePrompt();
            
            // Make sure image card is hidden for new entries
            imageCard.setVisibility(View.GONE);
        }

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.journal_back_button);
        saveButton = findViewById(R.id.journal_save_button);
        deleteButton = findViewById(R.id.journal_delete_button);
        dateText = findViewById(R.id.journal_date);
        lastUpdatedText = findViewById(R.id.journal_last_updated);
        journalEntry = findViewById(R.id.journal_entry);
        gratitudePrompt = findViewById(R.id.gratitude_prompt);
        gratitudeEntry = findViewById(R.id.gratitude_entry);
        imageCard = findViewById(R.id.journal_image_card);
        journalImage = findViewById(R.id.journal_image);
        removeImageButton = findViewById(R.id.remove_image_button);
        voiceInputButton = findViewById(R.id.voice_input_button);
        textToSpeechButton = findViewById(R.id.text_to_speech_button);
        attachImageButton = findViewById(R.id.attach_image_button);
        
        // Set initial active EditText for voice input
        activeEditText = journalEntry;
    }

    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> {
            // Ask user if they want to save before exiting if there's content
            if (!journalEntry.getText().toString().trim().isEmpty() || 
                !gratitudeEntry.getText().toString().trim().isEmpty() ||
                selectedImageUri != null || 
                capturedImageBitmap != null) {
                showSaveConfirmationDialog();
            } else {
                finish();
            }
        });

        // Save button
        saveButton.setOnClickListener(v -> saveJournalEntry());

        // Set focus listeners to track active EditText for voice input
        journalEntry.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) activeEditText = journalEntry;
        });
        
        gratitudeEntry.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) activeEditText = gratitudeEntry;
        });

        // Voice input button
        voiceInputButton.setOnClickListener(v -> startVoiceInput());

        // Text-to-speech button
        textToSpeechButton.setOnClickListener(v -> {
            String textToRead = journalEntry.getText().toString().trim();
            if (!textToRead.isEmpty() && isTtsReady) {
                textToSpeech.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, "journal_entry");
            } else {
                Toast.makeText(this, "No text to read or text-to-speech not ready", Toast.LENGTH_SHORT).show();
            }
        });

        // Attach image button
        attachImageButton.setOnClickListener(v -> showImageSourceDialog());

        // Remove image button
        removeImageButton.setOnClickListener(v -> {
            selectedImageUri = null;
            capturedImageBitmap = null;
            existingImageData = null; // Also clear existing image data
            imageCard.setVisibility(View.GONE);
        });

        // Delete button
        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void setRandomGratitudePrompt() {
        Random random = new Random();
        int index = random.nextInt(gratitudePrompts.length);
        gratitudePrompt.setText(gratitudePrompts[index]);
    }

    private void updateDateText() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
        dateText.setText("today, " + dateFormat.format(new Date()).toLowerCase());
    }

    private void showSaveConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Save Entry")
                .setMessage("Do you want to save this journal entry?")
                .setPositiveButton("Save", (dialog, which) -> {
                    saveJournalEntry();
                    finish();
                })
                .setNegativeButton("Discard", (dialog, which) -> finish())
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void saveJournalEntry() {
        String content = journalEntry.getText().toString().trim();
        String gratitudeContent = gratitudeEntry.getText().toString().trim();

        if (content.isEmpty() && gratitudeContent.isEmpty() && 
            selectedImageUri == null && capturedImageBitmap == null && existingImageData == null) {
            Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        saveButton.setEnabled(false);

        // Log image state for debugging
        Log.d(TAG, "Image state - selectedImageUri: " + (selectedImageUri != null) + 
                   ", capturedImageBitmap: " + (capturedImageBitmap != null) + 
                   ", existingImageData: " + (existingImageData != null));

        // If there's a new image (from uri or camera), encode it
        if (selectedImageUri != null || capturedImageBitmap != null) {
            Log.d(TAG, "Saving with new image");
            encodeImageAndSaveJournal(content, gratitudeContent);
        } else if (existingImageData != null) {
            // Use the existing image data if available
            Log.d(TAG, "Saving with existing image");
            saveJournalToDatabase(content, gratitudeContent, existingImageData);
        } else {
            // No image, just save the journal entry
            Log.d(TAG, "Saving without image");
            saveJournalToDatabase(content, gratitudeContent, null);
        }
    }

    private void encodeImageAndSaveJournal(String content, String gratitudeContent) {
        try {
            // Show encoding message
            Toast.makeText(this, "Processing image...", Toast.LENGTH_SHORT).show();
            
            Bitmap bitmap;
            if (selectedImageUri != null) {
                // Get bitmap from gallery URI
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
            } else {
                // Use captured bitmap
                bitmap = capturedImageBitmap;
            }
            
            // Resize bitmap to reduce storage size
            bitmap = resizeBitmap(bitmap, 800);
            
            // Convert bitmap to Base64 string
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageData = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageData, Base64.DEFAULT);
            
            // Save journal with encoded image
            saveJournalToDatabase(content, gratitudeContent, base64Image);
            
        } catch (Exception e) {
            Log.e(TAG, "Error encoding image", e);
            Toast.makeText(this, "Error processing image. Journal saved without image.", Toast.LENGTH_SHORT).show();
            saveJournalToDatabase(content, gratitudeContent, null);
        }
    }
    
    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        if (width <= maxSize && height <= maxSize) {
            return bitmap; // No need to resize
        }
        
        float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private void saveJournalToDatabase(String content, String gratitudeContent, String imageData) {
        long currentTime = System.currentTimeMillis();
        
        // Create journal object
        Journal journal = new Journal(content, gratitudeContent, imageData, 
                isEditing ? getOriginalTimestamp() : currentTime, userId);
        
        // Set last updated timestamp
        journal.setLastUpdated(currentTime);
        
        // Log journal details before saving
        Log.d(TAG, "Saving journal with userId: " + userId);
        Log.d(TAG, "Journal content: " + (content != null ? content.substring(0, Math.min(content.length(), 50)) + "..." : "null"));
        
        // Generate a unique journal ID if not editing
        if (!isEditing) {
            journalId = mDatabase.child("journals").push().getKey();
        }
        
        if (journalId != null) {
            Log.d(TAG, "Saving to journal ID: " + journalId);
            mDatabase.child("journals").child(journalId)
                    .setValue(journal.toMap())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Journal successfully saved");
                        Toast.makeText(JournalActivity.this, "Journal entry saved", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save journal entry", e);
                        Toast.makeText(JournalActivity.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        saveButton.setEnabled(true);
                    });
        } else {
            Log.e(TAG, "Failed to generate journal ID");
            Toast.makeText(this, "Failed to create journal ID", Toast.LENGTH_SHORT).show();
            saveButton.setEnabled(true);
        }
    }

    private long getOriginalTimestamp() {
        return originalTimestamp;
    }

    private void loadJournalEntry() {
        // Show loading state
        saveButton.setEnabled(false);
        Toast.makeText(this, "Loading journal entry...", Toast.LENGTH_SHORT).show();
        
        Log.d(TAG, "Loading journal entry with ID: " + journalId);
        
        // Fetch the journal entry from Firebase
        mDatabase.child("journals").child(journalId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Journal journal = dataSnapshot.getValue(Journal.class);
                if (journal != null) {
                    Log.d(TAG, "Journal entry loaded successfully");
                    
                    // Store the original timestamp
                    originalTimestamp = journal.getTimestamp();
                    
                    // Set the journal content
                    journalEntry.setText(journal.getContent());
                    
                    // Set the gratitude content
                    if (journal.getGratitudeContent() != null && !journal.getGratitudeContent().isEmpty()) {
                        gratitudeEntry.setText(journal.getGratitudeContent());
                    }
                    
                    // Set the image if available
                    if (journal.getImageData() != null && !journal.getImageData().isEmpty()) {
                        try {
                            // Store the existing image data
                            existingImageData = journal.getImageData();
                            
                            byte[] decodedString = Base64.decode(existingImageData, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            if (bitmap != null) {
                                processImage(bitmap);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error decoding image data", e);
                        }
                    }
                    
                    // Set the date
                    SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
                    dateText.setText(dateFormat.format(new Date(journal.getTimestamp())).toLowerCase());
                    
                    // Show last updated info if different from creation time
                    if (journal.getLastUpdated() > journal.getTimestamp()) {
                        lastUpdatedText.setText("last edited: " + journal.getLastUpdatedFormatted().toLowerCase());
                        lastUpdatedText.setVisibility(View.VISIBLE);
                    } else {
                        lastUpdatedText.setVisibility(View.GONE);
                    }
                    
                    // Show delete button when editing
                    deleteButton.setVisibility(View.VISIBLE);
                    
                    // Enable the save button
                    saveButton.setEnabled(true);
                } else {
                    Log.e(TAG, "Journal entry not found");
                    Toast.makeText(JournalActivity.this, "Journal entry not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading journal entry", databaseError.toException());
                Toast.makeText(JournalActivity.this, "Error loading journal entry", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");
        
        try {
            startActivityForResult(intent, REQUEST_VOICE_INPUT);
        } catch (Exception e) {
            Toast.makeText(this, "Voice input not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }

    private void showImageSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Image");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Take photo
                checkCameraPermissionAndOpenCamera();
            } else if (which == 1) {
                // Choose from gallery
                openGallery();
            } else {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, REQUEST_CAMERA_IMAGE);
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, REQUEST_GALLERY_IMAGE);
    }

    private void processImage(Bitmap bitmap) {
        // Set image preview
        journalImage.setImageBitmap(bitmap);
        imageCard.setVisibility(View.VISIBLE);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.getDefault());
            
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported");
                Toast.makeText(this, "Text-to-speech language not supported", Toast.LENGTH_SHORT).show();
            } else {
                isTtsReady = true;
            }
        } else {
            Log.e(TAG, "Failed to initialize Text-to-Speech");
            Toast.makeText(this, "Failed to initialize Text-to-Speech", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_VOICE_INPUT) {
                // Handle voice input result
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result != null && !result.isEmpty()) {
                    String spokenText = result.get(0);
                    
                    // Add to the active EditText
                    if (activeEditText != null) {
                        String currentText = activeEditText.getText().toString();
                        
                        // Append or replace text
                        if (currentText.isEmpty()) {
                            activeEditText.setText(spokenText);
                        } else {
                            activeEditText.setText(currentText + " " + spokenText);
                        }
                        
                        // Place cursor at the end
                        activeEditText.setSelection(activeEditText.getText().length());
                    }
                }
            } else if (requestCode == REQUEST_CAMERA_IMAGE) {
                // Handle camera image result
                Bundle extras = data.getExtras();
                if (extras != null) {
                    capturedImageBitmap = (Bitmap) extras.get("data");
                    selectedImageUri = null; // Clear any gallery selection
                    processImage(capturedImageBitmap);
                }
            } else if (requestCode == REQUEST_GALLERY_IMAGE) {
                // Handle gallery image result
                selectedImageUri = data.getData();
                capturedImageBitmap = null; // Clear any camera capture
                
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                    processImage(bitmap);
                } catch (IOException e) {
                    Log.e(TAG, "Error loading image from gallery", e);
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void showDeleteConfirmationDialog() {
        if (!isEditing || journalId == null) {
            return; // Nothing to delete
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Journal Entry")
                .setMessage("Are you sure you want to delete this journal entry? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteJournalEntry())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteJournalEntry() {
        // Show loading state
        Toast.makeText(this, "Deleting journal entry...", Toast.LENGTH_SHORT).show();
        deleteButton.setEnabled(false);
        saveButton.setEnabled(false);

        // Delete the journal entry from Firebase
        mDatabase.child("journals").child(journalId)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Journal entry deleted successfully");
                    Toast.makeText(JournalActivity.this, "Journal entry deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete journal entry", e);
                    Toast.makeText(JournalActivity.this, "Failed to delete journal entry", Toast.LENGTH_SHORT).show();
                    deleteButton.setEnabled(true);
                    saveButton.setEnabled(true);
                });
    }
} 