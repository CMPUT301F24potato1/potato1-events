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

public class FacilityAdapter extends RecyclerView.Adapter<FacilityAdapter.FacilityViewHolder> {

    private Context context;
    private List<Facility> facilityList;
    private FirebaseFirestore firestore;

    public FacilityAdapter(Context context, List<Facility> facilityList) {
        this.context = context;
        this.facilityList = facilityList;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public FacilityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_facility, parent, false);
        return new FacilityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FacilityViewHolder holder, int position) {
        Facility facility = facilityList.get(position);

        holder.facilityNameTextView.setText(facility.getFacilityName());
        holder.facilityAddressTextView.setText(facility.getFacilityAddress());
        holder.facilityDescriptionTextView.setText(facility.getFacilityDescription());

        // Load facility photo using Picasso
        if (facility.getFacilityPhotoUrl() != null && !facility.getFacilityPhotoUrl().isEmpty()) {
            Picasso.get()
                    .load(facility.getFacilityPhotoUrl())
                    .placeholder(R.drawable.ic_placeholder_image) // Optional placeholder
                    .error(R.drawable.ic_error_image) // Optional error image
                    .into(holder.facilityPhotoImageView);
        } else {
            holder.facilityPhotoImageView.setImageResource(R.drawable.ic_placeholder_image); // Default image
        }

        // Handle delete button click
        holder.deleteFacilityButton.setOnClickListener(v -> {
            // Confirm deletion
            new android.app.AlertDialog.Builder(context)
                    .setTitle("Delete Facility")
                    .setMessage("Are you sure you want to delete this facility?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Delete facility from Firestore
                        firestore.collection("Facilities")
                                .document(facility.getId()) // Corrected to getFacilityId()
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(context, "Facility deleted successfully.", Toast.LENGTH_SHORT).show();
                                    // Remove facility from the list and notify adapter
                                    facilityList.remove(position);
                                    notifyItemRemoved(position);
                                    notifyItemRangeChanged(position, facilityList.size());
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(context, "Failed to delete facility: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return facilityList.size();
    }

    public static class FacilityViewHolder extends RecyclerView.ViewHolder {

        ImageView facilityPhotoImageView;
        TextView facilityNameTextView, facilityAddressTextView, facilityDescriptionTextView;
        ImageButton deleteFacilityButton;

        public FacilityViewHolder(@NonNull View itemView) {
            super(itemView);
            facilityPhotoImageView = itemView.findViewById(R.id.facilityPhotoImageView);
            facilityNameTextView = itemView.findViewById(R.id.facilityNameTextView);
            facilityAddressTextView = itemView.findViewById(R.id.facilityAddressTextView);
            facilityDescriptionTextView = itemView.findViewById(R.id.facilityDescriptionTextView);
            deleteFacilityButton = itemView.findViewById(R.id.deleteFacilityButton);
        }
    }
}
