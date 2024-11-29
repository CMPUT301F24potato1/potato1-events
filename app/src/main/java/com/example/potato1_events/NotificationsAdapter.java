// File: NotificationsAdapter.java
package com.example.potato1_events;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_item, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem notification = notificationList.get(position);
        holder.bind(notification);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, messageTextView;
        Button acceptButton, declineButton;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.notificationTitleTextView);
            messageTextView = itemView.findViewById(R.id.notificationMessageTextView);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            declineButton = itemView.findViewById(R.id.declineButton);
        }

        public void bind(NotificationItem notification) {
            titleTextView.setText(notification.getTitle());
            messageTextView.setText(notification.getMessage());

            if ("selection".equals(notification.getType()) && !notification.isRead()) {
                acceptButton.setVisibility(View.VISIBLE);
                declineButton.setVisibility(View.VISIBLE);
            } else {
                acceptButton.setVisibility(View.GONE);
                declineButton.setVisibility(View.GONE);
            }

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
        }
    }
}