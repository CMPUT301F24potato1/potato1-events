package com.example.potato1_events;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;


import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Date;
import java.util.Map;

public class EventsTest {
    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private DocumentReference mockDocumentReference;

    @Mock
    private CollectionReference mockCollectionReference;

    @Mock
    private Task<Void> mockTask;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateEvent() {
        // Mock the behavior of Firestore's collection and document methods
        when(mockFirestore.collection("Events")).thenReturn(mockCollectionReference);
        when(mockCollectionReference.document(anyString())).thenReturn(mockDocumentReference);
        // Mock the behavior of the set() method returning a Task
        when(mockDocumentReference.set(any(Event.class))).thenReturn(mockTask);

        /*
        String id, String facilityId, String name, String description, Date startDate, Date endDate,
                 Date registrationStart, Date registrationEnd, double price, int capacity, int currentEntrantsNumber,
                 int waitingListCapacity, String posterImageUrl, String qrCodeHash, Map<String, String> entrants,
                 Date createdAt, String status, boolean geolocationRequired, String eventLocation */
        Event event = new Event(
                "1",
                "1",
                "Yoga",
                "classes",
                new Date(),
                new Date(System.currentTimeMillis() + 5),
                new Date(System.currentTimeMillis()),
                new Date(System.currentTimeMillis() + 5),
                50,
                30,
                0,
                20,
                "poster",
                "QR",

                new HashMap<String, String>(),
        new Date(System.currentTimeMillis()),
                "OK",
                true,
                "Laurent"
        );


        // Call the method under test
        Task<Void> result = mockFirestore.collection("Events").document("1").set(event);
        // Verify that Firestore's set() method was called with a map containing the user's data
        verify(mockDocumentReference).set(any(Event.class));

        // Check if the returned task matches the mocked task
        assertEquals(mockTask, result);
    }

    @Test
    public void testDeleteEvent(){

        when(mockFirestore.collection("Events")).thenReturn(mockCollectionReference);
        when(mockCollectionReference.document(anyString())).thenReturn(mockDocumentReference);
        // Mock the behavior of the set() method returning a Task
        when(mockDocumentReference.delete()).thenReturn(mockTask);

        // Call the method under test
        Task<Void> result = mockFirestore.collection("Events").document("1").delete();
        // Verify that Firestore's set() method was called with a map containing the user's data
        verify(mockDocumentReference).delete();

        // Check if the returned task matches the mocked task
        assertEquals(mockTask, result);



    }
}
