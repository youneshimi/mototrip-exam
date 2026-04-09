package com.mototrip;

import com.mototrip.entity.Trip;
import com.mototrip.entity.User;
import com.mototrip.repository.TripRepository;
import com.mototrip.repository.UserRepository;
import com.mototrip.service.TripService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class TripServiceTest {

    @Mock
    private TripRepository tripRepo;

    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private TripService service;

    @Test
    void createTripNominalCallsSave() {
        ArgumentCaptor<Trip> captor = ArgumentCaptor.forClass(Trip.class);
        when(tripRepo.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Trip trip = service.createTrip("Atlas", 3, true);

        verify(tripRepo).save(captor.capture());
        assertEquals("Atlas", captor.getValue().getName());
        assertEquals(3, captor.getValue().getMaxParticipants());
        assertTrue(captor.getValue().isPremiumOnly());
        assertEquals(trip, captor.getValue());
    }

    @Test
    void createTripCapacityZeroThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.createTrip("Atlas", 0, false));
        verify(tripRepo, never()).save(any());
    }

    @Test
    void createTripNegativeCapacityThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.createTrip("Atlas", -1, false));
        verify(tripRepo, never()).save(any());
    }

    @Test
    void joinTripNominalSavesTrip() {
        Trip trip = new Trip("Atlas", 2, false);
        User user = new User("Younes", false);
        when(tripRepo.findById(1L)).thenReturn(Optional.of(trip));
        when(userRepo.findById(2L)).thenReturn(Optional.of(user));
        when(tripRepo.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Trip result = service.joinTrip(1L, 2L);

        assertEquals(1, result.getParticipants().size());
        assertTrue(result.getParticipants().contains(user));
        verify(tripRepo).save(trip);
        assertEquals(10, user.getPoints());
    }

    @Test
    void joinTripMissingTripThrows() {
        when(tripRepo.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.joinTrip(1L, 2L));

        assertEquals("Trip not found", ex.getMessage());
        verify(userRepo, never()).findById(any());
        verify(tripRepo, never()).save(any());
    }

    @Test
    void joinTripMissingUserThrows() {
        Trip trip = new Trip("Atlas", 2, false);
        when(tripRepo.findById(1L)).thenReturn(Optional.of(trip));
        when(userRepo.findById(2L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.joinTrip(1L, 2L));

        assertEquals("User not found", ex.getMessage());
        verify(tripRepo, never()).save(any());
    }

    @Test
    void startTripNominalSetsStartedTrue() {
        Trip trip = new Trip("Atlas", 2, false);
        trip.join(new User("Younes", false));
        when(tripRepo.findById(1L)).thenReturn(Optional.of(trip));
        when(tripRepo.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Trip result = service.startTrip(1L);

        assertTrue(result.isStarted());
        verify(tripRepo).save(trip);
    }

    @Test
    void startTripMissingTripThrows() {
        when(tripRepo.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.startTrip(1L));

        assertEquals("Trip not found", ex.getMessage());
        verify(tripRepo, never()).save(any());
    }
}
