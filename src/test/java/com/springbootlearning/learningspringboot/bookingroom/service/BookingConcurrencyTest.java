package com.springbootlearning.learningspringboot.bookingroom.service;

import com.springbootlearning.learningspringboot.bookingroom.model.Booking;
import com.springbootlearning.learningspringboot.bookingroom.model.BookingStatus;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import com.springbootlearning.learningspringboot.bookingroom.model.User;
import com.springbootlearning.learningspringboot.bookingroom.repository.BookingRepository;
import com.springbootlearning.learningspringboot.bookingroom.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deux creations simultanees sur le meme creneau : le verrou pessimiste de la
 * salle doit en laisser passer une seule, l'autre recoit une erreur metier.
 */
@SpringBootTest
@ActiveProfiles({"dev", "test"})
class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void twoSimultaneousBookingsOnTheSameSlot_onlyOneShouldSucceed() throws Exception {
        Room room = roomRepository.findByNameIgnoreCase("SOLEIL").orElseThrow();
        User user = userService.register(
                "Concurrent", "User", "concurrency_user", "concurrency_user@test.com", "Password@123");
        LocalDateTime start = LocalDateTime.now().plusDays(50).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Long> createdIds = new ArrayList<>();
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);

            Callable<Object> attempt = () -> {
                ready.countDown();
                go.await(10, TimeUnit.SECONDS);
                try {
                    return bookingService.createBooking("Concurrence", start, end, user, room);
                } catch (RuntimeException e) {
                    return e.getMessage();
                }
            };

            Future<Object> first = executor.submit(attempt);
            Future<Object> second = executor.submit(attempt);

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();

            List<Object> results = List.of(
                    first.get(20, TimeUnit.SECONDS),
                    second.get(20, TimeUnit.SECONDS));

            List<Booking> successes = results.stream()
                    .filter(Booking.class::isInstance)
                    .map(Booking.class::cast)
                    .toList();
            successes.forEach(b -> createdIds.add(b.getId()));

            List<Object> failures = results.stream()
                    .filter(result -> !(result instanceof Booking))
                    .toList();

            assertThat(successes).hasSize(1);
            assertThat(failures).containsExactly("La salle est déjà réservée pour cette plage horaire.");

            // Une seule ligne en base pour ce creneau.
            assertThat(bookingRepository.findOverlappingBookings(
                    room.getId(), start, end, BookingStatus.CONFIRMED))
                    .extracting(Booking::getId)
                    .containsExactly(successes.get(0).getId());
        } finally {
            executor.shutdownNow();
            createdIds.forEach(id -> bookingRepository.findById(id).ifPresent(bookingRepository::delete));
        }
    }
}
