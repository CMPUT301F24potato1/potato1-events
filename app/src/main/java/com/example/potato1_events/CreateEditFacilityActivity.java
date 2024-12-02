// File: CreateEditFacilityActivity.java
package com.example.potato1_events;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import java.util.*;

/**
 * Activity to create or edit a facility within the application.
 * Handles user input, data validation, image uploads, and interaction with Firebase services.
 * Integrates the navigation drawer with admin functionalities.
 */
public class CreateEditFacilityActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // UI Components
    private EditText facilityNameEditText, facilityAddressEditText, facilityDescriptionEditText;
    private Button uploadFacilityPhotoButton, saveFacilityButton;
    private ImageView facilityPhotoView;
    private ProgressBar progressBar;

    // Firebase Instances
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    // Navigation Drawer Components
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    // Image Selection Variables
    private Uri selectedFacilityPhotoUri = null;
    private String facilityPhotoUrl = null;

    // Facility Identification
    private String facilityId = null; // Document ID set to deviceId

    // ActivityResultLaunchers
    private ActivityResultLauncher<String> selectImageLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    // Organizer's deviceId
    private String deviceId;

    // Placeholder Image URL (Set this to your actual placeholder image URL)
    private static final String PLACEHOLDER_IMAGE_URL = "https://firebasestorage.googleapis.com/v0/b/your-app-id.appspot.com/o/facility_photos%2Fplaceholder_facility.jpg?alt=media&token=your-token";

    // User Privileges
    private boolean isAdmin = false; // Retrieved from Intent

    /**
     * Initializes the activity, sets up UI components, Firebase instances, and event listeners.
     *
     * @param savedInstanceState The previously saved state of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_edit_facility);

        // Retrieve the isAdmin flag from Intent extras
        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);

        // Initialize Firebase Instances
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Retrieve deviceId
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        facilityId = deviceId; // Set facilityId to deviceId to enforce 1-to-1

        // Initialize UI Components
        facilityNameEditText = findViewById(R.id.facilityNameEditText);
        facilityAddressEditText = findViewById(R.id.facilityAddressEditText);
        facilityDescriptionEditText = findViewById(R.id.facilityDescriptionEditText);
        uploadFacilityPhotoButton = findViewById(R.id.uploadFacilityPhotoButton);
        saveFacilityButton = findViewById(R.id.save);
        facilityPhotoView = findViewById(R.id.facilityPhotoView);
        progressBar = findViewById(R.id.progressBar);

        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Create/Edit Facility");

        // Initialize DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawer_layout); // Updated to use the same drawer as CreateEditEventActivity
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Set up ActionBarDrawerToggle for navigation drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Adjust Navigation Drawer Menu Items Based on isAdmin
        if (isAdmin) {
            navigationView.getMenu().findItem(R.id.nav_manage_media).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_manage_users).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_create_event).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_edit_facility).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_my_events).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_manage_events).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_manage_facilities).setVisible(true);
        }

        // Initialize ActivityResultLauncher for image selection
        selectImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedFacilityPhotoUri = uri;
                        // Display the selected image in ImageView
                        Picasso.get().load(uri).into(facilityPhotoView);
                        uploadFacilityPhotoButton.setText("Change Facility Photo"); // Update button text
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
        uploadFacilityPhotoButton.setOnClickListener(v -> {
            // Check and request permissions if necessary
            if (ContextCompat.checkSelfPermission(this, getReadPermission()) == PackageManager.PERMISSION_GRANTED) {
                openImageSelector();
            } else {
                // Request permission
                requestPermissionLauncher.launch(getReadPermission());
            }
        });

        saveFacilityButton.setOnClickListener(v -> saveFacility());

        // Load existing facility data if it exists
        loadFacilityData();
        handleBackPressed();
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
     * Launches the image selector to allow the user to choose a facility photo from their device.
     */
    private void openImageSelector() {
        // Launch the image picker
        selectImageLauncher.launch("image/*");
    }

    /**
     * Loads existing facility data into the UI for editing.
     */
    private void loadFacilityData() {
        progressBar.setVisibility(View.VISIBLE);
        DocumentReference facilityRef = firestore.collection("Facilities").document(facilityId);
        facilityRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        // Convert the document snapshot to a Facility object
                        Facility facility = documentSnapshot.toObject(Facility.class);
                        if (facility != null) {
                            // Populate the UI fields with existing data
                            facilityNameEditText.setText(facility.getFacilityName());
                            facilityAddressEditText.setText(facility.getFacilityAddress());
                            facilityDescriptionEditText.setText(facility.getFacilityDescription());

                            // Load and display the facility photo if available
                            if (!TextUtils.isEmpty(facility.getFacilityPhotoUrl())) {
                                facilityPhotoUrl = facility.getFacilityPhotoUrl();
                                Picasso.get().load(facilityPhotoUrl).into(facilityPhotoView);
                                uploadFacilityPhotoButton.setText("Change Facility Photo"); // Update button text
                            }
                        }
                    } else {
                        // No existing facility; setup for creation
                        uploadFacilityPhotoButton.setText("Upload Facility Photo");
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load facility data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Saves the facility to Firebase Firestore, handling both creation and updating.
     */
    private void saveFacility() {
        String name = facilityNameEditText.getText().toString().trim();
        String address = facilityAddressEditText.getText().toString().trim();
        String description = facilityDescriptionEditText.getText().toString().trim();

        // Input validation to ensure all required fields are filled
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(address) || TextUtils.isEmpty(description)) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable the save button and show progress bar to indicate ongoing operation
        saveFacilityButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        // Handle facility photo upload if a new photo is selected
        if (selectedFacilityPhotoUri != null) {
            uploadFacilityPhoto(name, address, description);
        } else {
            // If no new photo is selected, assign the placeholder image URL or retain existing
            if (facilityPhotoUrl == null) {
                facilityPhotoUrl = PLACEHOLDER_IMAGE_URL;
            }
            saveFacilityToFirestore(name, address, description, facilityPhotoUrl);
        }
    }

    /**
     * Uploads the selected facility photo to Firebase Storage and retrieves its storage path.
     *
     * @param name        Facility name.
     * @param address     Facility address.
     * @param description Facility description.
     */
    private void uploadFacilityPhoto(String name, String address, String description) {
        // Create a unique filename for the facility photo
        String fileName = "images/facility_photos/" + facilityId + "/" + UUID.randomUUID() + ".jpg";
        StorageReference storageRef = storage.getReference().child(fileName);

        // Upload the image to Firebase Storage
        storageRef.putFile(selectedFacilityPhotoUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL of the uploaded image
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        facilityPhotoUrl = uri.toString();
                        // Proceed to save the facility with the photo URL
                        saveFacilityToFirestore(name, address, description, facilityPhotoUrl);
                    }).addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(CreateEditFacilityActivity.this, "Failed to get photo URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        saveFacilityButton.setEnabled(true);
                    });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(CreateEditFacilityActivity.this, "Photo upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    saveFacilityButton.setEnabled(true);
                });
    }

    /**
     * Saves the facility data to Firebase Firestore.
     *
     * @param name        Facility name.
     * @param address     Facility address.
     * @param description Facility description.
     * @param photoUrl    URL of the uploaded facility photo.
     */
    private void saveFacilityToFirestore(String name, String address, String description, String photoUrl) {
        // Create a Facility object with the provided details
        Facility facility = new Facility();
        facility.setId(facilityId);
        facility.setFacilityName(name);
        facility.setFacilityAddress(address);
        facility.setFacilityDescription(description);
        facility.setFacilityPhotoUrl(photoUrl);
        facility.setEventIds(new ArrayList<>()); // Initialize with empty list
        facility.setCreatedAt(new Date());

        // Reference to the facility document in Firestore
        DocumentReference facilityRef = firestore.collection("Facilities").document(facilityId);

        // Save or update the facility document in Firestore
        facilityRef.set(facility)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(CreateEditFacilityActivity.this, "Facility saved successfully!", Toast.LENGTH_SHORT).show();
                    navigateBackToPrevious(); // Navigate back after successful save
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(CreateEditFacilityActivity.this, "Failed to save facility: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    saveFacilityButton.setEnabled(true);
                });
    }

    /**
     * Navigates back to the previous activity after saving/updating.
     */
    private void navigateBackToPrevious() {
        Intent intent = new Intent(CreateEditFacilityActivity.this, OrganizerHomeActivity.class);
        // Pass isAdmin flag to OrganizerHomeActivity
        intent.putExtra("IS_ADMIN", isAdmin);
        startActivity(intent);
        finish();
    }

    /**
     * Handles navigation menu item selections.
     *
     * @param item The selected menu item.
     * @return True if the event was handled, false otherwise.
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation menu item selections
        int id = item.getItemId();
        Intent intent = null;

        if (id == R.id.nav_notifications) {
            // Navigate to NotificationsActivity
            // Uncomment and implement if NotificationsActivity exists
            intent = new Intent(CreateEditFacilityActivity.this, NotificationsActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_edit_profile) {
            // Navigate to UserInfoActivity in EDIT mode
            intent = new Intent(CreateEditFacilityActivity.this, UserInfoActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
            intent.putExtra("MODE", "EDIT");
        } else if (id == R.id.nav_manage_media) {
            // Navigate to ManageMediaActivity (visible only to admins)
            intent = new Intent(CreateEditFacilityActivity.this, ManageMediaActivity.class);
        } else if (id == R.id.nav_manage_users) {
            // Navigate to ManageUsersActivity (visible only to admins)
            intent = new Intent(CreateEditFacilityActivity.this, ManageUsersActivity.class);
        } else if (id == R.id.nav_manage_events) {
            // Navigate to ManageEventsActivity
            intent = new Intent(CreateEditFacilityActivity.this, ManageEventsActivity.class);
        } else if (id == R.id.nav_manage_facilities) {
            // Navigate to ManageFacilitiesActivity
            intent = new Intent(CreateEditFacilityActivity.this, ManageFacilitiesActivity.class);
        } else if (id == R.id.action_scan_qr) {
            // Navigate to QRScanActivity
            intent = new Intent(CreateEditFacilityActivity.this, QRScanActivity.class);
        } else if (id == R.id.nav_create_event) {
            // Navigate to CreateEditEventActivity
            intent = new Intent(CreateEditFacilityActivity.this, CreateEditEventActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_edit_facility) {
            // Already on this page
            Toast.makeText(this, "Already on this page.", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_my_events) {
            // Navigate to OrganizerHomeActivity
            intent = new Intent(CreateEditFacilityActivity.this, OrganizerHomeActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_view_joined_events) {
            // Navigate to EntrantHomeActivity
            intent = new Intent(CreateEditFacilityActivity.this, EntrantHomeActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        }

        if (intent != null) {
            startActivity(intent);
        }

        // Close the navigation drawer after selection
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Handles the back button press to close the navigation drawer if it's open.
     */
    private void handleBackPressed() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled */) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    // If the drawer is not open, proceed with the default back behavior
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    /**
     * Sets up a Firestore listener to monitor changes in the user's admin status.
     *
     * @param navigationView The NavigationView to update menu items.
     * @param toggle         The ActionBarDrawerToggle to sync state.
     */
    private void setupFirestoreListener(NavigationView navigationView, ActionBarDrawerToggle toggle) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("Users")
                .document(deviceId)
                .addSnapshotListener(this, (documentSnapshot, e) -> {
                    if (e != null) {
                        // Log the error or handle it as needed
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
