package com.springbootlearning.learningspringboot.bookingroom.service;

import com.springbootlearning.learningspringboot.bookingroom.model.User;

import java.util.Optional;

public interface UserService {
    User register(String firstname, String lastname, String username,String email, String password);
    Boolean existByUsername(String username);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
}
