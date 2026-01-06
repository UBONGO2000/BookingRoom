package com.springbootlearning.learningspringboot.bookingroom.service;

import com.springbootlearning.learningspringboot.bookingroom.model.User;

public interface UserService {
    User register(String firstname, String lastname, String username,String email, String password);
    Boolean existByUsername(String username);
}
