package com.mototrip;

import com.example.mototrips.MotoTripsApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.Import;
import com.example.mototrips.repository.TripRepository;
import com.example.mototrips.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = MotoTripsApplication.class,
        properties = "spring.jpa.properties.hibernate.globally_quoted_identifiers=true"
)
@AutoConfigureMockMvc
@Import(TripControllerTest.TestExceptionAdvice.class)
class TripControllerTest {

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
    void postUsersReturns200() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Younes\",\"premium\":false}"))
                .andExpect(status().isOk());
    }

    @Test
    void postTripsReturns200() throws Exception {
        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Atlas\",\"maxParticipants\":3,\"premiumOnly\":false}"))
                .andExpect(status().isOk());
    }

    @Test
    void postJoinReturns200() throws Exception {
        Long userId = createUser();
        Long tripId = createTrip();

        mockMvc.perform(post("/api/trips/{id}/join", tripId)
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk());
    }

    @Test
    void postStartReturns200() throws Exception {
        Long userId = createUser();
        Long tripId = createTrip();
        mockMvc.perform(post("/api/trips/{id}/join", tripId)
                .param("userId", String.valueOf(userId)));

        mockMvc.perform(post("/api/trips/{id}/start", tripId))
                .andExpect(status().isOk());
    }

    @Test
    void getTripsReturns200AndNonNullList() throws Exception {
        createTrip();

        mockMvc.perform(get("/api/trips"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not("")));
    }

    @Test
    void postTripsInvalidCapacityReturns500() throws Exception {
        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Atlas\",\"maxParticipants\":0,\"premiumOnly\":false}"))
                .andExpect(status().isInternalServerError());
    }

    private Long createUser() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Younes\",\"premium\":false}"))
                .andExpect(status().isOk());
        return readId(userRepository.findAll().get(0));
    }

    private Long createTrip() throws Exception {
        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Atlas\",\"maxParticipants\":2,\"premiumOnly\":false}"))
                .andExpect(status().isOk());
        return readId(tripRepository.findAll().get(0));
    }

    private Long readId(Object entity) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            return (Long) field.get(entity);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @RestControllerAdvice
    static class TestExceptionAdvice {
        @ExceptionHandler(IllegalArgumentException.class)
        ResponseEntity<Void> handleIllegalArgumentException() {
            return ResponseEntity.internalServerError().build();
        }
    }
}

