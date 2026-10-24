package com.springbootlearning.learningspringboot.bookingroom.service;

import com.springbootlearning.learningspringboot.bookingroom.model.User;
import com.springbootlearning.learningspringboot.bookingroom.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User register(String firstname, String lastname, String username, String email, String password) {
        if (existByUsername(username)) {
            throw new IllegalArgumentException("Ce nom d'utilisateur est deja utilise");
        }
        if (existByEmail(email)) {
            throw new IllegalArgumentException("Cette adresse email est deja utilisee");
        }

        User user = new User(firstname, lastname, username, email, passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    @Override
    public Boolean existByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public Boolean existByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

}
