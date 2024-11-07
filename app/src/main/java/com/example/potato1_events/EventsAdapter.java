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

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    private Context context;
    private List<Event> eventList;

    public EventsAdapter(Context context, List<Event> eventList) {
        this.context = context;
        this.eventList = eventList;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.event_item, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.eventNameTextView.setText(event.getName());
        holder.eventLocationTextView.setText(event.getEventLocation());

        if (!TextUtils.isEmpty(event.getPosterImageUrl())) {
            Picasso.get()
                    .load(event.getPosterImageUrl())
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_error_image)
                    .into(holder.eventPosterImageView);
        } else {
            holder.eventPosterImageView.setImageResource(R.drawable.ic_placeholder_image);
        }

        holder.eventCardView.setOnClickListener(v -> {
            Intent intent = new Intent(context, EventDetailsEntrantActivity.class);
            intent.putExtra("EVENT_ID", event.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {

        ImageView eventPosterImageView;
        TextView eventNameTextView, eventLocationTextView;
        View eventCardView;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventPosterImageView = itemView.findViewById(R.id.eventPosterImageView);
            eventNameTextView = itemView.findViewById(R.id.eventNameTextView);
            eventLocationTextView = itemView.findViewById(R.id.eventLocationTextView);
            eventCardView = itemView.findViewById(R.id.eventCardView);
        }
    }
}
