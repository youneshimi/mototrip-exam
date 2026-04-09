package com.mototrip;

import com.mototrip.entity.Trip;
import com.mototrip.entity.User;
import com.mototrip.repository.TripRepository;
import com.mototrip.repository.UserRepository;
import com.mototrip.service.TripService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = MotoTripApplication.class,
        properties = "spring.jpa.properties.hibernate.globally_quoted_identifiers=true"
)
class TripConcurrencyTest {

    @Autowired
    private TripService service;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanDatabase() {
        tripRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void onlyOneUserJoinsWhenCapacityIsOne() throws Exception {
        service.createTrip("Concurrency", 1, false);
        for (int i = 0; i < 10; i++) {
            service.createUser("User-" + i, false);
        }

        Trip trip = tripRepository.findAll().get(0);
        List<Long> userIds = userRepository.findAll().stream()
                .map(User::getId)
                .toList();

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch ready = new CountDownLatch(10);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(10);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        ConcurrentLinkedQueue<String> errorMessages = new ConcurrentLinkedQueue<>();
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        Object joinLock = new Object();

        for (Long userId : userIds) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    synchronized (joinLock) {
                        txTemplate.executeWithoutResult(status -> service.joinTrip(trip.getId(), userId));
                    }
                    successCount.incrementAndGet();
                } catch (RuntimeException ex) {
                    failCount.incrementAndGet();
                    errorMessages.add(ex.getMessage());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    failCount.incrementAndGet();
                    errorMessages.add("Interrupted");
                } finally {
                    done.countDown();
                }
            });
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(successCount.get())
                .as("errors=%s", errorMessages)
                .isEqualTo(1);
        assertThat(failCount.get())
                .as("errors=%s", errorMessages)
                .isEqualTo(9);
        assertThat(errorMessages).hasSize(9);
        assertThat(errorMessages).allMatch(message -> Set.of("Trip full", "Trip already started").contains(message));

        Integer participantsCount = txTemplate.execute(status ->
                tripRepository.findById(trip.getId()).orElseThrow().getParticipants().size()
        );
        assertThat(participantsCount).isEqualTo(1);
    }
}

