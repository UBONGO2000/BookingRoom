package com.springbootlearning.learningspringboot.bookingroom.service;

import com.springbootlearning.learningspringboot.bookingroom.model.Booking;
import com.springbootlearning.learningspringboot.bookingroom.model.BookingStatus;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import com.springbootlearning.learningspringboot.bookingroom.model.User;
import com.springbootlearning.learningspringboot.bookingroom.repository.BookingRepository;
import com.springbootlearning.learningspringboot.bookingroom.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifie le cycle de vie complet d'une reservation contre la vraie base (H2) :
 * conflits, annulation logique et filtrage des listes.
 */
@SpringBootTest
@ActiveProfiles({"dev", "test"})
@Transactional
class BookingLifecycleTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private Room room;

    @BeforeEach
    void setUp() {
        room = roomRepository.findByNameIgnoreCase("SOLEIL").orElseThrow();
    }

    private User newUser(String username) {
        return userService.register("Test", "Lifecycle", username, username + "@test.com", "Password@123");
    }

    @Test
    void cancelledBooking_shouldFreeTheSlot() {
        // Given : un premier utilisateur reserve un creneau
        User owner = newUser("lifecycle_owner_1");
        User other = newUser("lifecycle_other_1");
        LocalDateTime start = LocalDateTime.now().plusDays(40).withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(1);

        Booking first = bookingService.createBooking("Premiere", start, end, owner, room);

        // When : la reservation est annulee, puis le creneau est re-reserve
        bookingService.cancelBooking(first.getId());
        Booking second = bookingService.createBooking("Deuxieme", start, end, other, room);

        // Then : le creneau n'est plus considere comme occupe
        assertThat(second.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(bookingRepository.findOverlappingBookings(
                room.getId(), start, end, BookingStatus.CONFIRMED))
                .extracting(Booking::getId)
                .contains(second.getId())
                .doesNotContain(first.getId());
    }

    @Test
    void lists_shouldExcludeCancelledAndPastBookings() {
        // Given : une reservation annulee, une passee, une a venir
        User user = newUser("lifecycle_user_2");
        LocalDateTime start = LocalDateTime.now().plusDays(41).withHour(14).withMinute(0).withSecond(0).withNano(0);

        Booking cancelled = bookingService.createBooking("Annulee", start, start.plusHours(1), user, room);
        bookingService.cancelBooking(cancelled.getId());

        Booking upcoming = bookingService.createBooking(
                "A venir", start.plusHours(3), start.plusHours(4), user, room);

        Booking past = new Booking(
                "Passee",
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusDays(2),
                user,
                room);
        past = bookingRepository.save(past);

        // When / Then : le dashboard utilisateur ignore les annulations et le passe
        assertThat(bookingService.getUpcomingBookingsByUser(user.getUsername()))
                .extracting(Booking::getId)
                .contains(upcoming.getId())
                .doesNotContain(cancelled.getId(), past.getId());

        // Then : les occupations d'une salle suivent la meme regle
        assertThat(bookingService.getBookingsByRoom(room))
                .extracting(Booking::getId)
                .contains(upcoming.getId())
                .doesNotContain(cancelled.getId(), past.getId());
    }
}
