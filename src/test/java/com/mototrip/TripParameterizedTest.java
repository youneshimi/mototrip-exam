package com.mototrip;

import com.mototrip.entity.Trip;
import com.mototrip.entity.User;
import com.mototrip.repository.TripRepository;
import com.mototrip.repository.UserRepository;
import com.mototrip.service.TripService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripParameterizedTest {

    @Mock
    private TripRepository tripRepo;

    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private TripService service;

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10, -100})
    void createTripThrowsForInvalidCapacity(int capacity) {
        assertThrows(IllegalArgumentException.class, () -> service.createTrip("Atlas", capacity, false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Paris", "Lyon", "Moto66"})
    void createTripUsesProvidedName(String tripName) {
        when(tripRepo.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Trip created = service.createTrip(tripName, 2, false);

        assertEquals(tripName, created.getName());
        verify(tripRepo).save(any(Trip.class));
    }

    @ParameterizedTest
    @CsvSource({"true,true", "false,false"})
    void premiumTripAllowsOnlyPremiumUsers(boolean isPremiumUser, boolean shouldJoin) {
        Trip trip = new Trip("Premium Ride", 2, true);
        User user = new User("Rider", isPremiumUser);

        if (shouldJoin) {
            trip.join(user);
            assertEquals(1, trip.getParticipants().size());
        } else {
            RuntimeException ex = assertThrows(RuntimeException.class, () -> trip.join(user));
            assertEquals("Premium required", ex.getMessage());
        }
    }

    @ParameterizedTest
    @CsvSource({"1,0", "2,1", "5,4"})
    void remainingPlacesAfterOneJoin(int maxParticipants, int expectedRemaining) {
        Trip trip = new Trip("Atlas", maxParticipants, false);
        User user = new User("Younes", false);

        trip.join(user);

        assertEquals(expectedRemaining, trip.remainingPlaces());
    }
}

