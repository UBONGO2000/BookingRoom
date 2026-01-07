package com.springbootlearning.learningspringboot.bookingroom.controller;

import com.springbootlearning.learningspringboot.bookingroom.dto.UserDto;
import com.springbootlearning.learningspringboot.bookingroom.model.User;
import com.springbootlearning.learningspringboot.bookingroom.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login() {
        return "pages/auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new User());
        return "pages/auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user) {
        userService.register(
                user.getFirstname(),
                user.getLastname(),
                user.getUsername(),
                user.getEmail(),
                passwordEncoder.encode(user.getPassword())
        );
        return "redirect:/login";
    }
}
