package com.example.potato1_events;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;


public class UserInfoActivity extends AppCompatActivity {

    private String userType;
    private String deviceId;
    private FirebaseFirestore firestore;

    private ImageView profileImageView;
    private Button uploadPictureButton;
    private EditText nameEditText, emailEditText, phoneEditText;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_info);

        // Get user type from intent
        userType = getIntent().getStringExtra("USER_TYPE");

        // Get device ID
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Initialize Firebase Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        profileImageView = findViewById(R.id.profileImageView);
        uploadPictureButton = findViewById(R.id.uploadPictureButton);
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        saveButton = findViewById(R.id.saveButton);

        // Set up listeners
        uploadPictureButton.setOnClickListener(v -> {
            // Handle profile picture upload (implement as needed)
            Toast.makeText(this, "Upload Picture Clicked", Toast.LENGTH_SHORT).show();
        });

        saveButton.setOnClickListener(v -> saveUserInfo());
    }

    private void saveUserInfo() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phoneNumber = phoneEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create user object
        User user = new User(name, email, phoneNumber, null); // null for profile picture URL (implement upload later)

        // Save to Firestore
        firestore.collection(userType + "s").document(deviceId).set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(UserInfoActivity.this, "User information saved", Toast.LENGTH_SHORT).show();
                    navigateToHomePage();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserInfoActivity.this, "Error saving user information: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToHomePage() {
        if (userType.equals("Entrant")) {
            Intent intent = new Intent(UserInfoActivity.this, EntrantHomeActivity.class);
            startActivity(intent);
        } else if (userType.equals("Organizer")) {
            // Placeholder for OrganizerHomeActivity
            // Intent intent = new Intent(UserInfoActivity.this, OrganizerHomeActivity.class);
            // startActivity(intent);
        }
        finish(); // Close current activity
    }
}