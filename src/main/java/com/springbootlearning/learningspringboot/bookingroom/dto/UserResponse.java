package com.springbootlearning.learningspringboot.bookingroom.dto;

import com.springbootlearning.learningspringboot.bookingroom.model.User;

public record UserResponse(
        Long id,
        String username,
        String email,
        String firstname,
        String lastname,
        String role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstname(),
                user.getLastname(),
                user.getRole().name()
        );
    }
}
