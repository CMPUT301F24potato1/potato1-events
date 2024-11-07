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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for admins to manage user profiles.
 * Allows viewing and deleting users.
 */
public class ManageUsersActivity extends AppCompatActivity {

    private RecyclerView usersRecyclerView;
    private UsersAdapter usersAdapter;
    private List<User> userList;

    private FirebaseFirestore firestore;

    // Tag for logging
    private static final String TAG = "ManageUsersActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI Components
        Toolbar toolbar = findViewById(R.id.toolbar_manage_users);
        setSupportActionBar(toolbar);

        // Enable the Up button
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        usersRecyclerView.setHasFixedSize(true);

        userList = new ArrayList<>();
        usersAdapter = new UsersAdapter(userList);
        usersRecyclerView.setAdapter(usersAdapter);

        // Load users from Firestore
        loadUsers();
    }

    /**
     * Loads all users from "Entrants" and "Organizers" collections in Firestore.
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
     * Handles the Up button behavior.
     *
     * @param item The selected menu item.
     * @return True if handled, else false.
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
     * RecyclerView Adapter for displaying user profiles.
     */
    private class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

        private List<User> users;

        UsersAdapter(List<User> users) {
            this.users = users;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(view);
        }

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
                            .placeholder(R.drawable.ic_placeholder_image)
                            .error(R.drawable.ic_error_image)
                            .into(holder.profileImageView);
                }).addOnFailureListener(e -> {
                    holder.profileImageView.setImageResource(R.drawable.ic_error_image);
                });
            } else {
                holder.profileImageView.setImageResource(R.drawable.ic_placeholder_image);
            }

            holder.nameTextView.setText(user.getName());
            holder.emailTextView.setText(user.getEmail());
            holder.phoneTextView.setText(user.getPhoneNumber());

            // Set Delete Button Listener
            holder.deleteButton.setOnClickListener(v -> {
                confirmDeleteUser(user);
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        /**
         * ViewHolder for user items.
         */
        class UserViewHolder extends RecyclerView.ViewHolder {

            ImageView profileImageView;
            TextView nameTextView;
            TextView emailTextView;
            TextView phoneTextView;
            Button deleteButton;

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
     *
     * @param user The User object to delete.
     */
    private void deleteUser(User user) {
        String collection = "";
        switch (user.getRole()) {
            case "Entrant":
                collection = "Entrants";
                break;
            case "Organizer":
                collection = "Organizers";
                break;
            // Optionally handle Admin or other roles
            default:
                Toast.makeText(this, "Unknown user role: " + user.getRole(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Unknown user role: " + user.getRole());
                return; // Exit the method as the role is unrecognized
        }

        if (!collection.isEmpty()) {
            DocumentReference userRef = firestore.collection(collection).document(user.getUserId());

            // First, delete the Firestore document
            userRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(ManageUsersActivity.this, "User deleted successfully.", Toast.LENGTH_SHORT).show();

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
}
