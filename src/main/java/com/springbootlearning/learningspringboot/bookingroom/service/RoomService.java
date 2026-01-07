package com.springbootlearning.learningspringboot.bookingroom.service;


import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import com.springbootlearning.learningspringboot.bookingroom.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;

import com.springbootlearning.learningspringboot.bookingroom.dto.RoomSearchDto;
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
