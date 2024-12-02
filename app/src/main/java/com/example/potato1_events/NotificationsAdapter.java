// File: NotificationsAdapter.java
package com.example.potato1_events;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
// Import other necessary classes
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter class for managing and displaying notifications within a RecyclerView.
 * Handles the binding of NotificationItem data to the notification_item layout.
 * Supports actions such as accepting or declining notifications based on their type and status.
 */
public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {

    /**
     * List of NotificationItem objects to be displayed.
     */
    private List<NotificationItem> notificationList;

    /**
     * Context from the parent activity for accessing resources and layout inflaters.
     */
    private Context context;

    /**
     * Listener interface for handling notification actions like accept and decline.
     */
    private OnNotificationActionListener actionListener;

    /**
     * Interface to define callback methods for notification actions.
     * Implemented by activities or fragments to handle accept and decline events.
     */
    public interface OnNotificationActionListener {
        /**
         * Called when a notification is accepted.
         *
         * @param notification The NotificationItem that was accepted.
         */
        void onAccept(NotificationItem notification);

        /**
         * Called when a notification is declined.
         *
         * @param notification The NotificationItem that was declined.
         */
        void onDecline(NotificationItem notification);
    }

    /**
     * Constructor for NotificationsAdapter.
     *
     * @param notificationList List of notifications to display.
     * @param context          Context from the parent activity.
     * @param actionListener   Listener for handling notification actions.
     */
    public NotificationsAdapter(List<NotificationItem> notificationList, Context context, OnNotificationActionListener actionListener) {
        this.notificationList = notificationList;
        this.context = context;
        this.actionListener = actionListener;
    }

    /**
     * ViewHolder class for individual notification items.
     * Holds references to the UI components within each notification_item layout.
     */
    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        /**
         * TextView to display the notification title.
         */
        TextView notificationTitleTextView;

        /**
         * TextView to display the notification message.
         */
        TextView notificationMessageTextView;

        /**
         * Button to accept the notification action.
         */
        Button acceptButton;

        /**
         * Button to decline the notification action.
         */
        Button declineButton;

        /**
         * Constructor for NotificationViewHolder.
         *
         * @param itemView The root view of the notification_item layout.
         */
        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            notificationTitleTextView = itemView.findViewById(R.id.notificationTitleTextView);
            notificationMessageTextView = itemView.findViewById(R.id.notificationMessageTextView);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            declineButton = itemView.findViewById(R.id.declineButton);
        }

        /**
         * Binds the NotificationItem data to the UI components.
         * Sets up click listeners for accept and decline buttons based on notification type and status.
         *
         * @param notification   The NotificationItem to bind.
         * @param actionListener The listener for handling actions.
         */
        public void bind(NotificationItem notification, OnNotificationActionListener actionListener) {
            // Set the notification title and message
            notificationTitleTextView.setText(notification.getTitle());
            notificationMessageTextView.setText(notification.getMessage());

            // Check the type and status of the notification to determine button visibility
            if ("status_change".equals(notification.getType()) && "Selected".equals(notification.getStatus())) {
                // Show accept and decline buttons for applicable notifications
                acceptButton.setVisibility(View.VISIBLE);
                declineButton.setVisibility(View.VISIBLE);

                // Set click listener for the accept button
                acceptButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onAccept(notification); // Trigger the accept action
                    }
                });

                // Set click listener for the decline button
                declineButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onDecline(notification); // Trigger the decline action
                    }
                });
            } else {
                // Hide accept and decline buttons for other types or statuses
                acceptButton.setVisibility(View.GONE);
                declineButton.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Called when RecyclerView needs a new ViewHolder of the given type to represent an item.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new NotificationViewHolder that holds a View for each notification item.
     */
    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the notification_item layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_item, parent, false);
        return new NotificationViewHolder(view); // Return a new ViewHolder instance
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * Updates the contents of the ViewHolder to reflect the notification item at the given position.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the item.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        // Get the notification item at the current position
        NotificationItem notification = notificationList.get(position);
        // Bind the notification data to the ViewHolder
        holder.bind(notification, actionListener);
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of notification items.
     */
    @Override
    public int getItemCount() {
        return notificationList.size(); // Return the size of the notification list
    }
}
