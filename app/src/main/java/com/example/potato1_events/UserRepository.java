package com.example.potato1_events;

import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Handles user-related data operations with Firebase Firestore.
 * <p>
 * This class abstracts the Firebase Firestore interactions for user data,
 * providing methods to check if a user exists and retrieve their information.
 * </p>
 */
public class UserRepository {

    /**
     * Firebase Firestore instance for database operations.
     */
    private FirebaseFirestore firestore;

    /**
     * Constructs a new UserRepository with the given Firestore instance.
     *
     * @param firestore The FirebaseFirestore instance to use.
     */
    public UserRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Callback interface for checking if a user exists.
     */
    public interface UserExistsCallback {
        /**
         * Called when the user data is retrieved successfully.
         *
         * @param userData The retrieved user data.
         */
        void onResult(UserData userData);

        /**
         * Called when there is an error accessing the database.
         *
         * @param e The exception that occurred.
         */
        void onError(Exception e);
    }

    /**
     * Checks if a user exists in the database and retrieves their information.
     *
     * @param deviceId  The unique device ID of the user.
     * @param callback  The callback to handle the result.
     */
    public void checkUserExists(String deviceId,
                                UserExistsCallback callback) {
        firestore.collection("Users").document(deviceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean exists = documentSnapshot.exists();
                    boolean isAdmin = false;
                    if (exists && documentSnapshot.get("admin") != null) {
                        isAdmin = Boolean.parseBoolean(
                                documentSnapshot.get("admin").toString());
                    }
                    UserData userData = new UserData(exists, isAdmin);
                    callback.onResult(userData);
                })
                .addOnFailureListener(callback::onError);
    }
}
