package com.example.juno;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateTaskActivity extends AppCompatActivity {

    private static final String TAG = "CreateTaskActivity";
    private static final int REQUEST_VOICE_INPUT = 100;
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private static final int REQUEST_CAMERA_IMAGE = 102;
    private static final int REQUEST_GALLERY_IMAGE = 103;

    // UI Elements
    private ImageView backButton;
    private Spinner taskListSpinner;
    private ImageButton addListButton;
    private EditText taskDescriptionEditText;
    private ImageButton voiceInputButton;
    private ImageButton cameraInputButton;
    private CardView imagePreviewCard;
    private ImageView taskImagePreview;
    private TextView detectedObjectsText;
    private ImageButton removeImageButton;
    private LinearLayout datePickerLayout;
    private TextView dateTextView;
    private LinearLayout timePickerLayout;
    private TextView timeTextView;
    private LinearLayout lowPriorityButton;
    private LinearLayout mediumPriorityButton;
    private LinearLayout highPriorityButton;
    private ImageButton cancelButton;
    private ImageButton saveTaskButton;

    // Data
    private List<String> taskLists = new ArrayList<>();
    private String selectedTaskList;
    private String taskPriority = "medium"; // Default priority
    private Calendar selectedDateTime;
    private Uri selectedImageUri;
    private Bitmap capturedImageBitmap;
    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;
    private String userId;
    private boolean isTaskCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        // Check if we're in edit mode
        boolean isEditing = getIntent().getBooleanExtra("isEditing", false);
        String taskId = getIntent().getStringExtra("taskId");

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://juno-aa7d2-default-rtdb.firebaseio.com/");
        mDatabase = database.getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        // Get user ID from SharedPreferences
        userId = getSharedPreferences("JunoUserPrefs", MODE_PRIVATE).getString("userId", null);
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI components
        initializeViews();

        // Change title if in edit mode
        if (isEditing) {
            TextView titleTextView = findViewById(R.id.titleTextView);
            if (titleTextView != null) {
                titleTextView.setText("edit task");
            }
            
            // Load the existing task data for editing
            if (taskId != null && !taskId.isEmpty()) {
                loadTaskForEditing(taskId);
            }
        }
        
        // Set up task lists spinner
        fetchTaskLists();
        
        // Initialize date and time
        selectedDateTime = Calendar.getInstance();
        updateDateText();
        
        // Set up click listeners
        setupClickListeners();
        
        // Set up animations
        setupAnimations();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        taskListSpinner = findViewById(R.id.taskListSpinner);
        addListButton = findViewById(R.id.addListButton);
        taskDescriptionEditText = findViewById(R.id.taskDescriptionEditText);
        voiceInputButton = findViewById(R.id.voiceInputButton);
        cameraInputButton = findViewById(R.id.cameraInputButton);
        imagePreviewCard = findViewById(R.id.imagePreviewCard);
        taskImagePreview = findViewById(R.id.taskImagePreview);
        detectedObjectsText = findViewById(R.id.detectedObjectsText);
        removeImageButton = findViewById(R.id.removeImageButton);
        datePickerLayout = findViewById(R.id.datePickerLayout);
        dateTextView = findViewById(R.id.dateTextView);
        timePickerLayout = findViewById(R.id.timePickerLayout);
        timeTextView = findViewById(R.id.timeTextView);
        lowPriorityButton = findViewById(R.id.lowPriorityButton);
        mediumPriorityButton = findViewById(R.id.mediumPriorityButton);
        highPriorityButton = findViewById(R.id.highPriorityButton);
        cancelButton = findViewById(R.id.cancelButton);
        saveTaskButton = findViewById(R.id.saveTaskButton);
    }

    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> onBackPressed());
        
        // Add new list button
        addListButton.setOnClickListener(v -> showAddListDialog());
        
        // Voice input button
        voiceInputButton.setOnClickListener(v -> startVoiceInput());
        
        // Camera input button
        cameraInputButton.setOnClickListener(v -> showImageSourceDialog());
        
        // Remove image button
        removeImageButton.setOnClickListener(v -> {
            selectedImageUri = null;
            capturedImageBitmap = null;
            imagePreviewCard.setVisibility(View.GONE);
        });
        
        // Date picker
        datePickerLayout.setOnClickListener(v -> showDatePicker());
        
        // Time picker
        timePickerLayout.setOnClickListener(v -> showTimePicker());
        
        // Priority selection
        lowPriorityButton.setOnClickListener(v -> setPriority("low"));
        mediumPriorityButton.setOnClickListener(v -> setPriority("medium"));
        highPriorityButton.setOnClickListener(v -> setPriority("high"));
        
        // Cancel button
        cancelButton.setOnClickListener(v -> finish());
        
        // Save task button
        saveTaskButton.setOnClickListener(v -> saveTask());
    }

    private void setupAnimations() {
        // Fade in animations for all views
        View[] views = {
            backButton, taskListSpinner, addListButton, taskDescriptionEditText,
            voiceInputButton, cameraInputButton, datePickerLayout, timePickerLayout,
            lowPriorityButton, mediumPriorityButton, highPriorityButton,
            cancelButton, saveTaskButton
        };
        
        for (int i = 0; i < views.length; i++) {
            views[i].setAlpha(0f);
            views[i].animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay(50 + (i * 50))
                    .start();
        }
    }

    private void fetchTaskLists() {
        // Add default list
        taskLists.add("Personal Tasks");
        
        // Fetch from Firebase
        mDatabase.child("users").child(userId).child("task_lists")
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot listSnapshot : dataSnapshot.getChildren()) {
                        String listName = listSnapshot.getValue(String.class);
                        if (listName != null && !taskLists.contains(listName)) {
                            taskLists.add(listName);
                        }
                    }
                }
                
                // Set up spinner after fetching
                setupTaskListSpinner();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error fetching task lists", databaseError.toException());
                // Still set up spinner with default lists
                setupTaskListSpinner();
            }
        });
    }

    private void setupTaskListSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, taskLists);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        taskListSpinner.setAdapter(adapter);
        
        // If editing, try to select the correct task list
        boolean isEditing = getIntent().getBooleanExtra("isEditing", false);
        if (isEditing && selectedTaskList != null) {
            int position = taskLists.indexOf(selectedTaskList);
            if (position >= 0) {
                taskListSpinner.setSelection(position);
            }
        }
        
        taskListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTaskList = taskLists.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedTaskList = taskLists.get(0);
            }
        });
    }

    private void showAddListDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New List");
        
        final EditText input = new EditText(this);
        input.setHint("List name");
        builder.setView(input);
        
        builder.setPositiveButton("Create", (dialog, which) -> {
            String listName = input.getText().toString().trim();
            if (!listName.isEmpty()) {
                // Add to local list and update spinner
                taskLists.add(listName);
                setupTaskListSpinner();
                taskListSpinner.setSelection(taskLists.size() - 1);
                
                // Save to Firebase
                mDatabase.child("users").child(userId).child("task_lists")
                        .child(String.valueOf(taskLists.size() - 1))
                        .setValue(listName);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        
        builder.show();
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your task...");
        
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
        taskImagePreview.setImageBitmap(bitmap);
        imagePreviewCard.setVisibility(View.VISIBLE);
        
        // Simple placeholder for ML Kit object detection
        // In a real app, you would integrate ML Kit here
        detectedObjectsText.setText("Detected: Task-related objects");
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, month);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateText();
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateDateText() {
        // Format date based on whether it's today, tomorrow, or a future date
        Calendar today = Calendar.getInstance();
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        
        if (isSameDay(selectedDateTime, today)) {
            dateTextView.setText("Today");
        } else if (isSameDay(selectedDateTime, tomorrow)) {
            dateTextView.setText("Tomorrow");
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault());
            dateTextView.setText(dateFormat.format(selectedDateTime.getTime()));
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDateTime.set(Calendar.MINUTE, minute);
                    updateTimeText();
                },
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE),
                false
        );
        timePickerDialog.show();
    }

    private void updateTimeText() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        timeTextView.setText(timeFormat.format(selectedDateTime.getTime()));
    }

    private void setPriority(String priority) {
        taskPriority = priority;
        
        // Update UI to reflect selected priority
        lowPriorityButton.setBackgroundResource(priority.equals("low") ? 
                R.color.priority_low : R.drawable.priority_button_background);
        mediumPriorityButton.setBackgroundResource(priority.equals("medium") ? 
                R.color.priority_medium : R.drawable.priority_button_background);
        highPriorityButton.setBackgroundResource(priority.equals("high") ? 
                R.color.priority_high : R.drawable.priority_button_background);
    }

    private void loadTaskForEditing(String taskId) {
        // Show loading indicator
        showProgressBar(true);
        
        // Load the task data from Firebase
        mDatabase.child("users").child(userId).child("tasks").child(taskId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Task task = dataSnapshot.getValue(Task.class);
                        if (task != null) {
                            // Populate UI with task data
                            fillTaskDataInUI(task);
                        } else {
                            Toast.makeText(CreateTaskActivity.this, "Failed to load task data", Toast.LENGTH_SHORT).show();
                        }
                        showProgressBar(false);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error loading task for editing", databaseError.toException());
                        Toast.makeText(CreateTaskActivity.this, "Failed to load task: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        showProgressBar(false);
                    }
                });
    }

    private void fillTaskDataInUI(Task task) {
        // Set description
        if (task.getDescription() != null) {
            taskDescriptionEditText.setText(task.getDescription());
        }
        
        // Set task list (spinner selection)
        if (task.getTitle() != null) {
            selectedTaskList = task.getTitle();
            // Spinner selection will be handled after task lists are fetched
        }
        
        // Set due date if available
        if (task.getDueDate() > 0) {
            selectedDateTime.setTimeInMillis(task.getDueDate());
            updateDateText();
            updateTimeText();
        }
        
        // Set priority
        if (task.getPriority() != null) {
            setPriority(task.getPriority());
        }
        
        // Store completion status in a class variable to preserve it when updating
        isTaskCompleted = task.isCompleted();
        
        // Set image if available
        if (task.getImageData() != null && !task.getImageData().isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(task.getImageData(), Base64.DEFAULT);
                capturedImageBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                processImage(capturedImageBitmap);
            } catch (Exception e) {
                Log.e(TAG, "Error loading image data", e);
            }
        }
        
        // No need to update save button text, it's an icon now
    }

    // Helper method to show progress bar
    private void showProgressBar(boolean show) {
        // You might want to add a ProgressBar in your layout for this
        // For now, we'll just disable the save button
        saveTaskButton.setEnabled(!show);
    }

    private void saveTask() {
        String taskDescription = taskDescriptionEditText.getText().toString().trim();
        
        if (taskDescription.isEmpty()) {
            Toast.makeText(this, "Please enter a task description", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading state by disabling button
        saveTaskButton.setEnabled(false);
        
        // Create task data using the Task class field names
        final Map<String, Object> taskData = new HashMap<>();
        taskData.put("title", selectedTaskList); // Use list name as title
        taskData.put("description", taskDescription);
        
        // Check if we're in edit mode to preserve completion status
        boolean isEditing = getIntent().getBooleanExtra("isEditing", false);
        String taskId = getIntent().getStringExtra("taskId");
        
        // Use the stored completion status for editing, or default to false for new tasks
        taskData.put("completed", isEditing ? isTaskCompleted : false);
        
        taskData.put("dueDate", selectedDateTime.getTimeInMillis());
        if (!isEditing) {
            // Only set createdAt for new tasks
            taskData.put("createdAt", System.currentTimeMillis());
        }
        taskData.put("priority", taskPriority);
        
        // If there's an image, encode it as Base64 and add to task data
        if (selectedImageUri != null || capturedImageBitmap != null) {
            encodeImageAndSaveTask(taskData, isEditing, taskId);
        } else {
            // No image, just save the task
            saveTaskToDatabase(taskData, isEditing, taskId);
        }
    }

    private void encodeImageAndSaveTask(final Map<String, Object> taskData, boolean isEditing, String taskId) {
        // Check if user ID is available
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot encode image: User ID not available");
            Toast.makeText(this, "User ID missing. Task will be saved without image.", Toast.LENGTH_SHORT).show();
            saveTaskToDatabase(taskData, isEditing, taskId); // Save task without image
            return;
        }

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
            
            // Resize bitmap to reduce storage size (max width/height of 500px)
            bitmap = resizeBitmap(bitmap, 500);
            
            // Convert bitmap to Base64 string
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 30, baos); // Use low quality (30%) to reduce size
            byte[] imageData = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageData, Base64.DEFAULT);
            
            // Add base64 image string to task data
            Log.d(TAG, "Image encoded successfully, length: " + base64Image.length());
            taskData.put("imageData", base64Image);
            
            // Save task with encoded image
            saveTaskToDatabase(taskData, isEditing, taskId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error encoding image", e);
            Toast.makeText(this, "Error processing image. Task saved without image.", Toast.LENGTH_SHORT).show();
            saveTaskToDatabase(taskData, isEditing, taskId);
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

    private void saveTaskToDatabase(Map<String, Object> taskData, boolean isEditing, String taskId) {
        // Check if userId is valid
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot save task: User ID is null or empty");
            Toast.makeText(this, "Authentication error. Cannot save task.", Toast.LENGTH_LONG).show();
            saveTaskButton.setEnabled(true);
            return;
        }
        
        // Add a log to show we're attempting to save
        Log.d(TAG, "Attempting to save task to database for user: " + userId);
        
        // Generate a unique task ID
        final String taskIdToUse = taskId != null && !taskId.isEmpty() ? taskId : mDatabase.child("users").child(userId).child("tasks").push().getKey();
        
        if (taskIdToUse != null) {
            mDatabase.child("users").child(userId).child("tasks").child(taskIdToUse)
                    .setValue(taskData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(CreateTaskActivity.this, "Task " + (isEditing ? "updated" : "created") + " successfully", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Task " + (isEditing ? "updated" : "created") + " successfully with ID: " + taskIdToUse);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to " + (isEditing ? "update" : "create") + " task", e);
                        Toast.makeText(CreateTaskActivity.this, "Failed to " + (isEditing ? "update" : "create") + " task: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        saveTaskButton.setEnabled(true);
                    });
        } else {
            Log.e(TAG, "Failed to generate task ID");
            Toast.makeText(this, "Failed to create task ID", Toast.LENGTH_SHORT).show();
            saveTaskButton.setEnabled(true);
        }
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
                    String currentText = taskDescriptionEditText.getText().toString();
                    
                    // Append or replace text
                    if (currentText.isEmpty()) {
                        taskDescriptionEditText.setText(spokenText);
                    } else {
                        taskDescriptionEditText.setText(currentText + " " + spokenText);
                    }
                    
                    // Place cursor at the end
                    taskDescriptionEditText.setSelection(taskDescriptionEditText.getText().length());
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
} 