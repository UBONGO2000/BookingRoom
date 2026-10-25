package com.springbootlearning.learningspringboot.bookingroom.service;


import com.springbootlearning.learningspringboot.bookingroom.dto.RoomSearchDto;
import com.springbootlearning.learningspringboot.bookingroom.model.BookingStatus;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import com.springbootlearning.learningspringboot.bookingroom.repository.BookingRepository;
import com.springbootlearning.learningspringboot.bookingroom.repository.RoomRepository;
import com.springbootlearning.learningspringboot.bookingroom.repository.RoomSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    public RoomService(RoomRepository roomRepository, BookingRepository bookingRepository){
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<Room> getAllRooms(){
        return roomRepository.findAll();
    }

    public Page<Room> getAllRooms(Pageable pageable) {
        return roomRepository.findAll(pageable);
    }

    // Liste publique : une salle en maintenance ne doit pas etre proposee a la reservation.
    public Page<Room> getAvailableRooms(Pageable pageable) {
        return roomRepository.findByAvailableTrue(pageable);
    }

    // Recherche paginee en base (Specification) : tri deterministe par nom puis id.
    public Page<Room> searchRooms(RoomSearchDto criteria, Pageable pageable) {
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.asc("name").ignoreCase(), Sort.Order.asc("id")));
        return roomRepository.findAll(RoomSpecifications.withCriteria(criteria), sorted);
    }

    public Optional<Room> getRoomByName(String name){
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return roomRepository.findByNameIgnoreCase(name);
    }

    public Optional<Room> getRoomById(Long id) {
        return roomRepository.findById(id);
    }

    public void updateRoomAvailability(Long id, boolean available) {
        roomRepository.findById(id).ifPresent(r -> {
            r.setAvailable(available);
            roomRepository.save(r);
        });
    }

    public Room saveRoom(Room room) {
        return roomRepository.save(room);
    }

    public void deleteRoom(Long id) {
        boolean hasFutureBookings = bookingRepository.existsByRoomIdAndEndTimeAfterAndStatus(
                id, LocalDateTime.now(), BookingStatus.CONFIRMED);
        if (hasFutureBookings) {
            throw new IllegalStateException(
                    "Impossible de supprimer cette salle : des réservations à venir existent.");
        }
        roomRepository.deleteById(id);
    }
}
