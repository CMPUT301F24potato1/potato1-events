// File: ManageMediaActivity.java
package com.example.potato1_events;

import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.ListResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for admins to manage media files.
 * Allows viewing and deleting images stored in Firebase Storage.
 */
public class ManageMediaActivity extends AppCompatActivity implements MediaAdapter.OnMediaClickListener {

    // UI Components

    /**
     * RecyclerView to display the list of media files.
     */
    private RecyclerView mediaRecyclerView;

    /**
     * Adapter for the RecyclerView to manage media data.
     */
    private MediaAdapter mediaAdapter;

    /**
     * List holding all media StorageReferences fetched from Firebase Storage.
     */
    private List<StorageReference> mediaList;

    // Firebase Firestore

    /**
     * FirebaseStorage instance for interacting with Firebase Storage.
     */
    private FirebaseStorage firebaseStorage;

    /**
     * Called when the activity is first created.
     * Initializes UI components, Firebase instances, and loads media files.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_media);

        // Initialize Firebase Storage
        firebaseStorage = FirebaseStorage.getInstance();

        // Initialize UI Components
        Toolbar toolbar = findViewById(R.id.toolbar_manage_media);
        setSupportActionBar(toolbar);

        // Enable the Up button for navigation
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        actionBar.setTitle("Manage Media");

        mediaRecyclerView = findViewById(R.id.mediaRecyclerView);
        mediaRecyclerView.setLayoutManager(new GridLayoutManager(this, 3)); // 3 columns grid
        mediaRecyclerView.setHasFixedSize(true);

        mediaList = new ArrayList<>();
        mediaAdapter = new MediaAdapter(mediaList, this);
        mediaRecyclerView.setAdapter(mediaAdapter);

        // Load media from Firebase Storage
        loadMedia();
    }

    /**
     * Loads all media files from Firebase Storage under the 'images/' directory and its subdirectories.
     * Populates the RecyclerView with the fetched media.
     */
    private void loadMedia() {
        StorageReference imagesRef = firebaseStorage.getReference().child("images/");

        // Clear existing list and notify adapter
        mediaList.clear();
        mediaAdapter.notifyDataSetChanged();

        // Start recursive listing of media files
        listAllMedia(imagesRef);
    }

    /**
     * Recursively lists all media files under the given StorageReference.
     *
     * @param ref The StorageReference to start listing from.
     */
    private void listAllMedia(StorageReference ref) {
        ref.listAll()
                .addOnSuccessListener(listResult -> {
                    // Add all items (files) to the media list
                    mediaList.addAll(listResult.getItems());

                    // Recursively list all prefixes (subdirectories)
                    for (StorageReference prefix : listResult.getPrefixes()) {
                        listAllMedia(prefix);
                    }

                    // After listing, notify adapter
                    mediaAdapter.notifyDataSetChanged();
                    Toast.makeText(ManageMediaActivity.this, "Loaded Media: " + mediaList.size(), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(exception -> {
                    Toast.makeText(ManageMediaActivity.this, "Error loading media: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Handles the selection of menu items in the action bar.
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

    /**
     * Handles the delete action initiated from the MediaAdapter.
     * Prompts the admin to confirm deletion of the selected media.
     *
     * @param storageRef The StorageReference of the media to delete.
     */
    @Override
    public void onDeleteClick(StorageReference storageRef) {
        confirmDelete(storageRef);
    }

    /**
     * Prompts the admin to confirm deletion of a media file.
     *
     * @param mediaRef The StorageReference of the media to delete.
     */
    private void confirmDelete(StorageReference mediaRef) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Media")
                .setMessage("Are you sure you want to delete this media?")
                .setPositiveButton("Yes", (dialog, which) -> deleteMedia(mediaRef))
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Deletes the specified media file from Firebase Storage.
     *
     * @param mediaRef The StorageReference of the media to delete.
     */
    private void deleteMedia(StorageReference mediaRef) {
        mediaRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ManageMediaActivity.this, "Media deleted successfully.", Toast.LENGTH_SHORT).show();
                    loadMedia(); // Refresh the media list
                })
                .addOnFailureListener(exception -> {
                    Toast.makeText(ManageMediaActivity.this, "Error deleting media: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
