package com.example.potato1_events;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

// Import for image loading
import com.squareup.picasso.Picasso;

// Import for CircleImageView
import de.hdodenhof.circleimageview.CircleImageView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Activity responsible for handling user information, including profile picture selection,
 * uploading images to Firebase Storage, generating default avatars, and saving/updating user data to Firestore.
 */
public class UserInfoActivity extends AppCompatActivity {

    private static final String TAG = "UserInfoActivity";

    // Modes
    private static final String MODE_CREATE = "CREATE";
    private static final String MODE_EDIT = "EDIT";

    private String userType;
    private String deviceId;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    private CircleImageView profileImageView;
    private Button uploadRemovePictureButton;
    private EditText nameEditText, emailEditText, phoneEditText;
    private Button saveButton;

    private Uri selectedImageUri = null; // To store the selected image URI
    private boolean isProfilePictureRemoved = false; // Flag to indicate if the user removed the picture

    // Variables to hold existing image paths
    private String existingImagePath = null; // The current image path in Firestore

    // ActivityResultLauncher for selecting image
    private ActivityResultLauncher<String> selectImageLauncher;

    // ActivityResultLauncher for requesting permissions
    private ActivityResultLauncher<String> requestPermissionLauncher;

    // Current mode: CREATE or EDIT
    private String mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_info);

        // Get user type and mode from intent
        userType = getIntent().getStringExtra("USER_TYPE");
        mode = getIntent().getStringExtra("MODE"); // Expected to be "CREATE" or "EDIT"

        if (userType == null || (!mode.equals(MODE_CREATE) && !mode.equals(MODE_EDIT))) {
            Toast.makeText(this, "Invalid mode or user type.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get device ID
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Initialize Firebase Firestore and Storage
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Initialize views
        profileImageView = findViewById(R.id.profileImageView);
        uploadRemovePictureButton = findViewById(R.id.uploadPictureButton);
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        saveButton = findViewById(R.id.saveButton);

        // Initialize ActivityResultLauncher for image selection
        selectImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        isProfilePictureRemoved = false; // Reset removal flag
                        // Display the selected image in ImageView
                        Picasso.get().load(uri).into(profileImageView);
                        // Change button text to "Remove Picture"
                        uploadRemovePictureButton.setText("Remove Picture");
                    }
                }
        );

        // Initialize ActivityResultLauncher for permission requests
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openImageSelector();
                    } else {
                        Toast.makeText(this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Set up listeners
        uploadRemovePictureButton.setOnClickListener(v -> {
            String buttonText = uploadRemovePictureButton.getText().toString();
            if (buttonText.equals("Upload Picture")) {
                // Upload Picture functionality
                // Check and request permissions if necessary
                if (ContextCompat.checkSelfPermission(this, getReadPermission()) == PackageManager.PERMISSION_GRANTED) {
                    openImageSelector();
                } else {
                    // Request permission
                    requestPermissionLauncher.launch(getReadPermission());
                }
            } else if (buttonText.equals("Remove Picture")) {
                // Remove Picture functionality
                removeProfilePicture();
            }
        });

        saveButton.setOnClickListener(v -> saveUserInfo());

        // If in EDIT mode, load existing user data
        if (mode.equals(MODE_EDIT)) {
            loadUserData();
        } else {
            // In CREATE mode, set default UI state
            generateAndSetDefaultAvatar("");
            uploadRemovePictureButton.setText("Upload Picture");
        }
    }

    /**
     * Determines the appropriate read permission based on the device's Android version.
     *
     * @return The required read permission string.
     */
    private String getReadPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            return Manifest.permission.READ_EXTERNAL_STORAGE;
        }
    }

    /**
     * Launches the image selector to allow the user to choose a profile picture from their device.
     */
    private void openImageSelector() {
        // Launch the image picker
        selectImageLauncher.launch("image/*");
    }

    /**
     * Loads the existing user data from Firestore and pre-fills the input fields.
     */
    private void loadUserData() {
        saveButton.setEnabled(false); // Disable save button while loading
        firestore.collection(userType + "s").document(deviceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            nameEditText.setText(user.getName());
                            emailEditText.setText(user.getEmail());
                            phoneEditText.setText(user.getPhoneNumber());

                            existingImagePath = user.getImagePath(); // Store the existing image path

                            if (existingImagePath != null) {
                                StorageReference imageRef = storage.getReference().child(existingImagePath);
                                imageRef.getDownloadUrl()
                                        .addOnSuccessListener(uri -> {
                                            Picasso.get().load(uri).into(profileImageView);
                                            uploadRemovePictureButton.setText("Remove Picture");
                                        })
                                        .addOnFailureListener(e -> {
                                            // If failed to load, set default avatar and button text
                                            generateAndSetDefaultAvatar(user.getName());
                                            uploadRemovePictureButton.setText("Upload Picture");
                                            Log.e(TAG, "Failed to load profile image: " + e.getMessage());
                                        });
                            } else {
                                // No imagePath, generate default avatar
                                generateAndSetDefaultAvatar(user.getName());
                                uploadRemovePictureButton.setText("Upload Picture");
                            }
                        }
                    } else {
                        Toast.makeText(UserInfoActivity.this, "User data not found.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    saveButton.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserInfoActivity.this, "Error loading user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading user data", e);
                    saveButton.setEnabled(true);
                });
    }

    /**
     * Handles the process of saving or updating user information, including uploading a selected image
     * or handling image removal, and updating Firestore accordingly.
     */
    private void saveUserInfo() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phoneNumber = phoneEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable the save button to prevent multiple clicks
        saveButton.setEnabled(false);

        if (isProfilePictureRemoved) {
            // Handle profile picture removal
            if (existingImagePath != null) {
                // Delete existing image from Firebase Storage
                StorageReference imageRef = storage.getReference().child(existingImagePath);
                imageRef.delete()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Profile picture deleted successfully.");
                            // Generate and upload new default avatar
                            generateAndUploadDefaultAvatar(name, email, phoneNumber);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(UserInfoActivity.this, "Failed to delete profile picture: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Failed to delete profile picture", e);
                            saveButton.setEnabled(true);
                        });
            } else {
                // No existing image to delete, generate and upload default avatar
                generateAndUploadDefaultAvatar(name, email, phoneNumber);
            }
        }
        else if (selectedImageUri != null) {
            // Handle new image selected
            uploadImageToFirebase(name, email, phoneNumber);
        }
        else {
            // No changes to profile picture
            if (mode.equals(MODE_CREATE)) {
                // In CREATE mode, generate and upload default avatar
                generateAndUploadDefaultAvatar(name, email, phoneNumber);
            }
            else {
                // In EDIT mode, keep existing image (if any)
                updateUserInFirestore(name, email, phoneNumber, existingImagePath);
            }
        }
    }

    /**
     * Uploads the selected image to Firebase Storage and updates Firestore.
     *
     * @param name        The user's name.
     * @param email       The user's email address.
     * @param phoneNumber The user's phone number.
     */
    private void uploadImageToFirebase(String name, String email, String phoneNumber) {
        // Create a unique filename
        String fileName = "images/profile_pictures/" + userType + "/" + deviceId + "/" + UUID.randomUUID() + ".jpg";

        StorageReference storageRef = storage.getReference().child(fileName);

        // Upload the image
        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the storage path
                    String imagePath = storageRef.getPath();
                    // Delete existing image if it exists
                    if (existingImagePath != null) {
                        StorageReference existingImageRef = storage.getReference().child(existingImagePath);
                        existingImageRef.delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Existing image deleted successfully.");
                                    // Update Firestore with new image path
                                    updateUserInFirestore(name, email, phoneNumber, imagePath);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(UserInfoActivity.this, "Failed to delete existing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Failed to delete existing image", e);
                                    saveButton.setEnabled(true);
                                });
                    } else {
                        // No existing image, update Firestore with new image path
                        updateUserInFirestore(name, email, phoneNumber, imagePath);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserInfoActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Image upload failed", e);
                    saveButton.setEnabled(true);
                });
    }

    /**
     * Updates the user's information in Firestore without altering the eventsJoined list.
     *
     * @param name        The user's name.
     * @param email       The user's email address.
     * @param phoneNumber The user's phone number.
     * @param imagePath   The image path to save (can be null).
     */
    private void updateUserInFirestore(String name, String email, String phoneNumber, String imagePath) {
        firestore.collection(userType + "s").document(deviceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {

                            // Update fields with new values
                            user.setName(name);
                            user.setEmail(email);
                            user.setPhoneNumber(phoneNumber);
                            user.setImagePath(imagePath);

                            // Save back to Firestore
                            firestore.collection(userType + "s").document(deviceId)
                                    .set(user)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(UserInfoActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                        navigateToHomePage();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(UserInfoActivity.this, "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "Error updating profile", e);
                                        saveButton.setEnabled(true);
                                    });
                        }
                    } else {
                        Toast.makeText(UserInfoActivity.this, "User data not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserInfoActivity.this, "Error loading user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading user data", e);
                });
    }


    /**
     * Generates a default avatar, uploads it to Firebase Storage, and updates Firestore.
     *
     * @param name        The user's name.
     * @param email       The user's email address.
     * @param phoneNumber The user's phone number.
     */
    private void generateAndUploadDefaultAvatar(String name, String email, String phoneNumber) {
        // Generate initials from name
        String initials = getInitials(name);

        // Create a bitmap with a colored circle and initials
        Bitmap bitmap = createAvatarBitmap(initials);

        // Create a unique filename for the default avatar
        String fileName = "images/default_avatars/" + userType + "/" + deviceId + "/" + UUID.randomUUID() + ".png";
        StorageReference storageRef = storage.getReference().child(fileName);

        // Convert bitmap to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] data = baos.toByteArray();

        // Upload the bitmap
        storageRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the storage path
                    String avatarPath = storageRef.getPath();
                    // Update Firestore with new default avatar path
                    updateUserInFirestore(name, email, phoneNumber, avatarPath);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserInfoActivity.this, "Default avatar upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Default avatar upload failed", e);
                    saveButton.setEnabled(true);
                });
    }

    /**
     * Removes the profile picture by updating the UI and setting a flag.
     * The actual deletion will occur when the user clicks "Save".
     */
    private void removeProfilePicture() {
        String name = nameEditText.getText().toString().trim();
        // Set ImageView to default avatar
        generateAndSetDefaultAvatar(name);
        isProfilePictureRemoved = true;
        selectedImageUri = null; // Reset selected image
        // Change button text back to "Upload Picture"
        uploadRemovePictureButton.setText("Upload Picture");
    }

    /**
     * Generates and sets a default avatar locally without uploading to Firebase Storage.
     *
     * @param name The user's name.
     */
    private void generateAndSetDefaultAvatar(String name) {
        String initials = getInitials(name);
        Bitmap bitmap = createAvatarBitmap(initials);
        profileImageView.setImageBitmap(bitmap);
    }

    /**
     * Navigates the user to the appropriate home page based on their user type.
     * Closes the current activity after navigation.
     */
    private void navigateToHomePage() {
        if (userType.equals("Entrant")) {
            Intent intent = new Intent(UserInfoActivity.this, EntrantHomeActivity.class);
            startActivity(intent);
        } else if (userType.equals("Organizer")) {
            Intent intent = new Intent(UserInfoActivity.this, OrganizerHomeActivity.class);
            startActivity(intent);

        }
        finish(); // Close current activity
    }

    /**
     * Extracts up to two initials from the user's name.
     *
     * @param name The user's full name.
     * @return A string containing the initials in uppercase.
     */
    private String getInitials(String name) {
        String initials = "";
        String[] words = name.trim().split("\\s+");
        for (String word : words) {
            if (!word.isEmpty()) {
                initials += word.charAt(0);
                if (initials.length() == 2) break; // Limit to 2 initials
            }
        }
        return initials.toUpperCase();
    }

    /**
     * Creates a bitmap image featuring a colored circle with the user's initials centered on it.
     *
     * @param initials The user's initials.
     * @return A Bitmap object representing the generated avatar.
     */
    private Bitmap createAvatarBitmap(String initials) {
        int size = 200; // Size of the bitmap in pixels
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Draw colored circle
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(getColorFromName(initials));
        canvas.drawCircle(size / 2, size / 2, size / 2, paint);

        // Draw initials
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(80);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        // Adjust y-coordinate to center the text
        float x = size / 2;
        float y = size / 2 - ((textPaint.descent() + textPaint.ascent()) / 2);
        canvas.drawText(initials, x, y, textPaint);

        return bitmap;
    }

    /**
     * Generates a deterministic color based on the user's initials to ensure consistent avatar colors.
     *
     * @param initials The user's initials.
     * @return An integer representing the RGB color.
     */
    private int getColorFromName(String initials) {
        // Simple hash to generate a color from initials
        int hash = initials.hashCode();
        // Generate RGB components
        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = (hash & 0x0000FF);
        // Ensure the color is not too dark
        if (r + g + b < 300) { // Threshold can be adjusted
            r = (r + 100) % 256;
            g = (g + 100) % 256;
            b = (b + 100) % 256;
        }
        return Color.rgb(r, g, b);
    }
}
