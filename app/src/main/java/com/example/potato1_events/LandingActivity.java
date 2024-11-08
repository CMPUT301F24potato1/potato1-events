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

/**
 * Activity that serves as the landing page for the application.
 * Allows users to select their role as either an Entrant or an Organizer.
 * Checks if the user exists in the database and navigates accordingly.
 */
public class LandingActivity extends AppCompatActivity {

    // UI Components

    /**
     * Button to navigate as an Entrant.
     */
    private Button entrantButton;

    /**
     * Button to navigate as an Organizer.
     */
    private Button organizerButton;

    /**
     * Unique device ID used to identify the user.
     */
    private String deviceId;

    /**
     * Repository for handling user-related database operations.
     */
    private UserRepository userRepository;

    /**
     * Called when the activity is first created.
     * Initializes UI components, Firebase instances, and sets up event listeners.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landing);

        // Adjust padding for system bars to enable edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Firestore
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        userRepository = new UserRepository(firestore);

        // Retrieve Device ID (unique identifier for the user)
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Initialize buttons
        entrantButton = findViewById(R.id.entrantButton);
        organizerButton = findViewById(R.id.organizerButton);

        // Set onClickListeners for buttons
        entrantButton.setOnClickListener(v -> checkUserExists("Entrant"));
        organizerButton.setOnClickListener(v -> checkUserExists("Organizer"));
    }

    /**
     * Sets the UserRepository instance for testing purposes.
     *
     * @param userRepository The mocked UserRepository instance.
     */
    @VisibleForTesting
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Checks if a user of the specified type exists in the database.
     * If the user exists, navigates to the respective home activity.
     * If not, navigates to the UserInfoActivity to create a new user.
     *
     * @param userType The type of user ("Entrant" or "Organizer").
     */
    private void checkUserExists(String userType) {
        userRepository.checkUserExists(userType, deviceId, new UserRepository.UserExistsCallback() {
            /**
             * Called when the user existence check is successful.
             *
             * @param userData The data of the user if they exist.
             */
            @Override
            public void onResult(UserData userData) {
                if (userData.exists()) {
                    // User exists, navigate to the appropriate home activity
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
                    // User does not exist, navigate to UserInfoActivity to create a new user
                    Intent intent = new Intent(LandingActivity.this, UserInfoActivity.class);
                    intent.putExtra("USER_TYPE", userType);
                    intent.putExtra("MODE", "CREATE");
                    startActivity(intent);
                }
            }

            /**
             * Called when there is an error during the user existence check.
             *
             * @param e The exception that occurred.
             */
            @Override
            public void onError(Exception e) {
                Toast.makeText(LandingActivity.this, "Error accessing database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
