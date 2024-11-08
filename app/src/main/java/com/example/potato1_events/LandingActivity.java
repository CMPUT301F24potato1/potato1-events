// LandingActivity.java
package com.example.potato1_events;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LandingActivity extends AppCompatActivity {

    private Button entrantButton;
    private Button organizerButton;
    private String deviceId;
    private UserRepository userRepository;

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
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        userRepository = new UserRepository(firestore);

        // Get device ID
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Initialize buttons
        entrantButton = findViewById(R.id.entrantButton);
        organizerButton = findViewById(R.id.organizerButton);

        // Set onClickListeners
        entrantButton.setOnClickListener(v -> checkUserExists("Entrant"));
        organizerButton.setOnClickListener(v -> checkUserExists("Organizer"));
    }

    @VisibleForTesting
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private void checkUserExists(String userType) {
        userRepository.checkUserExists(userType, deviceId, new UserRepository.UserExistsCallback() {
            @Override
            public void onResult(UserData userData) {
                if (userData.exists()) {
                    // User exists
                    if (userType.equals("Entrant")) {
                        Intent intent = new Intent(LandingActivity.this, EntrantHomeActivity.class);
                        intent.putExtra("IS_ADMIN", userData.isAdmin());
                        startActivity(intent);
                    } else if (userType.equals("Organizer")) {
                        Intent intent = new Intent(LandingActivity.this, OrganizerHomeActivity.class);
                        intent.putExtra("IS_ADMIN", userData.isAdmin());
                        startActivity(intent);
                    }
                } else {
                    // User does not exist
                    Intent intent = new Intent(LandingActivity.this, UserInfoActivity.class);
                    intent.putExtra("USER_TYPE", userType);
                    intent.putExtra("MODE", "CREATE");
                    startActivity(intent);
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(LandingActivity.this, "Error accessing database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
