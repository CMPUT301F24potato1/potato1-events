package com.example.potato1_events;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

public class EventDetailsEntrantActivity extends AppCompatActivity {

    private String eventId;
    private FirebaseFirestore firestore;

    private ImageView eventPosterImageView;
    private TextView eventNameTextView;
    private TextView eventDescriptionTextView;
    private TextView eventTimeTextView;
    private TextView eventCapacityTextView;
    private TextView eventSignedUpTextView;

    // Placeholder data (if needed)
    private String placeholderText = "Placeholder";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_details_entrant);

        // Get event ID from intent
        eventId = getIntent().getStringExtra("EVENT_ID");

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        eventPosterImageView = findViewById(R.id.eventPosterImageView);
        eventNameTextView = findViewById(R.id.eventNameTextView);
        eventDescriptionTextView = findViewById(R.id.eventDescriptionTextView);
        eventTimeTextView = findViewById(R.id.eventTimeTextView);
        eventCapacityTextView = findViewById(R.id.eventCapacityTextView);
        eventSignedUpTextView = findViewById(R.id.eventSignedUpTextView);

        // Load event details
        loadEventDetails();
    }

    private void loadEventDetails() {
        firestore.collection("Events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);
                        displayEventDetails(event);
                    } else {
                        Toast.makeText(EventDetailsEntrantActivity.this, "Event not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EventDetailsEntrantActivity.this, "Error loading event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void displayEventDetails(Event event) {
        // Set event details
        eventNameTextView.setText(event.getName());
        eventDescriptionTextView.setText(event.getDescription());
        eventTimeTextView.setText(event.getRegistrationStart().toString()); // Assuming 'time' field exists
        eventCapacityTextView.setText("Capacity: " + event.getCapacity());
        eventSignedUpTextView.setText("Signed Up: " + event.getCapacity()); // Assuming 'signedUp' field exists

        // Load event poster using Glide or any other image loading library
        //FIXME
//        Glide.with(this)
//                .load(event.getPosterUrl())
//                .placeholder(R.drawable.ic_placeholder) // Placeholder image
//                .into(eventPosterImageView);
    }
}