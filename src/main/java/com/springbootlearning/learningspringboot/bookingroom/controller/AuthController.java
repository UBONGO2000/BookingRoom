package com.springbootlearning.learningspringboot.bookingroom.controller;

import com.springbootlearning.learningspringboot.bookingroom.dto.UserDto;
import com.springbootlearning.learningspringboot.bookingroom.model.User;
import com.springbootlearning.learningspringboot.bookingroom.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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
    public String registerUser(@Valid @ModelAttribute("user") User user, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "pages/auth/register";
        }
        
        // Check if username already exists
        if (userService.findByUsername(user.getUsername()).isPresent()) {
            model.addAttribute("usernameError", "Ce nom d'utilisateur est deja utilise");
            return "pages/auth/register";
        }
        
        // Check if email already exists
        if (userService.findByEmail(user.getEmail()).isPresent()) {
            model.addAttribute("emailError", "Cette adresse email est deja utilisee");
            return "pages/auth/register";
        }
        
        userService.register(
                user.getFirstname(),
                user.getLastname(),
                user.getUsername(),
                user.getEmail(),
                passwordEncoder.encode(user.getPassword())
        );
        return "redirect:/login?registered";
    }
}
