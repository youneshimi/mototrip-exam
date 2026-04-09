package com.mototrip;

import com.mototrip.entity.Trip;
import com.mototrip.entity.User;
import com.mototrip.repository.TripRepository;
import com.mototrip.repository.UserRepository;
import com.mototrip.service.TripService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripBranchCoverageTest {

    @Mock
    private TripRepository tripRepo;

    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private TripService service;

    @Test
    void joinWorksWhenNotStartedAndNotPremiumOnly() {
        Trip trip = new Trip("Atlas", 2, false);
        User user = new User("Younes", false);

        trip.join(user);

        assertEquals(1, trip.getParticipants().size());
        assertEquals(10, user.getPoints());
    }

    @Test
    void joinThrowsWhenTripAlreadyStarted() {
        Trip trip = new Trip("Atlas", 2, false);
        trip.join(new User("First", false));
        trip.start();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> trip.join(new User("Late", false)));

        assertEquals("Trip already started", ex.getMessage());
    }

    @Test
    void joinWorksForPremiumUserOnPremiumTrip() {
        Trip trip = new Trip("Premium", 2, true);
        User premiumUser = new User("Sara", true);

        trip.join(premiumUser);

        assertEquals(1, trip.getParticipants().size());
        assertTrue(trip.getParticipants().contains(premiumUser));
    }

    @Test
    void joinThrowsForNonPremiumUserOnPremiumTrip() {
        Trip trip = new Trip("Premium", 2, true);
        User nonPremiumUser = new User("Younes", false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> trip.join(nonPremiumUser));

        assertEquals("Premium required", ex.getMessage());
    }

    @Test
    void joinWorksWhenParticipantsBelowCapacity() {
        Trip trip = new Trip("Atlas", 2, false);

        trip.join(new User("Younes", false));

        assertEquals(1, trip.getParticipants().size());
    }

    @Test
    void joinThrowsWhenParticipantsReachCapacity() {
        Trip trip = new Trip("Atlas", 1, false);
        trip.join(new User("First", false));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> trip.join(new User("Second", false)));

        assertEquals("Trip full", ex.getMessage());
    }

    @Test
    void startWorksWhenParticipantsExist() {
        Trip trip = new Trip("Atlas", 2, false);
        trip.join(new User("Younes", false));

        trip.start();

        assertTrue(trip.isStarted());
    }

    @Test
    void startThrowsWhenParticipantsEmpty() {
        Trip trip = new Trip("Atlas", 2, false);

        RuntimeException ex = assertThrows(RuntimeException.class, trip::start);

        assertEquals("No participants", ex.getMessage());
    }

    @Test
    void createTripWorksWhenMaxPositive() {
        when(tripRepo.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Trip created = service.createTrip("Atlas", 3, false);

        assertEquals("Atlas", created.getName());
        assertEquals(3, created.getMaxParticipants());
        verify(tripRepo).save(any(Trip.class));
    }

    @Test
    void createTripThrowsWhenMaxZero() {
        RuntimeException ex = assertThrows(IllegalArgumentException.class, () -> service.createTrip("Atlas", 0, false));

        assertEquals("Invalid capacity", ex.getMessage());
        verify(tripRepo, never()).save(any());
    }

    @Test
    void createTripThrowsWhenMaxNegative() {
        RuntimeException ex = assertThrows(IllegalArgumentException.class, () -> service.createTrip("Atlas", -1, false));

        assertEquals("Invalid capacity", ex.getMessage());
        verify(tripRepo, never()).save(any());
    }

    @Test
    void joinTripWorksWhenTripAndUserFound() {
        Trip trip = new Trip("Atlas", 2, false);
        User user = new User("Younes", false);
        when(tripRepo.findById(1L)).thenReturn(Optional.of(trip));
        when(userRepo.findById(2L)).thenReturn(Optional.of(user));
        when(tripRepo.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Trip result = service.joinTrip(1L, 2L);

        assertEquals(1, result.getParticipants().size());
        verify(tripRepo).save(trip);
    }

    @Test
    void joinTripThrowsWhenTripNotFound() {
        when(tripRepo.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.joinTrip(1L, 2L));

        assertEquals("Trip not found", ex.getMessage());
        verify(userRepo, never()).findById(any());
        verify(tripRepo, never()).save(any());
    }

    @Test
    void joinTripThrowsWhenUserNotFound() {
        Trip trip = new Trip("Atlas", 2, false);
        when(tripRepo.findById(1L)).thenReturn(Optional.of(trip));
        when(userRepo.findById(2L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.joinTrip(1L, 2L));

        assertEquals("User not found", ex.getMessage());
        verify(tripRepo, never()).save(any());
    }
}

