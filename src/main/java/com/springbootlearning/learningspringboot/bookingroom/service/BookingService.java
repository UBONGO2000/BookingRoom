package com.springbootlearning.learningspringboot.bookingroom.service;

import com.springbootlearning.learningspringboot.bookingroom.model.Booking;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import com.springbootlearning.learningspringboot.bookingroom.model.User;
import com.springbootlearning.learningspringboot.bookingroom.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public List<Booking> getBookingsByUser(String username) {
        return bookingRepository.findByUserUsername(username);
    }

    public List<Booking> getBookingsByRoom(Room room) {
        return bookingRepository.findByRoomAndEndTimeAfterOrderByStartTimeAsc(room, LocalDateTime.now());
    }

    public List<Booking> searchBookings(String roomName, String username) {
        return bookingRepository.findAll().stream()
                .filter(b -> roomName == null || roomName.isEmpty() || b.getRoom().getName().toLowerCase().contains(roomName.toLowerCase()))
                .filter(b -> username == null || username.isEmpty() || b.getUser().getUsername().toLowerCase().contains(username.toLowerCase()))
                .toList();
    }

    public boolean isRoomAvailable(Long roomId, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isAfter(endTime) || startTime.isEqual(endTime)) {
            return false;
        }
        List<Booking> overlapping = bookingRepository.findOverlappingBookings(roomId, startTime, endTime);
        return overlapping.isEmpty();
    }

    public Booking createBooking(String title, LocalDateTime startTime, LocalDateTime endTime, User user, Room room) {
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Vous ne pouvez pas réserver dans le passé.");
        }
        if (!isRoomAvailable(room.getId(), startTime, endTime)) {
            throw new RuntimeException("La salle est déjà réservée pour cette plage horaire.");
        }
        if (!room.getAvailable()) {
            throw new RuntimeException("La salle n'est pas disponible pour le moment.");
        }
        
        // Vérification de la plage de disponibilité de la salle
        if (room.getAvailableFrom() != null && startTime.isBefore(room.getAvailableFrom())) {
            throw new RuntimeException("La salle n'est disponible qu'à partir du " + room.getAvailableFrom().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }
        if (room.getAvailableUntil() != null && endTime.isAfter(room.getAvailableUntil())) {
            throw new RuntimeException("La salle n'est plus disponible après le " + room.getAvailableUntil().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }

        Booking booking = new Booking(title, startTime, endTime, user, room);
        return bookingRepository.save(booking);
    }

    public void cancelBooking(Long id) {
        bookingRepository.deleteById(id);
    }

    public java.util.Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }
}
