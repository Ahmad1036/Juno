package com.example.juno;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";
    private static final String USERS_PATH = "users";
    private static final String PREFS_NAME = "JunoUserPrefs";
    private static final int MAX_PROFILE_IMAGE_SIZE = 500; // max width/height in pixels

    private ImageView backButton;
    private TextView titleTextView;
    private CircleImageView profileImageView;
    private ImageView editProfilePictureButton;
    private TextInputEditText nameEditText, emailEditText;
    private Button saveProfileButton, goToChangePasswordButton;
    private TextInputLayout nameInputLayout, emailInputLayout;
    private ProgressBar progressBar;
    
    // Firebase
    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;
    
    // User data
    private String userId;
    private String userEmail;
    private String userName;
    private String storedHashedPassword;
    private Uri selectedImageUri = null;
    private boolean imageChanged = false;
    
    // Activity result launcher for image selection
    private ActivityResultLauncher<String> mGetContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://juno-aa7d2-default-rtdb.firebaseio.com/");
        mDatabase = database.getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        // Register activity result launcher for image selection
        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    imageChanged = true;
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        bitmap = resizeImageIfNeeded(bitmap, MAX_PROFILE_IMAGE_SIZE);
                        profileImageView.setImageBitmap(bitmap);
                    } catch (IOException e) {
                        Log.e(TAG, "Error loading image from gallery", e);
                        Toast.makeText(EditProfileActivity.this, 
                                "Error loading image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
        
        // Initialize UI components
        initializeViews();
        
        // Create progress bar programmatically
        setupProgressBar();

        // Get user data from SharedPreferences
        retrieveUserData();
        
        // Set up animations for UI elements
        setupAnimations();

        // Set up click listeners
        setupClickListeners();
        
        // Load user profile data
        loadUserProfile();

        // Add this cleanup method after the onCreate method
        
        /**
         * Cleanup existing database inconsistencies that might have occurred
         * during previous profile updates. This helps maintain data integrity.
         */
        cleanupDatabaseInconsistencies();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        titleTextView = findViewById(R.id.titleTextView);
        
        // Profile picture views
        profileImageView = findViewById(R.id.profileImageView);
        editProfilePictureButton = findViewById(R.id.editProfilePictureButton);
        
        // Profile fields
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        nameInputLayout = findViewById(R.id.nameInputLayout);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        saveProfileButton = findViewById(R.id.saveProfileButton);
        goToChangePasswordButton = findViewById(R.id.goToChangePasswordButton);
    }
    
    private void setupProgressBar() {
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        
        // Add to layout
        FrameLayout rootView = (FrameLayout) findViewById(android.R.id.content);
        rootView.addView(progressBar);
        
        // Center in parent
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = android.view.Gravity.CENTER;
        progressBar.setLayoutParams(params);
    }

    private void retrieveUserData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        userName = prefs.getString("userName", "");
        userEmail = prefs.getString("userEmail", "");
        
        // Set name and email fields
        nameEditText.setText(userName);
        emailEditText.setText(userEmail);
        
        if (userId == null) {
            // No user logged in, redirect to sign in
            Toast.makeText(this, "Please sign in to edit your profile", Toast.LENGTH_LONG).show();
            navigateToSignIn();
        }
    }
    
    private void setupAnimations() {
        // Fade in animations for form elements
        View[] views = {profileImageView, editProfilePictureButton, nameEditText, emailEditText, 
                        saveProfileButton, goToChangePasswordButton};
        
        for (int i = 0; i < views.length; i++) {
            views[i].setAlpha(0f);
            views[i].animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setStartDelay(100 + (i * 100))
                    .start();
        }
    }

    private void setupClickListeners() {
        // Back button click
        backButton.setOnClickListener(v -> onBackPressed());
        
        // Edit profile picture button click
        editProfilePictureButton.setOnClickListener(v -> selectImage());
        
        // Save Profile button click
        saveProfileButton.setOnClickListener(v -> {
            if (validateProfileForm()) {
                showProgressBar(true);
                updateUserProfile();
            }
        });

        // Go to Change Password button click
        goToChangePasswordButton.setOnClickListener(v -> {
            Intent intent = new Intent(EditProfileActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }
    
    private void selectImage() {
        mGetContent.launch("image/*");
    }
    
    private void loadUserProfile() {
        if (userId != null) {
            showProgressBar(true);
            
            // Try to get user data directly from userId first
            mDatabase.child(USERS_PATH).orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // User found by email query
                        for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                            // Get user data
                            if (childSnapshot.hasChild("name")) {
                                userName = childSnapshot.child("name").getValue(String.class);
                                nameEditText.setText(userName);
                            }
                            
                            if (childSnapshot.hasChild("email")) {
                                userEmail = childSnapshot.child("email").getValue(String.class);
                                emailEditText.setText(userEmail);
                            }
                            
                            // Load profile image if exists
                            if (childSnapshot.hasChild("profileImageUrl")) {
                                String imageUrl = childSnapshot.child("profileImageUrl").getValue(String.class);
                                loadProfileImage(imageUrl);
                            } else {
                                // Use default placeholder
                                showProgressBar(false);
                            }
                            
                            return; // Process only the first match
                        }
                    } else {
                        // Try with encoded email as fallback
                        String encodedEmail = encodeEmail(userEmail);
                        mDatabase.child(USERS_PATH).child(encodedEmail).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    // Get user data
                                    if (dataSnapshot.hasChild("name")) {
                                        userName = dataSnapshot.child("name").getValue(String.class);
                                        nameEditText.setText(userName);
                                    }
                                    
                                    if (dataSnapshot.hasChild("email")) {
                                        userEmail = dataSnapshot.child("email").getValue(String.class);
                                        emailEditText.setText(userEmail);
                                    }
                                    
                                    // Load profile image if exists
                                    if (dataSnapshot.hasChild("profileImageUrl")) {
                                        String imageUrl = dataSnapshot.child("profileImageUrl").getValue(String.class);
                                        loadProfileImage(imageUrl);
                                    } else {
                                        // Use default placeholder
                                        showProgressBar(false);
                                    }
                                } else {
                                    // As a last resort, check all users
                                    mDatabase.child(USERS_PATH).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot allUsersSnapshot) {
                                            boolean userFound = false;
                                            for (DataSnapshot userSnapshot : allUsersSnapshot.getChildren()) {
                                                // Check if this node has our user ID or email
                                                if (userSnapshot.hasChild("email") && 
                                                    userEmail.equals(userSnapshot.child("email").getValue(String.class))) {
                                                    // Found the user
                                                    userFound = true;
                                                    
                                                    // Get user data
                                                    if (userSnapshot.hasChild("name")) {
                                                        userName = userSnapshot.child("name").getValue(String.class);
                                                        nameEditText.setText(userName);
                                                    }
                                                    
                                                    // Load profile image if exists
                                                    if (userSnapshot.hasChild("profileImageUrl")) {
                                                        String imageUrl = userSnapshot.child("profileImageUrl").getValue(String.class);
                                                        loadProfileImage(imageUrl);
                                                    } else {
                                                        showProgressBar(false);
                                                    }
                                                    
                                                    break;
                                                }
                                            }
                                            
                                            if (!userFound) {
                                                showProgressBar(false);
                                                showToast("User data not found. Please sign in again.");
                                            }
                                        }
                                        
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                            showProgressBar(false);
                                            Log.e(TAG, "Database error loading all users: " + databaseError.getMessage());
                                            showToast("Error loading profile: " + databaseError.getMessage());
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                showProgressBar(false);
                                Log.e(TAG, "Database error loading profile with encoded email: " + databaseError.getMessage());
                                showToast("Error loading profile: " + databaseError.getMessage());
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    showProgressBar(false);
                    Log.e(TAG, "Database error loading profile by email query: " + databaseError.getMessage());
                    showToast("Error loading profile: " + databaseError.getMessage());
                }
            });
        }
    }
    
    private void loadProfileImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Use Glide or Picasso to load the image
            // For simplicity, we'll just use the Firebase Storage download URL
            StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                // Use Glide to load image
                Glide.with(EditProfileActivity.this)
                        .load(uri)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(profileImageView);
                showProgressBar(false);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to load profile image", e);
                showProgressBar(false);
            });
        } else {
            showProgressBar(false);
        }
    }

    private boolean validateProfileForm() {
        boolean valid = true;

        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        
        // Validate name
        if (TextUtils.isEmpty(name)) {
            nameInputLayout.setError("Name cannot be empty");
            valid = false;
        } else {
            nameInputLayout.setError(null);
        }
        
        // Validate email
        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError("Email cannot be empty");
            valid = false;
        } else if (!isValidEmail(email)) {
            emailInputLayout.setError("Please enter a valid email");
            valid = false;
        } else {
            emailInputLayout.setError(null);
        }

        return valid;
    }

    private void updateUserProfile() {
        String newName = nameEditText.getText().toString().trim();
        String newEmail = emailEditText.getText().toString().trim();
        
        // If email is changed, need to update user node key
        if (!newEmail.equals(userEmail)) {
            // Check if new email already exists
            mDatabase.child(USERS_PATH).child(encodeEmail(newEmail))
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        showProgressBar(false);
                        emailInputLayout.setError("Email already in use");
                        showToast("Email already in use");
                    } else {
                        // Email is available, proceed with update
                        if (imageChanged && selectedImageUri != null) {
                            uploadImageAndUpdateProfile(newName, newEmail);
                        } else {
                            performProfileUpdate(newName, newEmail, null);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    showProgressBar(false);
                    Log.e(TAG, "Failed to check email availability", databaseError.toException());
                    showToast("Error checking email: " + databaseError.getMessage());
                }
            });
        } else {
            // Email not changed
            if (imageChanged && selectedImageUri != null) {
                uploadImageAndUpdateProfile(newName, userEmail);
            } else {
                performProfileUpdate(newName, userEmail, null);
            }
        }
    }
    
    private void uploadImageAndUpdateProfile(String newName, String newEmail) {
        try {
            // Convert the image to a bitmap
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
            bitmap = resizeImageIfNeeded(bitmap, MAX_PROFILE_IMAGE_SIZE);
            
            // Convert bitmap to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos);
            byte[] data = baos.toByteArray();
            
            // Create a storage reference
            String imagePath = "profile_images/" + userId + "_" + System.currentTimeMillis() + ".jpg";
            StorageReference imageRef = mStorageRef.child(imagePath);
            
            // Upload the image
            UploadTask uploadTask = imageRef.putBytes(data);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // Get the download URL
                imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    // Update profile with image URL
                    performProfileUpdate(newName, newEmail, uri.toString());
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get download URL", e);
                    performProfileUpdate(newName, newEmail, null);
                });
            }).addOnFailureListener(e -> {
                showProgressBar(false);
                Log.e(TAG, "Image upload failed", e);
                showToast("Failed to upload profile image: " + e.getMessage());
                // Still update the profile without image
                performProfileUpdate(newName, newEmail, null);
            });
        } catch (IOException e) {
            showProgressBar(false);
            Log.e(TAG, "Error processing the selected image", e);
            showToast("Error processing image: " + e.getMessage());
            // Still update the profile without image
            performProfileUpdate(newName, newEmail, null);
        }
    }
    
    private void performProfileUpdate(String newName, String newEmail, String imageUrl) {
        showProgressBar(true);
        
        final String oldEncodedEmail = encodeEmail(userEmail);
        final String newEncodedEmail = encodeEmail(newEmail);
        
        // First, find the current user node
        mDatabase.child(USERS_PATH).orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(
            new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Get the first node with this email
                        DataSnapshot userNode = dataSnapshot.getChildren().iterator().next();
                        final String userNodeKey = userNode.getKey();
                        
                        if (newEmail.equals(userEmail)) {
                            // Email not changed - just update name and image
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("users/" + userNodeKey + "/name", newName);
                            
                            // Add profile image URL if available
                            if (imageUrl != null) {
                                updates.put("users/" + userNodeKey + "/profileImageUrl", imageUrl);
                            }
                            
                            // Apply all updates at once for consistency
                            mDatabase.updateChildren(updates)
                                .addOnSuccessListener(aVoid -> {
                                    // Update SharedPreferences
                                    updateUserSession(newName, userEmail);
                                    showProgressBar(false);
                                    showToast("Profile updated successfully");
                                })
                                .addOnFailureListener(e -> {
                                    showProgressBar(false);
                                    Log.e(TAG, "Failed to update profile", e);
                                    showToast("Failed to update profile: " + e.getMessage());
                                });
                            
                        } else {
                            // Email changed - this is more complex
                            // First check if the new email already exists in the database
                            mDatabase.child(USERS_PATH).orderByChild("email").equalTo(newEmail)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            // Email already in use by someone else
                                            showProgressBar(false);
                                            emailInputLayout.setError("Email already in use");
                                            showToast("Email already in use by another account");
                                        } else {
                                            // Email available, create a multi-path update
                                            Map<String, Object> userData = new HashMap<>();
                                            
                                            // Get all existing data
                                            for (DataSnapshot child : userNode.getChildren()) {
                                                userData.put(child.getKey(), child.getValue());
                                            }
                                            
                                            // Update with new values
                                            userData.put("name", newName);
                                            userData.put("email", newEmail);
                                            
                                            // Add profile image URL if available
                                            if (imageUrl != null) {
                                                userData.put("profileImageUrl", imageUrl);
                                            }
                                            
                                            // Create map for atomic multi-path update
                                            Map<String, Object> updates = new HashMap<>();
                                            
                                            // Remove the user_emails old reference
                                            updates.put("user_emails/" + oldEncodedEmail, null);
                                            
                                            // Update the user_emails new reference
                                            updates.put("user_emails/" + newEncodedEmail, userNodeKey);
                                            
                                            // Update the user data
                                            updates.put("users/" + userNodeKey, userData);
                                            
                                            // Apply all updates at once for consistency
                                            mDatabase.updateChildren(updates)
                                                .addOnSuccessListener(aVoid -> {
                                                    // Update SharedPreferences
                                                    updateUserSession(newName, newEmail);
                                                    
                                                    showProgressBar(false);
                                                    showToast("Profile updated successfully");
                                                })
                                                .addOnFailureListener(e -> {
                                                    showProgressBar(false);
                                                    Log.e(TAG, "Failed to update profile", e);
                                                    showToast("Failed to update profile: " + e.getMessage());
                                                });
                                        }
                                    }
                                    
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        showProgressBar(false);
                                        Log.e(TAG, "Failed to check if new email exists", databaseError.toException());
                                        showToast("Error checking email: " + databaseError.getMessage());
                                    }
                                });
                        }
                    } else {
                        // Try with encoded email directly (fallback)
                        mDatabase.child(USERS_PATH).child(oldEncodedEmail).addListenerForSingleValueEvent(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        // Found user with encoded email, perform similar update logic
                                        if (newEmail.equals(userEmail)) {
                                            // Just update name and image
                                            Map<String, Object> updates = new HashMap<>();
                                            updates.put("name", newName);
                                            
                                            // Add profile image URL if available
                                            if (imageUrl != null) {
                                                updates.put("profileImageUrl", imageUrl);
                                            }
                                            
                                            mDatabase.child(USERS_PATH).child(oldEncodedEmail).updateChildren(updates)
                                                .addOnSuccessListener(aVoid -> {
                                                    updateUserSession(newName, userEmail);
                                                    showProgressBar(false);
                                                    showToast("Profile updated successfully");
                                                })
                                                .addOnFailureListener(e -> {
                                                    showProgressBar(false);
                                                    Log.e(TAG, "Failed to update profile", e);
                                                    showToast("Failed to update profile: " + e.getMessage());
                                                });
                                        } else {
                                            // Handle email change for encoded email path
                                            Map<String, Object> userData = new HashMap<>();
                                            
                                            // Copy all existing data
                                            for (DataSnapshot child : dataSnapshot.getChildren()) {
                                                userData.put(child.getKey(), child.getValue());
                                            }
                                            
                                            // Update with new values
                                            userData.put("name", newName);
                                            userData.put("email", newEmail);
                                            
                                            // Add profile image URL if available
                                            if (imageUrl != null) {
                                                userData.put("profileImageUrl", imageUrl);
                                            }
                                            
                                            // Multi-path update
                                            Map<String, Object> updates = new HashMap<>();
                                            updates.put("users/" + newEncodedEmail, userData);
                                            updates.put("users/" + oldEncodedEmail, null);
                                            updates.put("user_emails/" + oldEncodedEmail, null);
                                            updates.put("user_emails/" + newEncodedEmail, newEncodedEmail);
                                            
                                            mDatabase.updateChildren(updates)
                                                .addOnSuccessListener(aVoid -> {
                                                    updateUserSession(newName, newEmail);
                                                    showProgressBar(false);
                                                    showToast("Profile updated successfully");
                                                })
                                                .addOnFailureListener(e -> {
                                                    showProgressBar(false);
                                                    Log.e(TAG, "Failed to update profile", e);
                                                    showToast("Failed to update profile: " + e.getMessage());
                                                });
                                        }
                                    } else {
                                        showProgressBar(false);
                                        showToast("Could not find user data to update");
                                    }
                                }
                                
                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    showProgressBar(false);
                                    Log.e(TAG, "Database error checking encoded email", databaseError.toException());
                                    showToast("Error updating profile: " + databaseError.getMessage());
                                }
                            });
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    showProgressBar(false);
                    Log.e(TAG, "Database error finding user for profile update", databaseError.toException());
                    showToast("Error updating profile: " + databaseError.getMessage());
                }
            });
    }
    
    private void updateUserSession(String newName, String newEmail) {
        // Update SharedPreferences
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString("userName", newName);
        editor.putString("userEmail", newEmail);
        editor.apply();
        
        // Update instance variables
        userName = newName;
        userEmail = newEmail;
    }
    
    private Bitmap resizeImageIfNeeded(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        float ratio = (float) width / (float) height;
        
        if (width > maxSize || height > maxSize) {
            if (width > height) {
                width = maxSize;
                height = (int) (width / ratio);
            } else {
                height = maxSize;
                width = (int) (height * ratio);
            }
            return Bitmap.createScaledBitmap(image, width, height, true);
        }
        
        return image;
    }
    
    private String encodeEmail(String email) {
        return email.replace(".", ",");
    }
    
    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    
    private void navigateToSignIn() {
        Intent intent = new Intent(EditProfileActivity.this, SignInActivity.class);
        startActivity(intent);
        finish();
    }
    
    private void showProgressBar(boolean show) {
        if (progressBar != null) {
            if (show) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        }
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * Cleanup existing database inconsistencies that might have occurred
     * during previous profile updates. This helps maintain data integrity.
     */
    private void cleanupDatabaseInconsistencies() {
        // Only run if we have user info
        if (userEmail == null || userEmail.isEmpty()) {
            return;
        }
        
        showProgressBar(true);
        Log.d(TAG, "Running database cleanup for " + userEmail);
        
        // First, check for any entries with the current email directly
        String encodedEmail = encodeEmail(userEmail);
        mDatabase.child(USERS_PATH).orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(
            new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Found user nodes with this email
                        DataSnapshot targetNode = null;
                        Map<String, Object> nodesToRemove = new HashMap<>();
                        
                        // Find the most complete node to keep
                        for (DataSnapshot node : dataSnapshot.getChildren()) {
                            if (targetNode == null || 
                                (node.hasChild("password") && node.hasChild("createdAt"))) {
                                targetNode = node;
                            }
                        }
                        
                        if (targetNode != null) {
                            final String nodeKey = targetNode.getKey();
                            
                            // Any additional nodes with this email should be removed
                            for (DataSnapshot node : dataSnapshot.getChildren()) {
                                if (!node.getKey().equals(nodeKey)) {
                                    nodesToRemove.put("users/" + node.getKey(), null);
                                }
                            }
                            
                            // Check if we have an incorrect entry in the users path with key=encodedEmail
                            mDatabase.child(USERS_PATH).child(encodedEmail).addListenerForSingleValueEvent(
                                new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists() && !encodedEmail.equals(nodeKey)) {
                                            // We have a node with encoded email as key, should remove if different from our target node
                                            nodesToRemove.put("users/" + encodedEmail, null);
                                        }
                                        
                                        // Update user_emails entry to point to the correct node
                                        nodesToRemove.put("user_emails/" + encodedEmail, nodeKey);
                                        
                                        // Perform database cleanup if needed
                                        if (!nodesToRemove.isEmpty()) {
                                            mDatabase.updateChildren(nodesToRemove)
                                                .addOnSuccessListener(aVoid -> {
                                                    Log.d(TAG, "Database cleanup successful");
                                                    showProgressBar(false);
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "Database cleanup failed", e);
                                                    showProgressBar(false);
                                                });
                                        } else {
                                            showProgressBar(false);
                                        }
                                    }
                                    
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        showProgressBar(false);
                                        Log.e(TAG, "Cleanup check for encoded email failed", databaseError.toException());
                                    }
                                }
                            );
                        } else {
                            showProgressBar(false);
                        }
                    } else {
                        showProgressBar(false);
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    showProgressBar(false);
                    Log.e(TAG, "Database cleanup by email query failed", databaseError.toException());
                }
            }
        );
    }
}