package com.mototrip;

import com.example.mototrips.MotoTripsApplication;
import com.example.mototrips.entity.Trip;
import com.example.mototrips.entity.User;
import com.example.mototrips.repository.TripRepository;
import com.example.mototrips.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.lang.reflect.Field;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = MotoTripsApplication.class,
        properties = "spring.jpa.properties.hibernate.globally_quoted_identifiers=true"
)
@AutoConfigureMockMvc
@Transactional
@Import(MotoTripE2ETest.E2EExceptionAdvice.class)
class MotoTripE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        tripRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void completeScenarioWorks() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content("{\"name\":\"Younes\",\"premium\":false}"))
                .andExpect(status().isOk());
        User user = userRepository.findAll().get(0);
        Long userId = readId(user);
        assertFalse(readPremium(user));
        assertEquals(0, readPoints(user));

        mockMvc.perform(post("/api/trips")
                        .contentType("application/json")
                        .content("{\"name\":\"Atlas\",\"maxParticipants\":2,\"premiumOnly\":false}"))
                .andExpect(status().isOk());
        Trip trip = tripRepository.findAll().get(0);
        Long tripId = readId(trip);
        assertEquals(2, readMaxParticipants(trip));
        assertFalse(readPremiumOnly(trip));

        mockMvc.perform(post("/api/trips/{id}/join", tripId)
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/trips"))
                .andExpect(status().isOk());
        Trip joinedTrip = tripRepository.findById(tripId).orElseThrow();
        User joinedUser = userRepository.findById(userId).orElseThrow();
        assertEquals(1, readParticipants(joinedTrip).size());
        assertEquals(10, readPoints(joinedUser));

        mockMvc.perform(post("/api/trips/{id}/start", tripId))
                .andExpect(status().isOk());

        Trip startedTrip = tripRepository.findById(tripId).orElseThrow();
        assertTrue(readStarted(startedTrip));

        mockMvc.perform(post("/api/trips/{id}/join", tripId)
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Trip already started")));
    }

    private Long readId(Object entity) {
        return (Long) readField(entity, "id");
    }

    private int readPoints(Object entity) {
        return (Integer) readField(entity, "points");
    }

    private boolean readPremium(Object entity) {
        return (Boolean) readField(entity, "premium");
    }

    private boolean readPremiumOnly(Object entity) {
        return (Boolean) readField(entity, "premiumOnly");
    }

    private boolean readStarted(Object entity) {
        return (Boolean) readField(entity, "started");
    }

    private int readMaxParticipants(Object entity) {
        return (Integer) readField(entity, "maxParticipants");
    }

    @SuppressWarnings("unchecked")
    private List<Object> readParticipants(Object entity) {
        return (List<Object>) readField(entity, "participants");
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

    @RestControllerAdvice
    static class E2EExceptionAdvice {
        @ExceptionHandler(RuntimeException.class)
        ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }
}


