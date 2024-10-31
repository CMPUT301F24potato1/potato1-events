package com.example.potato1_events;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;


import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class Users_OpsTest {
    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private DocumentReference mockDocumentReference;

    @Mock
    private CollectionReference mockCollectionReference;

    @Mock
    private Task<Void> mockTask;

//    private EntrantRepository entrantRepository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Initialize the repository with the mocked Firestore's instance

        // Mock the behavior of Firestore's collection and document methods
        when(mockFirestore.collection("Entrants")).thenReturn(mockCollectionReference);
        when(mockCollectionReference.document(anyString())).thenReturn(mockDocumentReference);
    }

    @Test
    public void testAddEntrant_Success() {
        // Mock the behavior of the set() method returning a Task
        when(mockDocumentReference.set(any(User.class))).thenReturn(mockTask);

        User user = new User(
                "1234",
                "Entrant",
                "Xavier",
                "x@gmail.com",
                "1234",
                "profile", // holds the storage path in firebase storage
                true, // Default to notifications enabled
                System.currentTimeMillis() // Current timestamp
        );
        // Call the method under test
        Task<Void> result = mockFirestore.collection("Entrants").document("testID").set(user);
        // Verify that Firestore's set() method was called with a map containing the user's data
        verify(mockDocumentReference).set(any(User.class));

        // Check if the returned task matches the mocked task
        assertEquals(mockTask, result);
    }
}
