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

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {

    private List<NotificationItem> notificationList;
    private Context context;
    private OnNotificationActionListener actionListener;

    public interface OnNotificationActionListener {
        void onAccept(NotificationItem notification);
        void onDecline(NotificationItem notification);
    }

    public NotificationsAdapter(List<NotificationItem> notificationList, Context context, OnNotificationActionListener actionListener) {
        this.notificationList = notificationList;
        this.context = context;
        this.actionListener = actionListener;
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView notificationTitleTextView;
        TextView notificationMessageTextView;
        Button acceptButton;
        Button declineButton;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            notificationTitleTextView = itemView.findViewById(R.id.notificationTitleTextView);
            notificationMessageTextView = itemView.findViewById(R.id.notificationMessageTextView);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            declineButton = itemView.findViewById(R.id.declineButton);
        }

        public void bind(NotificationItem notification, OnNotificationActionListener actionListener) {
            notificationTitleTextView.setText(notification.getTitle());
            notificationMessageTextView.setText(notification.getMessage());

            if ("status_change".equals(notification.getType()) && "Selected".equals(notification.getStatus())) {
                // Show accept/decline buttons
                acceptButton.setVisibility(View.VISIBLE);
                declineButton.setVisibility(View.VISIBLE);

                acceptButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onAccept(notification);
                    }
                });

                declineButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onDecline(notification);
                    }
                });
            } else {
                // Hide accept/decline buttons
                acceptButton.setVisibility(View.GONE);
                declineButton.setVisibility(View.GONE);
            }
        }
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_item, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem notification = notificationList.get(position);
        holder.bind(notification, actionListener);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }
}