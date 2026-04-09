package com.mototrip;

import com.mototrip.entity.Trip;
import com.mototrip.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripTest {

    @Test
    void joinNominalWorks() {
        Trip trip = new Trip("Atlas", 2, false);
        User user = new User("Younes", false);

        trip.join(user);

        assertEquals(1, trip.getParticipants().size());
        assertTrue(trip.getParticipants().contains(user));
        assertEquals(10, user.getPoints());
    }

    @Test
    void startNominalWorks() {
        Trip trip = new Trip("Atlas", 2, false);
        User user = new User("Younes", false);
        trip.join(user);

        trip.start();

        assertTrue(trip.isStarted());
    }

    @Test
    void remainingPlacesIsCorrect() {
        Trip trip = new Trip("Atlas", 3, false);
        User user1 = new User("Younes", false);
        User user2 = new User("Sara", false);

        trip.join(user1);
        trip.join(user2);

        assertEquals(1, trip.remainingPlaces());
    }

    @Test
    void joinThrowsWhenTripAlreadyStarted() {
        Trip trip = new Trip("Atlas", 2, false);
        User first = new User("Younes", false);
        User second = new User("Sara", false);
        trip.join(first);
        trip.start();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> trip.join(second));

        assertEquals("Trip already started", ex.getMessage());
    }

    @Test
    void joinThrowsWhenPremiumRequired() {
        Trip trip = new Trip("Atlas", 2, true);
        User user = new User("Younes", false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> trip.join(user));

        assertEquals("Premium required", ex.getMessage());
    }

    @Test
    void joinThrowsWhenTripFull() {
        Trip trip = new Trip("Atlas", 1, false);
        User first = new User("Younes", false);
        User second = new User("Sara", false);
        trip.join(first);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> trip.join(second));

        assertEquals("Trip full", ex.getMessage());
    }

    @Test
    void startThrowsWhenNoParticipants() {
        Trip trip = new Trip("Atlas", 2, false);

        RuntimeException ex = assertThrows(RuntimeException.class, trip::start);

        assertEquals("No participants", ex.getMessage());
    }
}

