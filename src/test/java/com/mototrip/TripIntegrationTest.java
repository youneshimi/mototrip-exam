package com.mototrip;

import com.example.mototrips.MotoTripsApplication;
import com.example.mototrips.entity.Trip;
import com.example.mototrips.entity.User;
import com.example.mototrips.repository.TripRepository;
import com.example.mototrips.repository.UserRepository;
import com.example.mototrips.service.TripService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = MotoTripsApplication.class,
        properties = "spring.jpa.properties.hibernate.globally_quoted_identifiers=true"
)
@Transactional
class TripIntegrationTest {

    @Autowired
    private TripService service;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savedUserIsRetrievable() {
        User user = userRepository.save(new User("Younes", false));
        Long id = readId(user);

        entityManager.flush();
        entityManager.clear();

        Optional<User> loaded = userRepository.findById(id);

        assertTrue(loaded.isPresent());
        assertEquals("Younes", readName(loaded.get()));
        assertFalse(readPremium(loaded.get()));
        assertEquals(0, readPoints(loaded.get()));
    }

    @Test
    void savedTripIsRetrievable() {
        Trip trip = tripRepository.save(new Trip("Atlas", 3, false));
        Long id = readId(trip);

        entityManager.flush();
        entityManager.clear();

        Optional<Trip> loaded = tripRepository.findById(id);

        assertTrue(loaded.isPresent());
        assertEquals("Atlas", readName(loaded.get()));
        assertEquals(3, readMaxParticipants(loaded.get()));
        assertFalse(readPremiumOnly(loaded.get()));
        assertFalse(readStarted(loaded.get()));
    }

    @Test
    void joinTripUpdatesParticipantsAndPoints() {
        Trip trip = service.createTrip("Atlas", 2, false);
        User user = service.createUser("Younes", false);
        Long tripId = readId(trip);
        Long userId = readId(user);

        service.joinTrip(tripId, userId);

        entityManager.flush();
        entityManager.clear();

        Trip loadedTrip = tripRepository.findById(tripId).orElseThrow();
        User loadedUser = userRepository.findById(userId).orElseThrow();
        List<?> participants = readParticipants(loadedTrip);

        assertEquals(1, participants.size());
        assertEquals(10, readPoints(loadedUser));
    }

    @Test
    void startTripPersistsStartedTrue() {
        Trip trip = service.createTrip("Atlas", 2, false);
        User user = service.createUser("Younes", false);
        Long tripId = readId(trip);
        Long userId = readId(user);
        service.joinTrip(tripId, userId);

        service.startTrip(tripId);

        entityManager.flush();
        entityManager.clear();

        Trip loadedTrip = tripRepository.findById(tripId).orElseThrow();

        assertTrue(readStarted(loadedTrip));
    }

    private Long readId(Object target) {
        return (Long) readField(target, "id");
    }

    private int readPoints(Object target) {
        return (Integer) readField(target, "points");
    }

    private boolean readPremium(Object target) {
        return (Boolean) readField(target, "premium");
    }

    private boolean readPremiumOnly(Object target) {
        return (Boolean) readField(target, "premiumOnly");
    }

    private boolean readStarted(Object target) {
        return (Boolean) readField(target, "started");
    }

    private int readMaxParticipants(Object target) {
        return (Integer) readField(target, "maxParticipants");
    }

    private String readName(Object target) {
        return (String) readField(target, "name");
    }

    private List<?> readParticipants(Object target) {
        return (List<?>) readField(target, "participants");
    }

    private Object readField(Object target, String field) {
        try {
            Field declaredField = target.getClass().getDeclaredField(field);
            declaredField.setAccessible(true);
            return declaredField.get(target);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

