// File: EventsAdapter.java
package com.example.potato1_events;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Adapter class for managing and displaying a list of events in a RecyclerView.
 * Binds event data to the UI components and handles user interactions with each event item.
 */
public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    // Context from the hosting activity
    private Context context;

    // List of Event objects to display
    private List<Event> eventList;

    /**
     * Constructs an EventsAdapter with the specified context and list of events.
     *
     * @param context   The context from the hosting activity.
     * @param eventList The list of Event objects to display.
     */
    public EventsAdapter(Context context, List<Event> eventList) {
        this.context = context;
        this.eventList = eventList;
    }

    /**
     * Called when RecyclerView needs a new {@link EventViewHolder} of the given type to represent an item.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new EventViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the event_item layout
        View view = LayoutInflater.from(context).inflate(R.layout.event_item, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * Updates the contents of the EventViewHolder to reflect the event at the given position.
     *
     * @param holder   The EventViewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        // Get the event at the current position
        Event event = eventList.get(position);

        // Bind event name and location to TextViews
        holder.eventNameTextView.setText(event.getName());
        holder.eventLocationTextView.setText(event.getEventLocation());

        // Load event poster image using Picasso
        if (!TextUtils.isEmpty(event.getPosterImageUrl())) {
            Picasso.get()
                    .load(event.getPosterImageUrl())
                    .placeholder(R.drawable.ic_placeholder_image) // Placeholder image while loading
                    .error(R.drawable.ic_error_image)             // Image to display on error
                    .into(holder.eventPosterImageView);
        } else {
            // Set a default placeholder image if poster URL is empty
            holder.eventPosterImageView.setImageResource(R.drawable.ic_placeholder_image);
        }

        // Set OnClickListener for the entire event card to navigate to EventDetailsEntrantActivity
        holder.eventCardView.setOnClickListener(v -> {
            Intent intent = new Intent(context, EventDetailsEntrantActivity.class);
            intent.putExtra("EVENT_ID", event.getId()); // Pass the event ID to the details activity
            context.startActivity(intent);
        });
    }

    /**
     * Returns the total number of events in the data set held by the adapter.
     *
     * @return The total number of events.
     */
    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * ViewHolder class for holding and recycling event item views.
     * Caches references to the UI components for each event item to improve performance.
     */
    public static class EventViewHolder extends RecyclerView.ViewHolder {

        // ImageView to display the event poster
        ImageView eventPosterImageView;

        // TextViews to display the event name and location
        TextView eventNameTextView, eventLocationTextView;

        // View representing the entire event card
        View eventCardView;

        /**
         * Constructs an EventViewHolder and initializes the UI components.
         *
         * @param itemView The View of the individual event item.
         */
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize UI components from the event_item layout
            eventPosterImageView = itemView.findViewById(R.id.eventPosterImageView);
            eventNameTextView = itemView.findViewById(R.id.eventNameTextView);
            eventLocationTextView = itemView.findViewById(R.id.eventLocationTextView);
            eventCardView = itemView.findViewById(R.id.eventCardView);
        }
    }
}
