// File: UserData.java
package com.example.potato1_events;

/**
 * Represents user data used in the application.
 * <p>
 * This class encapsulates the information about whether a user exists and
 * whether they have admin privileges. It is used to pass user information
 * between the UserRepository and other components without relying on Firebase
 * classes.
 * </p>
 */
public class UserData {
    /**
     * Indicates whether the user exists in the database.
     */
    private boolean exists;

    /**
     * Indicates whether the user has admin privileges.
     */
    private boolean isAdmin;

    /**
     * Constructs a new UserData instance.
     *
     * @param exists  True if the user exists in the database.
     * @param isAdmin True if the user has admin privileges.
     */
    public UserData(boolean exists, boolean isAdmin) {
        this.exists = exists;
        this.isAdmin = isAdmin;
    }

    /**
     * Checks if the user exists.
     *
     * @return True if the user exists, false otherwise.
     */
    public boolean exists() {
        return exists;
    }

    /**
     * Checks if the user has admin privileges.
     *
     * @return True if the user is an admin, false otherwise.
     */
    public boolean isAdmin() {
        return isAdmin;
    }
}
