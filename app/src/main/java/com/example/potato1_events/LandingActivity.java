package com.example.potato1_events;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LandingActivity extends AppCompatActivity {

    private Button entrantButton;
    private Button organizerButton;
    private String deviceId;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landing);

        // Adjust padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Firestore
        firestore = FirebaseFirestore.getInstance();
        // Get device ID
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        // Initialize buttons
        entrantButton = findViewById(R.id.entrantButton);
        organizerButton = findViewById(R.id.organizerButton);

        // Set onClickListeners
        entrantButton.setOnClickListener(v -> checkUserExists("Entrant"));
        organizerButton.setOnClickListener(v -> checkUserExists("Organizer"));
    }

    private void checkUserExists(String userType) {
        firestore.collection(userType + "s").document(deviceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // User exists, navigate to the home activity
                        if (userType.equals("Entrant")) {
                            Intent intent = new Intent(LandingActivity.this, EntrantHomeActivity.class);
                            startActivity(intent);
                        } else if (userType.equals("Organizer")) {
                            Intent intent = new Intent(LandingActivity.this, OrganizerHomeActivity.class);
                            startActivity(intent);
                        }
                    } else {
                        // User does not exist, navigate to UserInfoActivity
                        Intent intent = new Intent(LandingActivity.this, UserInfoActivity.class);
                        intent.putExtra("USER_TYPE", userType);
                        intent.putExtra("MODE", "CREATE");
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LandingActivity.this, "Error accessing database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}