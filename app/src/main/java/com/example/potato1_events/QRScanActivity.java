// QRScanActivity.java
package com.example.potato1_events;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import javax.annotation.Nullable;

/**
 * Activity to handle QR code scanning and navigation based on scanned event's facilityId.
 */
public class QRScanActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private FirebaseFirestore firestore;
    private String deviceId; // Organizer's device ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // No need to set a content view since this activity only handles scanning

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Optional: Configure Firestore settings if needed
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);

        // Get device ID
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Check for camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, initiate scan
            startQRScan();
        }
    }

    /**
     * Initiates the QR Code Scan using ZXing's IntentIntegrator.
     */
    private void startQRScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan a QR Code");
        integrator.setOrientationLocked(true);  // Lock orientation to portrait
        integrator.setCaptureActivity(PortraitCaptureActivity.class);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();
    }

    /**
     * Handles the result from the QR code scanning activity.
     *
     * @param requestCode The integer request code originally supplied to startActivityForResult(),
     *                    allowing you to identify who this result came from.
     * @param resultCode  The integer result code returned by the child activity through its setResult().
     * @param data        An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
                finish(); // Close the activity
            } else {
                String scannedData = result.getContents();
                // Handle the scanned data
                handleScannedData(scannedData);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Handles the scanned QR code data by determining the user's role based on facilityId.
     *
     * @param scannedData The data obtained from scanning the QR code (assumed to be eventId).
     */
    private void handleScannedData(String scannedData) {
        // Validate scannedData
        if (TextUtils.isEmpty(scannedData)) {
            Toast.makeText(this, "Scanned data is empty.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String eventId = scannedData.trim();

        // Query Firestore to get the event document
        DocumentReference eventRef = firestore.collection("Events").document(eventId);

        eventRef.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot eventDoc = task.getResult();
                        if (eventDoc.exists()) {
                            String eventFacilityId = eventDoc.getString("facilityId");
                            if (TextUtils.isEmpty(eventFacilityId)) {
                                Toast.makeText(QRScanActivity.this, "Event facilityId is missing.", Toast.LENGTH_SHORT).show();
                                finish();
                                return;
                            }

                            // Compare facilityId with deviceId
                            if (eventFacilityId.equals(deviceId)) {
                                // User is the organizer
                                navigateToOrganizerEventDetails(eventId);
                            } else {
                                // User is an entrant
                                navigateToEntrantEventDetails(eventId);
                            }
                        } else {
                            Toast.makeText(QRScanActivity.this, "Event not found.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Exception e = task.getException();
                        Toast.makeText(QRScanActivity.this, "Error fetching event: " + (e != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    /**
     * Navigates to OrganizerEventDetailsActivity with the given eventId.
     *
     * @param eventId The ID of the event.
     */
    private void navigateToOrganizerEventDetails(String eventId) {
        Intent intent = new Intent(this, EventDetailsOrganizerActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        startActivity(intent);
        finish(); // Close QRScanActivity
    }

    /**
     * Navigates to EntrantEventDetailsActivity with the given eventId.
     *
     * @param eventId The ID of the event.
     */
    private void navigateToEntrantEventDetails(String eventId) {
        Intent intent = new Intent(this, EventDetailsEntrantActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        startActivity(intent);
        finish(); // Close QRScanActivity
    }

    /**
     * Handles the result of the permission request.
     *
     * @param requestCode  The request code passed in requestPermissions().
     * @param permissions  The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, initiate scan
                startQRScan();
            } else {
                // Permission denied, disable functionality that depends on this permission.
                Toast.makeText(this, "Camera permission is required to scan QR codes.", Toast.LENGTH_SHORT).show();
                finish(); // Close the activity
            }
        }
    }
}
