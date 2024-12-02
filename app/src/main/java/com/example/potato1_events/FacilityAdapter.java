// File: FacilityAdapter.java
package com.example.potato1_events;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Adapter class for managing and displaying facilities within a RecyclerView.
 * Handles the binding of Facility data to the item_facility layout.
 * Supports actions such as deleting a facility from Firestore.
 */
public class FacilityAdapter extends RecyclerView.Adapter<FacilityAdapter.FacilityViewHolder> {

    /**
     * Context from the parent activity for accessing resources and layout inflaters.
     */
    private Context context;

    /**
     * List of Facility objects to be displayed.
     */
    private List<Facility> facilityList;

    /**
     * FirebaseFirestore instance for database interactions.
     */
    private FirebaseFirestore firestore;

    /**
     * Constructor for FacilityAdapter.
     *
     * @param context      Context from the parent activity.
     * @param facilityList List of facilities to display.
     */
    public FacilityAdapter(Context context, List<Facility> facilityList) {
        this.context = context;
        this.facilityList = facilityList;
        this.firestore = FirebaseFirestore.getInstance(); // Initialize Firestore instance
    }

    /**
     * Called when RecyclerView needs a new ViewHolder of the given type to represent an item.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new FacilityViewHolder that holds a View for each facility item.
     */
    @NonNull
    @Override
    public FacilityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item_facility layout to create a new View
        View view = LayoutInflater.from(context).inflate(R.layout.item_facility, parent, false);
        return new FacilityViewHolder(view); // Return a new ViewHolder instance
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * Updates the contents of the ViewHolder to reflect the facility item at the given position.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the item.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull FacilityViewHolder holder, int position) {
        // Get the facility item at the current position
        Facility facility = facilityList.get(position);

        // Set the facility name, address, and description in the respective TextViews
        holder.facilityNameTextView.setText(facility.getFacilityName());
        holder.facilityAddressTextView.setText(facility.getFacilityAddress());
        holder.facilityDescriptionTextView.setText(facility.getFacilityDescription());

        // Load facility photo using Picasso library
        if (facility.getFacilityPhotoUrl() != null && !facility.getFacilityPhotoUrl().isEmpty()) {
            Picasso.get()
                    .load(facility.getFacilityPhotoUrl())
                    .placeholder(R.drawable.ic_placeholder_image) // Optional placeholder image while loading
                    .error(R.drawable.ic_error_image) // Optional error image if loading fails
                    .into(holder.facilityPhotoImageView);
        } else {
            // Set a default placeholder image if no photo URL is available
            holder.facilityPhotoImageView.setImageResource(R.drawable.ic_placeholder_image);
        }

        // Handle delete button click event
        holder.deleteFacilityButton.setOnClickListener(v -> {
            // Show a confirmation dialog to prevent accidental deletions
            new android.app.AlertDialog.Builder(context)
                    .setTitle("Delete Facility") // Dialog title
                    .setMessage("Are you sure you want to delete this facility?") // Dialog message
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Delete facility from Firestore using the facility's ID
                        firestore.collection("Facilities")
                                .document(facility.getId()) // Corrected to getFacilityId()
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    // Inform the user of successful deletion
                                    Toast.makeText(context, "Facility deleted successfully.", Toast.LENGTH_SHORT).show();
                                    // Remove facility from the list and notify the adapter to update the RecyclerView
                                    facilityList.remove(position);
                                    notifyItemRemoved(position);
                                    notifyItemRangeChanged(position, facilityList.size());
                                })
                                .addOnFailureListener(e -> {
                                    // Inform the user if deletion fails
                                    Toast.makeText(context, "Failed to delete facility: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("No", null) // Do nothing on "No" button click
                    .show(); // Display the confirmation dialog
        });
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of facility items.
     */
    @Override
    public int getItemCount() {
        return facilityList.size(); // Return the size of the facility list
    }

    /**
     * ViewHolder class for individual facility items.
     * Holds references to the UI components within each item_facility layout.
     */
    public static class FacilityViewHolder extends RecyclerView.ViewHolder {

        /**
         * ImageView to display the facility photo.
         */
        ImageView facilityPhotoImageView;

        /**
         * TextView to display the facility name.
         */
        TextView facilityNameTextView;

        /**
         * TextView to display the facility address.
         */
        TextView facilityAddressTextView;

        /**
         * TextView to display the facility description.
         */
        TextView facilityDescriptionTextView;

        /**
         * ImageButton to delete the facility.
         */
        ImageButton deleteFacilityButton;

        /**
         * Constructor for FacilityViewHolder.
         *
         * @param itemView The root view of the item_facility layout.
         */
        public FacilityViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize UI components by finding them by their IDs
            facilityPhotoImageView = itemView.findViewById(R.id.facilityPhotoImageView);
            facilityNameTextView = itemView.findViewById(R.id.facilityNameTextView);
            facilityAddressTextView = itemView.findViewById(R.id.facilityAddressTextView);
            facilityDescriptionTextView = itemView.findViewById(R.id.facilityDescriptionTextView);
            deleteFacilityButton = itemView.findViewById(R.id.deleteFacilityButton);
        }
    }
}
