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
 */
public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.MediaViewHolder> {

    private List<StorageReference> mediaList;
    private OnMediaClickListener listener;

    /**
     * Interface to handle media item clicks.
     */
    public interface OnMediaClickListener {
        void onDeleteClick(StorageReference storageRef);
    }

    /**
     * Constructor for MediaAdapter.
     *
     * @param mediaList List of StorageReference objects representing media.
     * @param listener  Listener for media item actions.
     */
    public MediaAdapter(List<StorageReference> mediaList, OnMediaClickListener listener) {
        this.mediaList = mediaList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media, parent, false);
        return new MediaViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        StorageReference storageRef = mediaList.get(position);
        // Load image URL
        storageRef.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    Picasso.get()
                            .load(uri)
                            .placeholder(R.drawable.ic_placeholder_image)
                            .error(R.drawable.ic_error_image)
                            .into(holder.imageView);
                })
                .addOnFailureListener(e -> {
                    holder.imageView.setImageResource(R.drawable.ic_error_image);
                });

        // Set delete button click listener
        holder.deleteButton.setOnClickListener(v -> listener.onDeleteClick(storageRef));
    }

    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    /**
     * ViewHolder class for media items.
     */
    public static class MediaViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton deleteButton;

        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewMedia);
            deleteButton = itemView.findViewById(R.id.buttonDeleteMedia);
        }
    }
}
