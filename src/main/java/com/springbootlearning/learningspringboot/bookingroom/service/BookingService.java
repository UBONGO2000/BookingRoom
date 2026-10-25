package com.springbootlearning.learningspringboot.bookingroom.service;

import com.springbootlearning.learningspringboot.bookingroom.model.Booking;
import com.springbootlearning.learningspringboot.bookingroom.model.BookingStatus;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import com.springbootlearning.learningspringboot.bookingroom.model.User;
import com.springbootlearning.learningspringboot.bookingroom.exception.BookingConflictException;
import com.springbootlearning.learningspringboot.bookingroom.repository.BookingRepository;
import com.springbootlearning.learningspringboot.bookingroom.repository.RoomRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    public BookingService(BookingRepository bookingRepository, RoomRepository roomRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
    }

    public List<Booking> getBookingsByUser(String username) {
        return bookingRepository.findByUserUsername(username);
    }

    public Page<Booking> getBookingsByUser(String username, Pageable pageable) {
        return bookingRepository.findByUserUsername(username, pageable);
    }

    // Dashboard : uniquement les reservations confirmees a venir.
    public List<Booking> getUpcomingBookingsByUser(String username) {
        return bookingRepository.findByUserUsernameAndEndTimeAfterAndStatusOrderByStartTimeAsc(
                username, LocalDateTime.now(), BookingStatus.CONFIRMED);
    }

    public List<Booking> getBookingsByRoom(Room room) {
        return bookingRepository.findByRoomAndEndTimeAfterAndStatusOrderByStartTimeAsc(
                room, LocalDateTime.now(), BookingStatus.CONFIRMED);
    }

    // Recherche paginee en base : le filtrage ne charge plus toutes les reservations.
    public Page<Booking> searchBookings(String roomName, String username, Pageable pageable) {
        return bookingRepository.searchBookings(
                toLikePattern(roomName),
                toLikePattern(username),
                pageable);
    }

    private static String toLikePattern(String value) {
        if (value == null || value.isBlank()) {
            return "%";
        }
        return "%" + value.trim().toLowerCase() + "%";
    }

    public boolean isRoomAvailable(Long roomId, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isAfter(endTime) || startTime.isEqual(endTime)) {
            return false;
        }
        // Une reservation annulee ne bloque plus le creneau.
        List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                roomId, startTime, endTime, BookingStatus.CONFIRMED);
        return overlapping.isEmpty();
    }

    @Transactional
    public Booking createBooking(String title, LocalDateTime startTime, LocalDateTime endTime, User user, Room room) {
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Vous ne pouvez pas réserver dans le passé.");
        }

        // Verrou pessimiste sur la salle : deux creations simultanees sont
        // serialisees, la seconde voit la reservation de la premiere et echoue.
        Room lockedRoom = roomRepository.findByIdForUpdate(room.getId())
                .orElseThrow(() -> new RuntimeException("La salle n'existe pas."));

        if (!isRoomAvailable(lockedRoom.getId(), startTime, endTime)) {
            throw new BookingConflictException("La salle est déjà réservée pour cette plage horaire.");
        }
        if (!lockedRoom.getAvailable()) {
            throw new RuntimeException("La salle n'est pas disponible pour le moment.");
        }

        // Vérification de la plage de disponibilité de la salle
        if (lockedRoom.getAvailableFrom() != null && startTime.isBefore(lockedRoom.getAvailableFrom())) {
            throw new RuntimeException("La salle n'est disponible qu'à partir du " + lockedRoom.getAvailableFrom().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }
        if (lockedRoom.getAvailableUntil() != null && endTime.isAfter(lockedRoom.getAvailableUntil())) {
            throw new RuntimeException("La salle n'est plus disponible après le " + lockedRoom.getAvailableUntil().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }

        Booking booking = new Booking(title, startTime, endTime, user, lockedRoom);
        return bookingRepository.save(booking);
    }

    // Annulation logique : la ligne reste en base pour l'historique.
    @Transactional
    public void cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cette réservation n'existe pas."));
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return;
        }
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);
    }

    public java.util.Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }
}
