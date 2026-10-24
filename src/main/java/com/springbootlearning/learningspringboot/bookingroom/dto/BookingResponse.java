package com.springbootlearning.learningspringboot.bookingroom.dto;

import com.springbootlearning.learningspringboot.bookingroom.model.Booking;

import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String roomName,
        Long roomId
) {
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getTitle(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getRoom().getName(),
                booking.getRoom().getId()
        );
    }
}
