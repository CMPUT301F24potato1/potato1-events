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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

// Import for image loading
import com.squareup.picasso.Picasso;

// Import for CircleImageView
import de.hdodenhof.circleimageview.CircleImageView;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

/**
 * Activity responsible for handling user information, including profile picture selection,
 * uploading images to Firebase Storage, generating default avatars, and saving user data to Firestore.
 */
public class UserInfoActivity extends AppCompatActivity {

    private static final String TAG = "UserInfoActivity";

    private String userType;
    private String deviceId;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    private CircleImageView profileImageView;
    private Button uploadPictureButton;
    private EditText nameEditText, emailEditText, phoneEditText;
    private Button saveButton;

    private Uri selectedImageUri = null; // To store the selected image URI

    // ActivityResultLauncher for selecting image
    private ActivityResultLauncher<String> selectImageLauncher;

    // ActivityResultLauncher for requesting permissions
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_info);

        // Get user type from intent
        userType = getIntent().getStringExtra("USER_TYPE");

        // Get device ID
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Initialize Firebase Firestore and Storage
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Initialize views
        profileImageView = findViewById(R.id.profileImageView);
        uploadPictureButton = findViewById(R.id.uploadPictureButton);
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
                        // Display the selected image in ImageView
                        Picasso.get().load(uri).into(profileImageView);
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
        uploadPictureButton.setOnClickListener(v -> {
            // Check and request permissions if necessary
            if (ContextCompat.checkSelfPermission(this, getReadPermission()) == PackageManager.PERMISSION_GRANTED) {
                openImageSelector();
            } else {
                // Request permission
                requestPermissionLauncher.launch(getReadPermission());
            }
        });

        saveButton.setOnClickListener(v -> saveUserInfo());
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
     * Handles the process of saving user information, including uploading a selected image
     * or generating a default avatar if no image is selected.
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

        if (selectedImageUri != null) {
            // User has selected an image, upload it to Firebase Storage
            uploadImageToFirebase(name, email, phoneNumber);
        } else {
            // No image selected, generate a deterministic avatar
            generateDefaultAvatarAndSaveUser(name, email, phoneNumber);
        }
    }

    /**
     * Uploads the selected image to Firebase Storage and retrieves its storage path.
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
                    // Instead of getting the download URL, use the storage path
                    String imagePath = storageRef.getPath();
                    saveUserToFirestore(name, email, phoneNumber, imagePath);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserInfoActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    saveButton.setEnabled(true);
                });
    }

    /**
     * Generates a default avatar based on the user's name, uploads it to Firebase Storage,
     * and saves the user information to Firestore.
     *
     * @param name        The user's name.
     * @param email       The user's email address.
     * @param phoneNumber The user's phone number.
     */
    private void generateDefaultAvatarAndSaveUser(String name, String email, String phoneNumber) {
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
                    // Instead of getting the download URL, use the storage path
                    String avatarPath = storageRef.getPath();
                    // Save user info with avatar path
                    saveUserToFirestore(name, email, phoneNumber, avatarPath);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserInfoActivity.this, "Default avatar upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    saveUserToFirestore(name, email, phoneNumber, null); // Save without avatar path
                });

        // Return a placeholder or null since the actual path will be set asynchronously
        // No return statement needed as method is void
    }

    /**
     * Saves the user's information to Firestore, including the profile picture storage path.
     *
     * @param name        The user's name.
     * @param email       The user's email address.
     * @param phoneNumber The user's phone number.
     * @param imagePath   The storage path of the profile picture.
     */
    private void saveUserToFirestore(String name, String email, String phoneNumber, String imagePath) {
        String userId = UUID.randomUUID().toString();
        // Create user object
        User user = new User(
                userId,
                userType,
                name,
                email,
                phoneNumber,
                imagePath, // holds the storage path in firebase storage
                true, // Default to notifications enabled
                System.currentTimeMillis() // Current timestamp
        );

        // Save to Firestore
        firestore.collection(userType + "s").document(deviceId).set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(UserInfoActivity.this, "User information saved", Toast.LENGTH_SHORT).show();
                    navigateToHomePage();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserInfoActivity.this, "Error saving user information: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    saveButton.setEnabled(true);
                });
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
            // Placeholder for OrganizerHomeActivity
            // Intent intent = new Intent(UserInfoActivity.this, OrganizerHomeActivity.class);
            // startActivity(intent);
            Toast.makeText(this, "Organizer Home Page not implemented yet.", Toast.LENGTH_SHORT).show();
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
