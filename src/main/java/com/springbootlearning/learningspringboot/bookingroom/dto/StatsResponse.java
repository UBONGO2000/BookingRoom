package com.springbootlearning.learningspringboot.bookingroom.dto;

public record StatsResponse(
        long totalRooms,
        long totalBookings,
        long totalUsers,
        long availableRooms
) {
}
