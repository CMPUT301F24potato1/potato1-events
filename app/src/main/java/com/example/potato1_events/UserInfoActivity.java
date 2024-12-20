// File: UserInfoActivity.java
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
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Activity responsible for handling user information, including profile picture selection,
 * uploading images to Firebase Storage, generating default avatars, requesting location permissions,
 * fetching user location, and saving/updating user data to Firestore.
 */
public class UserInfoActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "UserInfoActivity";

    // Modes
    private static final String MODE_CREATE = "CREATE";
    private static final String MODE_EDIT = "EDIT";
    private boolean isAdmin = false; // Retrieved from Intent

    // Drawer Layout and Navigation View
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private androidx.appcompat.widget.Toolbar toolbar;

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
    private boolean isUsingDefaultAvatar = true; // Flag to indicate if using default avatar
    private String originalName = ""; // To track if the name has changed

    // ActivityResultLauncher for selecting image
    private ActivityResultLauncher<String> selectImageLauncher;

    // ActivityResultLauncher for requesting storage permissions
    private ActivityResultLauncher<String> requestStoragePermissionLauncher;

    // ActivityResultLauncher for location permissions
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    // Current mode: CREATE or EDIT
    private String mode;

    // ActionBarDrawerToggle
    private ActionBarDrawerToggle toggle;

    // Location Components
    private FusedLocationProviderClient fusedLocationClient;

    private List<String> existingEventsJoined = new ArrayList<>();


    // User Object
    private User currentUser; // To hold the user data being saved

    /**
     * Sets the FirebaseFirestore instance.
     * <p>
     * This method is primarily used for testing purposes.
     * </p>
     *
     * @param firestore The FirebaseFirestore instance to set.
     */
    @VisibleForTesting
    public void setFirestore(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Sets the FirebaseStorage instance.
     * <p>
     * This method is primarily used for testing purposes.
     * </p>
     *
     * @param storage The FirebaseStorage instance to set.
     */
    @VisibleForTesting
    public void setStorage(FirebaseStorage storage) {
        this.storage = storage;
    }

    /**
     * Called when the activity is starting. Initializes the activity, sets up UI components,
     * and handles mode-specific configurations.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        // Initialize Firebase Firestore and Storage
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Initialize Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.user_info_activity);
        navigationView = findViewById(R.id.nav_view);

        // Initialize Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("User Information");
        }

        // Setup ActionBarDrawerToggle
        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Set NavigationView listener
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize UI Components
        profileImageView = findViewById(R.id.profileImageView);
        uploadRemovePictureButton = findViewById(R.id.uploadPictureButton);
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        saveButton = findViewById(R.id.saveButton);

        // Get mode from intent
        Intent intent = getIntent();
        mode = intent.getStringExtra("MODE");

        isAdmin = intent.getBooleanExtra("IS_ADMIN", false);

        if (isAdmin) {
            navigationView.getMenu().findItem(R.id.nav_manage_media).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_manage_users).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_create_event).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_edit_facility).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_my_events).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_manage_events).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_manage_facilities).setVisible(true);
        }

        if (mode == null || (!mode.equals(MODE_CREATE) && !mode.equals(MODE_EDIT))) {
            Toast.makeText(this, "Invalid mode.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get device ID
        deviceId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);

        // Initialize ActivityResultLauncher for image selection
        selectImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        isProfilePictureRemoved = false; // Reset removal flag
                        isUsingDefaultAvatar = false; // Now using a custom image
                        // Display the selected image in ImageView
                        Picasso.get().load(uri).into(profileImageView);
                        // Change button text to "Remove Picture"
                        uploadRemovePictureButton.setText("Remove Picture");
                    }
                }
        );

        // Initialize ActivityResultLauncher for storage permissions
        requestStoragePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openImageSelector();
                    } else {
                        Toast.makeText(this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Initialize ActivityResultLauncher for location permissions
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    if (fineLocationGranted != null && fineLocationGranted) {
                        // Precise location access granted
                        if (currentUser != null) {
                            getLastLocation(currentUser);
                        }
                    } else if (coarseLocationGranted != null && coarseLocationGranted) {
                        // Only approximate location access granted
                        if (currentUser != null) {
                            getLastLocation(currentUser);
                        }
                    } else {
                        // No location access granted
                        Toast.makeText(this, "Location permission denied. Proceeding without location data.", Toast.LENGTH_SHORT).show();
                        if (currentUser != null) {
                            currentUser.setLatitude(null);
                            currentUser.setLongitude(null);
                            saveUserToFirestore(currentUser);
                        }
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
                    requestStoragePermissionLauncher.launch(getReadPermission());
                }
            } else if (buttonText.equals("Remove Picture")) {
                // Remove Picture functionality
                removeProfilePicture();
            }
        });

        saveButton.setOnClickListener(v -> saveUserInfo());

        // Load user data or set up for new user
        loadOrInitializeUserData();

        // Handle back press to close drawer if open
        handleBackPressed();

        // Disable the drawer if in MODE_CREATE
        if (mode.equals(MODE_CREATE)) {
            disableDrawer();
        } else {
            enableDrawer();
        }
        setupFirestoreListener(navigationView, toggle);
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
     * Loads the existing user data from Firestore or initializes for a new user.
     * Additionally, sets the state of the upload/remove picture button based on imagePath.
     */
    private void loadOrInitializeUserData() {
        saveButton.setEnabled(false); // Disable save button while loading
        firestore.collection("Users").document(deviceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // User data exists, proceed in EDIT mode
                        mode = MODE_EDIT;
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            existingEventsJoined = user.getEventsJoined();
                            if (existingEventsJoined == null) {
                                existingEventsJoined = new ArrayList<>();
                            }
                            nameEditText.setText(user.getName());
                            emailEditText.setText(user.getEmail());
                            phoneEditText.setText(user.getPhoneNumber());

                            originalName = user.getName(); // Store the original name

                            existingImagePath = user.getImagePath(); // Store the existing image path

                            if (existingImagePath != null) {
                                StorageReference imageRef = storage.getReference().child(existingImagePath);
                                imageRef.getDownloadUrl()
                                        .addOnSuccessListener(uri -> {
                                            Picasso.get().load(uri).into(profileImageView);
                                            if (existingImagePath.startsWith("/images/default")) {
                                                isUsingDefaultAvatar = true;
                                                uploadRemovePictureButton.setText("Upload Picture");
                                            } else {
                                                isUsingDefaultAvatar = false;
                                                uploadRemovePictureButton.setText("Remove Picture");
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            // If failed to load, set default avatar and button text
                                            generateAndSetDefaultAvatar(user.getName());
                                            isUsingDefaultAvatar = true;
                                            uploadRemovePictureButton.setText("Upload Picture");
                                            Log.e(TAG, "Failed to load profile image: " + e.getMessage());
                                        });
                            } else {
                                // No imagePath, generate default avatar
                                isUsingDefaultAvatar = true;
                                generateAndSetDefaultAvatar(user.getName());
                                uploadRemovePictureButton.setText("Upload Picture");
                            }
                        }
                    } else {
                        // User data does not exist, proceed in CREATE mode
                        mode = MODE_CREATE;
                        // In CREATE mode, set default UI state
                        isUsingDefaultAvatar = true;
                        generateAndSetDefaultAvatar("");
                        uploadRemovePictureButton.setText("Upload Picture");
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
     * Disables the navigation drawer by locking it and hiding the hamburger icon.
     */
    private void disableDrawer() {
        // Disable the drawer toggle
        toggle.setDrawerIndicatorEnabled(false);
        // Lock the drawer closed
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        // Remove the navigation icon
        toolbar.setNavigationIcon(null);
    }

    /**
     * Enables the navigation drawer by unlocking it and showing the hamburger icon.
     */
    private void enableDrawer() {
        // Enable the drawer toggle
        toggle.setDrawerIndicatorEnabled(true);
        // Unlock the drawer
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        // Restore the hamburger icon
        toggle.syncState();
    }

    /**
     * Handles the process of saving or updating user information, including uploading a selected image,
     * handling image removal, fetching location, and updating Firestore accordingly.
     */
    private void saveUserInfo() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phoneNumber = phoneEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter all required fields", Toast.LENGTH_SHORT).show();
            saveButton.setEnabled(true);
            return;
        }

        // Disable the save button to prevent multiple clicks
        saveButton.setEnabled(false);

        boolean nameChanged = !name.equals(originalName);

        if (isUsingDefaultAvatar && nameChanged) {
            // User changed their name and is using a default avatar
            // Delete the old default avatar and upload a new one
            if (existingImagePath != null && existingImagePath.startsWith("/images/default")) {
                StorageReference oldAvatarRef = storage.getReference().child(existingImagePath);
                oldAvatarRef.delete()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Old default avatar deleted successfully.");
                            // Generate and upload new default avatar
                            generateAndUploadDefaultAvatar(name, email, phoneNumber);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(UserInfoActivity.this, "Failed to delete old avatar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Failed to delete old avatar", e);
                            saveButton.setEnabled(true);
                        });
            } else {
                // No existing default avatar found, just generate a new one
                generateAndUploadDefaultAvatar(name, email, phoneNumber);
            }
        } else if (isProfilePictureRemoved) {
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
        } else if (selectedImageUri != null) {
            // Handle new image selected
            uploadImageToFirebase(name, email, phoneNumber);
        } else {
            // No changes to profile picture
            if (mode.equals(MODE_CREATE)) {
                // In CREATE mode, generate and upload default avatar
                generateAndUploadDefaultAvatar(name, email, phoneNumber);
            } else {
                // In EDIT mode, keep existing image (if any)
                saveOrUpdateUserInFirestore(name, email, phoneNumber, existingImagePath);
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
        String fileName = "images/profile_pictures/" + deviceId + "/" + UUID.randomUUID() + ".jpg";

        StorageReference storageRef = storage.getReference().child(fileName);

        // Upload the image
        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the storage path
                    String imagePath = storageRef.getPath();
                    // Delete existing image if it exists
                    if (existingImagePath != null && !existingImagePath.startsWith("/images/default")) {
                        StorageReference existingImageRef = storage.getReference().child(existingImagePath);
                        existingImageRef.delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Existing image deleted successfully.");
                                    // Initialize currentUser
                                    currentUser = new User();
                                    currentUser.setUserId(deviceId); // Assign device ID as userId
                                    currentUser.setName(name);
                                    currentUser.setEmail(email);
                                    currentUser.setPhoneNumber(phoneNumber);
                                    currentUser.setImagePath(imagePath);
                                    currentUser.setEventsJoined(existingEventsJoined); // Initialize eventsJoined

                                    // Proceed to fetch location and save
                                    fetchUserLocationAndSave(currentUser);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(UserInfoActivity.this, "Failed to delete existing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Failed to delete existing image", e);
                                    saveButton.setEnabled(true);
                                });
                    } else {
                        // No existing image or it's a default avatar, proceed to save
                        currentUser = new User();
                        currentUser.setUserId(deviceId); // Assign device ID as userId
                        currentUser.setName(name);
                        currentUser.setEmail(email);
                        currentUser.setPhoneNumber(phoneNumber);
                        currentUser.setImagePath(imagePath);
                        currentUser.setEventsJoined(new ArrayList<>()); // Initialize eventsJoined

                        // Proceed to fetch location and save
                        fetchUserLocationAndSave(currentUser);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserInfoActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Image upload failed", e);
                    saveButton.setEnabled(true);
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
        String fileName = "images/default_avatars/" + deviceId + "/" + UUID.randomUUID() + ".png";
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
                    // Initialize currentUser
                    currentUser = new User();
                    currentUser.setUserId(deviceId); // Assign device ID as userId
                    currentUser.setName(name);
                    currentUser.setEmail(email);
                    currentUser.setPhoneNumber(phoneNumber);
                    currentUser.setImagePath(avatarPath);
                    currentUser.setEventsJoined(existingEventsJoined); // Initialize eventsJoined

                    // Proceed to fetch location and save
                    fetchUserLocationAndSave(currentUser);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserInfoActivity.this, "Default avatar upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Default avatar upload failed", e);
                    saveButton.setEnabled(true);
                });
    }

    /**
     * Saves or updates the user's information in Firestore, including location data.
     *
     * @param name        The user's name.
     * @param email       The user's email address.
     * @param phoneNumber The user's phone number.
     * @param imagePath   The image path to save (can be null).
     */
    private void saveOrUpdateUserInFirestore(String name, String email, String phoneNumber, String imagePath) {
        currentUser = new User();
        currentUser.setUserId(deviceId); // Assign device ID as userId
        currentUser.setName(name);
        currentUser.setEmail(email);
        currentUser.setPhoneNumber(phoneNumber);
        currentUser.setImagePath(imagePath);
        currentUser.setEventsJoined(existingEventsJoined); // Initialize eventsJoined
        currentUser.setAdmin(isAdmin);

        // Proceed to fetch location and save
        fetchUserLocationAndSave(currentUser);
    }

    /**
     * Fetches the user's current location and updates the User object before saving to Firestore.
     *
     * @param user The User object to update with location data.
     */
    private void fetchUserLocationAndSave(User user) {
        // Check location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permissions are already granted
            getLastLocation(user);
        } else {
            // Request location permissions
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    /**
     * Retrieves the user's last known location and updates the User object.
     *
     * @param user The User object to update with location data.
     */
    private void getLastLocation(User user) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permissions are not granted
            Toast.makeText(this, "Location permissions are not granted.", Toast.LENGTH_SHORT).show();
            user.setLatitude(null);
            user.setLongitude(null);
            saveUserToFirestore(user);
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        android.location.Location location = task.getResult();
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        user.setLatitude(latitude);
                        user.setLongitude(longitude);
                    } else {
                        Toast.makeText(UserInfoActivity.this, "Unable to retrieve location. Please ensure location services are enabled.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to get location");
                        user.setLatitude(null);
                        user.setLongitude(null);
                    }
                    // Proceed to save user data
                    saveUserToFirestore(user);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserInfoActivity.this, "Unable to retrieve location. Proceeding without location data.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to get location", e);
                    user.setLatitude(null);
                    user.setLongitude(null);
                    saveUserToFirestore(user);
                });
    }

    /**
     * Saves the User object to Firestore.
     *
     * @param user The User object containing all user data.
     */
    private void saveUserToFirestore(User user) {
        firestore.collection("Users").document(deviceId)
                .set(user, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (mode.equals(MODE_CREATE)) {
                        Toast.makeText(UserInfoActivity.this, "Profile created successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(UserInfoActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    }
                    navigateToHomePage();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserInfoActivity.this, "Error saving profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error saving profile", e);
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
        isUsingDefaultAvatar = true;
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
     * Generates a pastel color based on the user's initials to ensure consistent avatar colors.
     *
     * @param initials The user's initials.
     * @return An integer representing the RGB color.
     */
    private int getColorFromName(String initials) {
        // Use initials to create a seed
        long seed = initials.hashCode();
        Random random = new Random(seed);

        // Generate hue between 0 and 360
        float hue = random.nextInt(360);

        // Pastel colors have low to medium saturation and high lightness
        float saturation = 0.4f + (random.nextFloat() * 0.2f); // 0.4 to 0.6
        float lightness = 0.8f + (random.nextFloat() * 0.1f);  // 0.8 to 0.9

        return hslToRgb(hue, saturation, lightness);
    }

    /**
     * Converts HSL color values to RGB.
     *
     * @param h Hue angle in degrees [0-360).
     * @param s Saturation [0-1].
     * @param l Lightness [0-1].
     * @return An integer RGB color.
     */
    private int hslToRgb(float h, float s, float l) {
        float c = (1 - Math.abs(2 * l - 1)) * s;
        float hPrime = h / 60;
        float x = c * (1 - Math.abs(hPrime % 2 - 1));
        float r1 = 0, g1 = 0, b1 = 0;

        if (0 <= hPrime && hPrime < 1) {
            r1 = c;
            g1 = x;
        } else if (1 <= hPrime && hPrime < 2) {
            r1 = x;
            g1 = c;
        } else if (2 <= hPrime && hPrime < 3) {
            g1 = c;
            b1 = x;
        } else if (3 <= hPrime && hPrime < 4) {
            g1 = x;
            b1 = c;
        } else if (4 <= hPrime && hPrime < 5) {
            r1 = x;
            b1 = c;
        } else if (5 <= hPrime && hPrime < 6) {
            r1 = c;
            b1 = x;
        }

        float m = l - c / 2;
        int r = Math.round((r1 + m) * 255);
        int g = Math.round((g1 + m) * 255);
        int b = Math.round((b1 + m) * 255);

        return Color.rgb(r, g, b);
    }

    /**
     * Navigates the user to the appropriate home page.
     * Since roles are removed, navigate to a single HomeActivity.
     * Closes the current activity after navigation.
     */
    private void navigateToHomePage() {
        Intent intent = new Intent(UserInfoActivity.this, EntrantHomeActivity.class);
        intent.putExtra("IS_ADMIN", isAdmin);
        startActivity(intent);
        finish(); // Close current activity
    }

    /**
     * Handles navigation menu item selections.
     *
     * @param item The selected menu item.
     * @return True if the event was handled, false otherwise.
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation
        int id = item.getItemId();
        Intent intent = null;

        if (id == R.id.nav_notifications) {
            // Navigate to NotificationsActivity
            // Uncomment and implement if NotificationsActivity exists
            intent = new Intent(UserInfoActivity.this, NotificationsActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_edit_profile) {
            Toast.makeText(this, "Already on this page.", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_manage_media) {
            // Navigate to ManageMediaActivity (visible only to admins)
            intent = new Intent(UserInfoActivity.this, ManageMediaActivity.class);
        } else if (id == R.id.nav_manage_users) {
            // Navigate to ManageUsersActivity (visible only to admins)
            intent = new Intent(UserInfoActivity.this, ManageUsersActivity.class);
        } else if (id == R.id.nav_manage_events) {
            intent = new Intent(UserInfoActivity.this, ManageEventsActivity.class);
        } else if (id == R.id.nav_manage_facilities) {
            intent = new Intent(UserInfoActivity.this, ManageFacilitiesActivity.class);
        } else if (id == R.id.action_scan_qr) {
            intent = new Intent(UserInfoActivity.this, QRScanActivity.class);
        } else if (id == R.id.nav_create_event) {
            intent = new Intent(UserInfoActivity.this, CreateEditEventActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_edit_facility) {
            intent = new Intent(UserInfoActivity.this, CreateEditFacilityActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_my_events) {
            intent = new Intent(UserInfoActivity.this, OrganizerHomeActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_view_joined_events) {
            intent = new Intent(UserInfoActivity.this, EntrantHomeActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        }

        if (intent != null) {
            startActivity(intent);
        }


        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * If back button is pressed and side bar is opened, then close the drawer.
     * Otherwise, perform the default back press action.
     */
    private void handleBackPressed() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled */) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    UserInfoActivity.super.onBackPressed();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void setupFirestoreListener(NavigationView navigationView, ActionBarDrawerToggle toggle) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("Users")
                .document(deviceId)
                .addSnapshotListener(this, (documentSnapshot, e) -> {
                    if (e != null) {
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        // Retrieve the 'admin' field from the user document
                        Boolean admin = documentSnapshot.getBoolean("admin");

                        if (admin != null && admin != isAdmin) {
                            // Update the isAdmin variable if there's a change
                            isAdmin = admin;

                            // Update the navigation menu based on the new isAdmin value
                            runOnUiThread(() -> {
                                if (isAdmin) {
                                    // Show admin-specific menu items
                                    navigationView.getMenu().findItem(R.id.nav_manage_media).setVisible(true);
                                    navigationView.getMenu().findItem(R.id.nav_manage_users).setVisible(true);
                                    navigationView.getMenu().findItem(R.id.nav_manage_facilities).setVisible(true);
                                    navigationView.getMenu().findItem(R.id.nav_manage_events).setVisible(true);
                                } else {
                                    // Hide admin-specific menu items
                                    navigationView.getMenu().findItem(R.id.nav_manage_media).setVisible(false);
                                    navigationView.getMenu().findItem(R.id.nav_manage_users).setVisible(false);
                                    navigationView.getMenu().findItem(R.id.nav_manage_facilities).setVisible(false);
                                    navigationView.getMenu().findItem(R.id.nav_manage_events).setVisible(false);
                                }
                                // Sync the toggle state to reflect menu changes
                                toggle.syncState();
                            });
                        }
                    }
                });
    }
}
