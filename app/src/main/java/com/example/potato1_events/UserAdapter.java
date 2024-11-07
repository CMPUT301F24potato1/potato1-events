package com.example.potato1_events;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

// Import Firebase Storage
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Adapter for displaying users (entrants) in the waiting list.
 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private Context context;

    /**
     * Constructor for UserAdapter.
     *
     * @param userList The list of users to display.
     * @param context  The context from the calling Activity.
     */
    public UserAdapter(List<User> userList, Context context) {
        this.userList = userList;
        this.context = context;
    }

    /**
     * Inflates the entrant_item layout and returns a ViewHolder.
     */
    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View userView = LayoutInflater.from(parent.getContext()).inflate(R.layout.entrant_item, parent, false);
        return new UserViewHolder(userView);
    }

    /**
     * Binds user data to the UI components.
     */
    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user);
    }

    /**
     * Returns the total number of users in the list.
     */
    @Override
    public int getItemCount() {
        return userList.size();
    }

    /**
     * ViewHolder class for user items.
     */
    class UserViewHolder extends RecyclerView.ViewHolder {

        ImageView entrantProfileImageView;
        TextView entrantNameTextView;
        TextView entrantStatusTextView;

        /**
         * Initializes the UI components for each list item.
         */
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            entrantProfileImageView = itemView.findViewById(R.id.entrantProfileImageView);
            entrantNameTextView = itemView.findViewById(R.id.entrantNameTextView);
            entrantStatusTextView = itemView.findViewById(R.id.entrantStatusTextView);
        }

        /**
         * Binds the user data to the UI components.
         *
         * @param user The User object containing entrant details.
         */
        public void bind(User user) {
            // Load profile image using Firebase Storage
            if (!TextUtils.isEmpty(user.getImagePath())) {
                StorageReference imageRef = FirebaseStorage.getInstance().getReference().child(user.getImagePath());
                imageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            Picasso.get()
                                    .load(uri)
                                    .placeholder(R.drawable.ic_placeholder_image)
                                    .error(R.drawable.ic_error_image)
                                    .into(entrantProfileImageView);
                        })
                        .addOnFailureListener(e -> {
                            entrantProfileImageView.setImageResource(R.drawable.ic_error_image);
                        });
            } else {
                entrantProfileImageView.setImageResource(R.drawable.ic_placeholder_image);
            }

            // Set entrant name
            entrantNameTextView.setText(user.getName() != null ? user.getName() : "Unnamed Entrant");

            // Set entrant status
            String statusText = "Status: " + (user.getStatus() != null ? user.getStatus() : "Unknown");
            entrantStatusTextView.setText(statusText);
        }
    }
}
