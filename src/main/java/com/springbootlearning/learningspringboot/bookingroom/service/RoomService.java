package com.springbootlearning.learningspringboot.bookingroom.service;


import com.springbootlearning.learningspringboot.bookingroom.dto.RoomSearchDto;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import com.springbootlearning.learningspringboot.bookingroom.repository.RoomRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository){
        this.roomRepository = roomRepository;
    }

    public List<Room> getAllRooms(){
        return roomRepository.findAll();
    }

    public Page<Room> getAllRooms(Pageable pageable) {
        return roomRepository.findAll(pageable);
    }

    public Page<Room> searchRooms(RoomSearchDto criteria, Pageable pageable) {
        List<Room> allFound = roomRepository.findAll().stream()
                .filter(r -> criteria.getName() == null || criteria.getName().isBlank() || r.getName().toLowerCase().contains(criteria.getName().toLowerCase()))
                .filter(r -> criteria.getCapacity() == null || r.getCapacity() >= criteria.getCapacity())
                .filter(r -> !criteria.isProjector() || r.getProjector())
                .filter(r -> !criteria.isWhiteboard() || r.getWhiteboard())
                .filter(r -> !criteria.isVideoconferencing() || r.getVideoconferencing())
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allFound.size());

        if (start > allFound.size()) {
            return new PageImpl<>(List.of(), pageable, allFound.size());
        }

        return new PageImpl<>(allFound.subList(start, end), pageable, allFound.size());
    }

    public List<Room> searchRooms(RoomSearchDto criteria) {
        return roomRepository.findAll().stream()
                .filter(r -> criteria.getName() == null || criteria.getName().isBlank() || r.getName().toLowerCase().contains(criteria.getName().toLowerCase()))
                .filter(r -> criteria.getCapacity() == null || r.getCapacity() >= criteria.getCapacity())
                .filter(r -> !criteria.isProjector() || r.getProjector())
                .filter(r -> !criteria.isWhiteboard() || r.getWhiteboard())
                .filter(r -> !criteria.isVideoconferencing() || r.getVideoconferencing())
                .toList();
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
        roomRepository.deleteById(id);
    }
}
