package com.example.potato1_events;

public class User {
    private String name;
    private String email;
    private String phoneNumber;
    private String profilePictureUrl;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String name, String email, String phoneNumber, String profilePictureUrl) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.profilePictureUrl = profilePictureUrl;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

}
