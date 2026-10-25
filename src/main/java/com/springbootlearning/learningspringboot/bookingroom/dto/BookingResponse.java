package com.springbootlearning.learningspringboot.bookingroom.dto;

import com.springbootlearning.learningspringboot.bookingroom.model.Booking;
import com.springbootlearning.learningspringboot.bookingroom.model.BookingStatus;

import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String roomName,
        Long roomId,
        BookingStatus status,
        LocalDateTime cancelledAt
) {
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getTitle(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getRoom().getName(),
                booking.getRoom().getId(),
                booking.getStatus(),
                booking.getCancelledAt()
        );
    }
}
