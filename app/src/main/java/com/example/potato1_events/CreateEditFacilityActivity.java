// File: CreateEditFacilityActivity.java
package com.example.potato1_events;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import java.util.*;

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
    private String facilityId = null; // Document ID

    // ActivityResultLaunchers
    private ActivityResultLauncher<String> selectImageLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_edit_facility);

        // Initialize Firebase Instances
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

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
        drawerLayout = findViewById(R.id.drawer_organizer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Set up ActionBarDrawerToggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Initialize ActivityResultLauncher for image selection
        selectImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedFacilityPhotoUri = uri;
                        // Display the selected image in ImageView
                        Picasso.get().load(uri).into(facilityPhotoView);
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

        // Determine if we're editing an existing facility or creating a new one
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("facilityId")) {
            // Editing an existing facility
            facilityId = intent.getStringExtra("facilityId");
            loadFacilityData();
        } else {
            // Creating a new facility
            // Generate a unique document ID using Firestore's auto-generated ID
            DocumentReference newFacilityRef = firestore.collection("Facilities").document();
            facilityId = newFacilityRef.getId();
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
                        Facility facility = documentSnapshot.toObject(Facility.class);
                        if (facility != null) {
                            facilityNameEditText.setText(facility.getFacilityName());
                            facilityAddressEditText.setText(facility.getFacilityAddress());
                            facilityDescriptionEditText.setText(facility.getFacilityDescription());

                            if (!TextUtils.isEmpty(facility.getFacilityPhotoUrl())) {
                                facilityPhotoUrl = facility.getFacilityPhotoUrl();
                                Picasso.get().load(facilityPhotoUrl).into(facilityPhotoView);
                            }
                        }
                    }
                    // If document does not exist, it's a new facility
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

        // Input validation
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(address) || TextUtils.isEmpty(description)) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable the save button to prevent multiple clicks
        saveFacilityButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        // Handle facility photo upload
        if (selectedFacilityPhotoUri != null) {
            uploadFacilityPhoto(name, address, description);
        } else {
            // If no new photo is selected, check if an existing photo URL exists
            if (facilityPhotoUrl != null) {
                saveFacilityToFirestore(name, address, description, facilityPhotoUrl);
            } else {
                // Photo is mandatory; prompt the user to upload
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Please upload a facility photo.", Toast.LENGTH_SHORT).show();
                saveFacilityButton.setEnabled(true);
            }
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
        // Create a unique filename
        String fileName = "facility_photos/" + UUID.randomUUID() + ".jpg";
        StorageReference storageRef = storage.getReference().child(fileName);

        // Upload the image
        storageRef.putFile(selectedFacilityPhotoUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL
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
        // Create a Facility object
        Facility facility = new Facility();
        facility.setId(facilityId);
        facility.setFacilityName(name);
        facility.setFacilityAddress(address);
        facility.setFacilityDescription(description);
        facility.setFacilityPhotoUrl(photoUrl);
        facility.setEventIds(new ArrayList<>()); // Initialize with empty list
        facility.setCreatedAt(new Date());

        DocumentReference facilityRef = firestore.collection("Facilities").document(facilityId);

        // Save or update the facility document
        facilityRef.set(facility)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(CreateEditFacilityActivity.this, "Facility saved successfully!", Toast.LENGTH_SHORT).show();
                    navigateBackToPrevious();
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
        // Implement navigation logic, e.g., finish() or start a new Activity
        finish();
    }

    /**
     * Handles navigation item selections.
     *
     * @param item The selected menu item.
     * @return True if the event was handled, false otherwise.
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation
        int id = item.getItemId();

        if (id == R.id.nav_organizer_profile) {
            //FIXME Implement this
            // Navigate to Organizer Profile Activity
            //Intent intent = new Intent(CreateEditFacilityActivity.this, OrganizerProfileActivity.class);
            //startActivity(intent);
        } else if (id == R.id.nav_create_event) {
            Intent intent = new Intent(CreateEditFacilityActivity.this, CreateEditEventActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_edit_facility) {
            Toast.makeText(this, "Already on this page.", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_my_events) {
            Intent intent = new Intent(CreateEditFacilityActivity.this, OrganizerHomeActivity.class);
            startActivity(intent);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Handles the back button press to close the drawer if it's open.
     */
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
