// File: ManageFacilitiesActivity.java
package com.example.potato1_events;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for managing facilities.
 * Displays a list of facilities with options to delete each facility.
 * Includes a back button in the toolbar to navigate back.
 */
public class ManageFacilitiesActivity extends AppCompatActivity {

    private RecyclerView facilitiesRecyclerView;
    private FacilityAdapter facilityAdapter;
    private List<Facility> facilityList;
    private FirebaseFirestore firestore;

    private Toolbar toolbar;

    // Organizer's deviceId acting as facilityId
    private String deviceId;

    // User Privileges
    private boolean isAdmin = false; // Retrieved from Intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_facilities);

        // Retrieve the isAdmin flag from Intent extras
        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);

        // Initialize Firebase Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize deviceId
        deviceId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        // Initialize UI components
        toolbar = findViewById(R.id.toolbar_manage_facilities);
        setSupportActionBar(toolbar);

        // Enable the Up button for navigation
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        actionBar.setTitle("Manage Facilities");

        // Initialize RecyclerView
        facilitiesRecyclerView = findViewById(R.id.facilitiesRecyclerView);
        facilitiesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        facilitiesRecyclerView.setHasFixedSize(true);

        // Initialize facility list and adapter
        facilityList = new ArrayList<>();
        facilityAdapter = new FacilityAdapter(this, facilityList);
        facilitiesRecyclerView.setAdapter(facilityAdapter);

        // Fetch facilities from Firestore
        fetchFacilities();
    }

    /**
     * Fetches facilities from the "Facilities" collection in Firestore.
     */
    private void fetchFacilities() {
        firestore.collection("Facilities")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<DocumentSnapshot> documents = queryDocumentSnapshots.getDocuments();
                        facilityList.clear(); // Clear existing list
                        for (DocumentSnapshot document : documents) {
                            Facility facility = document.toObject(Facility.class);
                            if (facility != null) {
                                facilityList.add(facility);
                            }
                        }
                        facilityAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "No facilities found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch facilities: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Handles selection of the Up (back) button in the toolbar.
     *
     * @param item The selected menu item.
     * @return True if the event was handled, false otherwise.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle Up button presses
        if (item.getItemId() == android.R.id.home) {
            finish(); // Closes the current activity and returns to the parent
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
