package com.example.potato1_events;

import com.google.type.Date;
import com.google.type.DateTime;

public class Event {
    private String eventId;
    private String name;
    private String description;
    private String location;
    private String posterUrl;
    private int capacity;
    private int SignedUp;
    private String organizerId;
    private DateTime time;

    public Event() {
        // Default constructor
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public DateTime getTime() {
        return this.time;
    }

    public String getCapacity() {
        //FIXME Type Casting
        return new String(String.valueOf(this.capacity));
    }

    public String getSignedUp() {
        //FIXME Type Casting
        return new String(String.valueOf(this.SignedUp));
    }
}