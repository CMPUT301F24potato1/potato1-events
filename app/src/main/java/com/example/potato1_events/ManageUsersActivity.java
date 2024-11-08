// File: ManageUsersActivity.java
package com.example.potato1_events;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for admins to manage user profiles.
 * Allows viewing and deleting users from the "Entrants" and "Organizers" collections in Firestore.
 * Also handles the deletion of user profile images from Firebase Storage and their removal from event waiting lists.
 */
public class ManageUsersActivity extends AppCompatActivity {

    // UI Components

    /**
     * RecyclerView to display the list of users.
     */
    private RecyclerView usersRecyclerView;

    /**
     * Adapter for the RecyclerView to manage user data.
     */
    private UsersAdapter usersAdapter;

    /**
     * List holding all User objects fetched from Firestore.
     */
    private List<User> userList;

    // Firebase Firestore

    /**
     * FirebaseFirestore instance for database interactions.
     */
    private FirebaseFirestore firestore;

    // Tag for logging

    /**
     * Tag used for logging within this activity.
     */
    private static final String TAG = "ManageUsersActivity";

    /**
     * Called when the activity is first created.
     * Initializes UI components, Firebase instances, and loads user data.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI Components
        Toolbar toolbar = findViewById(R.id.toolbar_manage_users);
        setSupportActionBar(toolbar);

        // Enable the Up button for navigation
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Initialize RecyclerView
        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        usersRecyclerView.setHasFixedSize(true);

        // Initialize user list and adapter
        userList = new ArrayList<>();
        usersAdapter = new UsersAdapter(userList);
        usersRecyclerView.setAdapter(usersAdapter);

        // Load users from Firestore
        loadUsers();
    }

    /**
     * Loads all users from "Entrants" and "Organizers" collections in Firestore.
     * Populates the RecyclerView with the fetched user data.
     */
    private void loadUsers() {
        CollectionReference entrantsRef = firestore.collection("Entrants");
        CollectionReference organizersRef = firestore.collection("Organizers");

        // Create tasks for both queries
        Task<QuerySnapshot> entrantsTask = entrantsRef.get();
        Task<QuerySnapshot> organizersTask = organizersRef.get();

        // Use Tasks.whenAllSuccess to wait for both queries to complete successfully
        Tasks.whenAllSuccess(entrantsTask, organizersTask)
                .addOnSuccessListener(results -> {
                    userList.clear(); // Clear existing users to avoid duplication

                    for (Object result : results) {
                        if (result instanceof QuerySnapshot) {
                            QuerySnapshot querySnapshot = (QuerySnapshot) result;
                            for (DocumentSnapshot doc : querySnapshot) {
                                User user = doc.toObject(User.class);
                                if (user != null) {
                                    userList.add(user);
                                }
                            }
                        }
                    }

                    usersAdapter.notifyDataSetChanged(); // Refresh the RecyclerView

                    if (userList.isEmpty()) {
                        Toast.makeText(ManageUsersActivity.this, "No users found.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ManageUsersActivity.this, "Loaded Users: " + userList.size(), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ManageUsersActivity.this, "Error loading users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading users", e);
                });
    }

    /**
     * Handles the selection of menu items in the action bar.
     * Specifically handles the Up button to navigate back to the parent activity.
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
     * Adapter class for managing and displaying a list of users in the RecyclerView.
     * Binds user data to the UI components and handles user interactions such as deletion.
     */
    private class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

        // List of users to display
        private List<User> users;

        /**
         * Constructs a UsersAdapter with the specified list of users.
         *
         * @param users The list of User objects to display.
         */
        UsersAdapter(List<User> users) {
            this.users = users;
        }

        /**
         * Called when RecyclerView needs a new {@link UserViewHolder} of the given type to represent an item.
         *
         * @param parent   The ViewGroup into which the new View will be added after it is bound to an adapter position.
         * @param viewType The view type of the new View.
         * @return A new UserViewHolder that holds a View of the given view type.
         */
        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Inflate the item_user layout
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(view);
        }

        /**
         * Called by RecyclerView to display the data at the specified position.
         * Updates the contents of the UserViewHolder to reflect the user at the given position.
         *
         * @param holder   The UserViewHolder which should be updated to represent the contents of the item at the given position in the data set.
         * @param position The position of the item within the adapter's data set.
         */
        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            User user = users.get(position);

            // Load profile image
            if (!TextUtils.isEmpty(user.getImagePath())) {
                // Construct the full URL or use Firebase Storage's getDownloadUrl()
                StorageReference imageRef = FirebaseStorage.getInstance().getReference().child(user.getImagePath());
                imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Picasso.get()
                            .load(uri)
                            .placeholder(R.drawable.ic_placeholder_image) // Placeholder image while loading
                            .error(R.drawable.ic_error_image)             // Image to display on error
                            .into(holder.profileImageView);
                }).addOnFailureListener(e -> {
                    holder.profileImageView.setImageResource(R.drawable.ic_error_image); // Set error image on failure
                });
            } else {
                // Set a default placeholder image if imagePath is empty
                holder.profileImageView.setImageResource(R.drawable.ic_placeholder_image);
            }

            // Bind user data to TextViews
            holder.nameTextView.setText(user.getName());
            holder.emailTextView.setText(user.getEmail());
            holder.phoneTextView.setText(user.getPhoneNumber());

            // Set Delete Button Listener
            holder.deleteButton.setOnClickListener(v -> {
                confirmDeleteUser(user);
            });
        }

        /**
         * Returns the total number of users in the data set held by the adapter.
         *
         * @return The total number of users.
         */
        @Override
        public int getItemCount() {
            return users.size();
        }

        /**
         * ViewHolder class for holding and recycling user item views.
         * Caches references to the UI components for each user item to improve performance.
         */
        class UserViewHolder extends RecyclerView.ViewHolder {

            /**
             * ImageView to display the user's profile image.
             */
            ImageView profileImageView;

            /**
             * TextView to display the user's name.
             */
            TextView nameTextView;

            /**
             * TextView to display the user's email.
             */
            TextView emailTextView;

            /**
             * TextView to display the user's phone number.
             */
            TextView phoneTextView;

            /**
             * Button to delete the user.
             */
            Button deleteButton;

            /**
             * Constructs a UserViewHolder and initializes the UI components.
             *
             * @param itemView The View of the individual user item.
             */
            UserViewHolder(@NonNull View itemView) {
                super(itemView);
                profileImageView = itemView.findViewById(R.id.userProfileImageView);
                nameTextView = itemView.findViewById(R.id.userNameTextView);
                emailTextView = itemView.findViewById(R.id.userEmailTextView);
                phoneTextView = itemView.findViewById(R.id.userPhoneTextView);
                deleteButton = itemView.findViewById(R.id.deleteUserButton);
            }
        }
    }

    /**
     * Prompts the admin to confirm deletion of a user.
     *
     * @param user The User object to delete.
     */
    private void confirmDeleteUser(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete this user? This action cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> deleteUser(user))
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Deletes the specified user from Firestore and their profile image from Firebase Storage.
     * Also removes the user from any event waiting lists they are part of.
     *
     * @param user The User object to delete.
     */
    private void deleteUser(User user) {
        String collection = "";
        String role = user.getRole();

        if (role == null) {
            Toast.makeText(this, "User role is not specified.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "User role is null for user: " + user.getUserId());
            return;
        }

        switch (role) {
            case "Entrant":
                collection = "Entrants";
                break;
            case "Organizer":
                collection = "Organizers";
                break;
            // Optionally handle Admin or other roles
            default:
                Toast.makeText(this, "Unknown user role: " + role, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Unknown user role: " + role);
                return; // Exit the method as the role is unrecognized
        }

        if (!collection.isEmpty()) {
            DocumentReference userRef = firestore.collection(collection).document(user.getUserId());

            // First, delete the Firestore document
            userRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(ManageUsersActivity.this, "User deleted successfully.", Toast.LENGTH_SHORT).show();

                        // Remove user from events' waiting lists
                        removeUserFromEventsWaitingLists(user);

                        // Now, delete the profile image from Firebase Storage if it exists
                        if (!TextUtils.isEmpty(user.getImagePath())) {
                            StorageReference imageRef = FirebaseStorage.getInstance().getReference().child(user.getImagePath());
                            imageRef.delete()
                                    .addOnSuccessListener(aVoid1 -> {
                                        Log.d(TAG, "Profile image deleted successfully.");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error deleting profile image: " + e.getMessage(), e);
                                    });
                        }

                        loadUsers(); // Refresh the user list
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ManageUsersActivity.this, "Error deleting user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error deleting user", e);
                    });
        } else {
            Toast.makeText(this, "Cannot determine collection for user.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Collection not determined for user: " + user.getUserId());
        }
    }

    /**
     * Removes the user from the waiting lists of the events they have joined.
     *
     * @param user The user to remove from events' waiting lists.
     */
    private void removeUserFromEventsWaitingLists(User user) {
        List<String> eventsJoined = user.getEventsJoined();

        if (eventsJoined == null || eventsJoined.isEmpty()) {
            Log.d(TAG, "User has not joined any events.");
            return;
        }

        for (String eventId : eventsJoined) {
            DocumentReference eventRef = firestore.collection("Events").document(eventId);

            // Remove entrant from the entrants map
            eventRef.update("entrants." + user.getUserId(), FieldValue.delete())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Removed " + user.getUserId() + " from event waiting list: " + eventId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error removing user from event " + eventId + ": " + e.getMessage(), e);
                    });

            // Decrement the current entrants number
            eventRef.update("currentEntrantsNumber", FieldValue.increment(-1))
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Decremented currentEntrantsNumber for event: " + eventId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error decrementing currentEntrantsNumber for event " + eventId + ": " + e.getMessage(), e);
                    });
        }
    }
}
