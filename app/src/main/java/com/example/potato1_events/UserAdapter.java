// File: UserAdapter.java
package com.example.potato1_events;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import java.util.List;
import java.util.Map;

/**
 * Adapter class for managing and displaying a list of users in a RecyclerView.
 * Handles binding user data to the UI components and loading profile images from Firebase Storage.
 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    /**
     * List of users to be displayed in the RecyclerView.
     */
    private List<User> userList;

    /**
     * Map containing user statuses keyed by userId.
     */
    private Map<String, String> userStatusMap;

    /**
     * Context from the Activity for accessing resources and layout inflaters.
     */
    private Context context;

    /**
     * Listener for handling cancel actions.
     */
    private OnCancelClickListener onCancelClickListener;

    /**
     * Tag for logging.
     */
    private static final String TAG = "UserAdapter";
    /**
     * Interface for handling cancel action.
     */
    public interface OnCancelClickListener {
        void onCancelClick(User user);
    }
    /**
     * Constructor for UserAdapter.
     *
     * @param userList      List of users to display.
     * @param userStatusMap Map of userId to status.
     * @param context       The context from the Activity.
     */
    public UserAdapter(List<User> userList, Map<String, String> userStatusMap, Context context, OnCancelClickListener onCancelClickListener) {
        this.userList = userList;
        this.userStatusMap = userStatusMap;
        this.context = context;
        this.onCancelClickListener = onCancelClickListener;
    }

    /**
     * Retrieves the status of an entrant based on userId.
     *
     * @param userId The ID of the user.
     * @return The status string ("waitlist", "enrolled", etc.) or "Unknown" if not found.
     */
    public String getEntrantStatus(String userId) {
        String status = userStatusMap.getOrDefault(userId, "Unknown");
        Log.d(TAG, "User ID: " + userId + ", Status: " + status);
        return status;
    }

    /**
     * Called when RecyclerView needs a new {@link UserViewHolder} of the given type to represent an item.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new UserViewHolder that holds a View for each user item.
     */
    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View userView = LayoutInflater.from(parent.getContext()).inflate(R.layout.entrant_item, parent, false);
        return new UserViewHolder(userView);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method updates the contents of the {@link UserViewHolder} to reflect the user at the given position.
     *
     * @param holder   The UserViewHolder which should be updated to represent the contents of the item.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user);
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of users.
     */
    @Override
    public int getItemCount() {
        return userList.size();
    }

    /**
     * ViewHolder class for representing each user item in the RecyclerView.
     * Handles binding user data to UI components and loading profile images.
     */
    class UserViewHolder extends RecyclerView.ViewHolder {
        /**
         * ImageView for displaying the entrant's profile image.
         */
        ImageView entrantProfileImageView;

        /**
         * TextView for displaying the entrant's name.
         */
        TextView entrantNameTextView;

        /**
         * TextView for displaying the entrant's status.
         */
        TextView entrantStatusTextView;

        /**
         * Button for canceling an entrant.
         */
        Button cancelButton;

        /**
         * Constructor for UserViewHolder.
         *
         * @param itemView The view representing a single user item.
         */
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            entrantProfileImageView = itemView.findViewById(R.id.entrantProfileImageView);
            entrantNameTextView = itemView.findViewById(R.id.entrantNameTextView);
            entrantStatusTextView = itemView.findViewById(R.id.entrantStatusTextView);
            cancelButton = itemView.findViewById(R.id.cancelEntrantButton);
        }

        /**
         * Binds the user data to the UI components.
         * Loads the profile image from Firebase Storage and sets the entrant's name and status.
         *
         * @param user The user object containing data to bind.
         */
        public void bind(User user) {
            // Load profile image
            if (!TextUtils.isEmpty(user.getImagePath())) {
                StorageReference imageRef = FirebaseStorage.getInstance().getReference().child(user.getImagePath());
                imageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            Picasso.get().load(uri)
                                    .placeholder(R.drawable.ic_placeholder_image)
                                    .error(R.drawable.ic_error_image)
                                    .into(entrantProfileImageView);
                            Log.d(TAG, "Loaded profile image for user: " + user.getUserId());
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to load profile image for user: " + user.getUserId(), e);
                            entrantProfileImageView.setImageResource(R.drawable.ic_error_image);
                        });
            } else {
                entrantProfileImageView.setImageResource(R.drawable.ic_placeholder_image);
                Log.d(TAG, "No profile image path for user: " + user.getUserId());
            }

            // Set entrant name
            entrantNameTextView.setText(user.getName() != null ? user.getName() : "Unnamed Entrant");

            // Retrieve and set entrant status from the map using userId
            String status = getEntrantStatus(user.getUserId());
            entrantStatusTextView.setText("Status: " + capitalizeFirstLetter(status));

            // Optional: Change text color based on status for better UX
            switch (status.toLowerCase()) {
                case "enrolled":
                    entrantStatusTextView.setTextColor(context.getResources().getColor(R.color.enrolledColor));
                    break;
                case "waitlist":
                    entrantStatusTextView.setTextColor(context.getResources().getColor(R.color.waitlistColor));
                    break;
                case "canceled":
                    entrantStatusTextView.setTextColor(context.getResources().getColor(R.color.canceledColor));
                    break;
                case "chosen":
                    entrantStatusTextView.setTextColor(context.getResources().getColor(R.color.chosenColor));
                    break;
                default:
                    entrantStatusTextView.setTextColor(context.getResources().getColor(R.color.unknownColor));
                    break;
            }
            if ("Selected".equalsIgnoreCase(status)) {
                cancelButton.setVisibility(View.VISIBLE);
            } else {
                cancelButton.setVisibility(View.GONE);
            }

            // Handle "Cancel" button click
            cancelButton.setOnClickListener(v -> {
                if (onCancelClickListener != null) {
                    onCancelClickListener.onCancelClick(user);
                } else {
                    Toast.makeText(context, "Cancel action not implemented.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Capitalizes the first letter of a string.
     *
     * @param text The input string.
     * @return The string with the first letter capitalized.
     */
    private String capitalizeFirstLetter(String text) {
        if (TextUtils.isEmpty(text)) {
            return "Unknown";
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}
