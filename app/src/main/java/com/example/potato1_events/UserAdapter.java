// File: UserAdapter.java
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import java.util.List;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private Map<String, String> userStatusMap; // Map of user statuses keyed by userId
    private Context context;

    /**
     * Constructor for UserAdapter.
     *
     * @param userList      List of users to display.
     * @param userStatusMap Map of userId to status.
     * @param context       The context from the Activity.
     */
    public UserAdapter(List<User> userList, Map<String, String> userStatusMap, Context context) {
        this.userList = userList;
        this.userStatusMap = userStatusMap;
        this.context = context;
    }

    /**
     * Retrieves the status of an entrant based on userId.
     *
     * @param userId The ID of the user.
     * @return The status string ("waitlist", "enrolled", etc.) or null if not found.
     */
    public String getEntrantStatus(String userId) {
        return userStatusMap.get(userId);
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View userView = LayoutInflater.from(parent.getContext()).inflate(R.layout.entrant_item, parent, false);
        return new UserViewHolder(userView);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView entrantProfileImageView;
        TextView entrantNameTextView;
        TextView entrantStatusTextView;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            entrantProfileImageView = itemView.findViewById(R.id.entrantProfileImageView);
            entrantNameTextView = itemView.findViewById(R.id.entrantNameTextView);
            entrantStatusTextView = itemView.findViewById(R.id.entrantStatusTextView);
        }

        public void bind(User user) {
            // Load profile image
            if (!TextUtils.isEmpty(user.getImagePath())) {
                StorageReference imageRef = FirebaseStorage.getInstance().getReference().child(user.getImagePath());
                imageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> Picasso.get().load(uri)
                                .placeholder(R.drawable.ic_placeholder_image)
                                .error(R.drawable.ic_error_image)
                                .into(entrantProfileImageView))
                        .addOnFailureListener(e -> entrantProfileImageView.setImageResource(R.drawable.ic_error_image));
            } else {
                entrantProfileImageView.setImageResource(R.drawable.ic_placeholder_image);
            }

            // Set entrant name
            entrantNameTextView.setText(user.getName() != null ? user.getName() : "Unnamed Entrant");

            // Retrieve and set entrant status from the map using userId
            String status = userStatusMap.getOrDefault(user.getUserId(), "Unknown");
            entrantStatusTextView.setText("Status: " + capitalizeFirstLetter(status));
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
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}
