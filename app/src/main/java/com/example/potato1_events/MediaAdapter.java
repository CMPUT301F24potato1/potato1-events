// File: MediaAdapter.java
package com.example.potato1_events;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * RecyclerView Adapter for displaying media (images) in ManageMediaActivity.
 * Handles the binding of media data to the UI components and manages user interactions such as deletion.
 */
public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.MediaViewHolder> {

    // List of StorageReference objects representing media files
    private List<StorageReference> mediaList;

    // Listener for handling media item actions (e.g., deletion)
    private OnMediaClickListener listener;

    /**
     * Interface to handle media item clicks.
     * Allows external classes to define actions when media items are interacted with.
     */
    public interface OnMediaClickListener {
        /**
         * Called when the delete button of a media item is clicked.
         *
         * @param storageRef The StorageReference of the media to delete.
         */
        void onDeleteClick(StorageReference storageRef);
    }

    /**
     * Constructs a MediaAdapter with the specified list of media and listener.
     *
     * @param mediaList List of StorageReference objects representing media.
     * @param listener  Listener for media item actions.
     */
    public MediaAdapter(List<StorageReference> mediaList, OnMediaClickListener listener) {
        this.mediaList = mediaList;
        this.listener = listener;
    }

    /**
     * Called when RecyclerView needs a new {@link MediaViewHolder} of the given type to represent an item.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new MediaViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item_media layout
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media, parent, false);
        return new MediaViewHolder(v);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * Updates the contents of the MediaViewHolder to reflect the media at the given position.
     *
     * @param holder   The MediaViewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        StorageReference storageRef = mediaList.get(position);

        // Load image URL from Firebase Storage
        storageRef.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    // Use Picasso to load the image into the ImageView with placeholders for loading and error states
                    Picasso.get()
                            .load(uri)
                            .placeholder(R.drawable.ic_placeholder_image) // Placeholder image while loading
                            .error(R.drawable.ic_error_image)             // Image to display on error
                            .into(holder.imageView);
                })
                .addOnFailureListener(e -> {
                    // Set an error image if the download URL retrieval fails
                    holder.imageView.setImageResource(R.drawable.ic_error_image);
                });

        // Set delete button click listener to handle media deletion
        holder.deleteButton.setOnClickListener(v -> listener.onDeleteClick(storageRef));
    }

    /**
     * Returns the total number of media items in the data set held by the adapter.
     *
     * @return The total number of media items.
     */
    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    /**
     * ViewHolder class for media items.
     * Holds references to the UI components for each media item to improve performance.
     */
    public static class MediaViewHolder extends RecyclerView.ViewHolder {
        /**
         * ImageView to display the media (image).
         */
        ImageView imageView;

        /**
         * ImageButton to delete the media item.
         */
        ImageButton deleteButton;

        /**
         * Constructs a MediaViewHolder and initializes the UI components.
         *
         * @param itemView The View of the individual media item.
         */
        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewMedia);
            deleteButton = itemView.findViewById(R.id.buttonDeleteMedia);
        }
    }
}
