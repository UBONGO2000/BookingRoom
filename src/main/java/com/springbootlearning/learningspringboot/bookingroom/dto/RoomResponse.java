package com.springbootlearning.learningspringboot.bookingroom.dto;

import com.springbootlearning.learningspringboot.bookingroom.model.Room;

import java.time.LocalDateTime;

public record RoomResponse(
        Long id,
        String name,
        String description,
        Integer capacity,
        String location,
        Boolean projector,
        Boolean whiteboard,
        Boolean videoconferencing,
        Boolean available,
        LocalDateTime availableFrom,
        LocalDateTime availableUntil
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.getCapacity(),
                room.getLocation(),
                room.getProjector(),
                room.getWhiteboard(),
                room.getVideoconferencing(),
                room.getAvailable(),
                room.getAvailableFrom(),
                room.getAvailableUntil()
        );
    }
}
