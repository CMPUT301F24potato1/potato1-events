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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class UserInfoActivity extends AppCompatActivity {

    private String userType;
    private String deviceId;
    private DatabaseReference databaseReference;

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

        // Initialize Firebase reference
        databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userType + "s");

        // Initialize views
        profileImageView = findViewById(R.id.profileImageView);
        uploadPictureButton = findViewById(R.id.uploadPictureButton);
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        saveButton = findViewById(R.id.saveButton);

        // Check if user already exists
        checkIfUserExists();

        // Set up listeners
        uploadPictureButton.setOnClickListener(v -> {
            // Handle profile picture upload (implement as needed)
            Toast.makeText(this, "Upload Picture Clicked", Toast.LENGTH_SHORT).show();
        });

        saveButton.setOnClickListener(v -> saveUserInfo());
    }

    private void checkIfUserExists() {
        databaseReference.child(deviceId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    // User exists, proceed to the next activity
                    navigateToHomePage();
                } else {
                    // User does not exist, stay on this page for input
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle errors here
                Toast.makeText(UserInfoActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserInfo() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phoneNumber = phoneEditText.getText().toString().trim();

        if(TextUtils.isEmpty(name) || TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create user object
        User user = new User(name, email, phoneNumber, null); // null for profile picture URL (implement upload later)

        // Save to Firebase
        databaseReference.child(deviceId).setValue(user).addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                Toast.makeText(UserInfoActivity.this, "User information saved", Toast.LENGTH_SHORT).show();
                navigateToHomePage();
            } else {
                Toast.makeText(UserInfoActivity.this, "Error saving user information", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToHomePage() {
        if(userType.equals("ENTRANT")) {
            //FIXME
            //Intent intent = new Intent(UserInfoActivity.this, EntrantHomeActivity.class);
            //startActivity(intent);
        } else if(userType.equals("ORGANIZER")) {
            //FIXME
            //Intent intent = new Intent(UserInfoActivity.this, OrganizerHomeActivity.class);
            //startActivity(intent);
        }
        finish(); // Close current activity
    }
}