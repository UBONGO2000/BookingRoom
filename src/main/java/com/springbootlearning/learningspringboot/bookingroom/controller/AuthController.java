package com.springbootlearning.learningspringboot.bookingroom.controller;

import com.springbootlearning.learningspringboot.bookingroom.dto.RegisterFormDto;
import com.springbootlearning.learningspringboot.bookingroom.exception.UserAlreadyExistsException;
import com.springbootlearning.learningspringboot.bookingroom.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "pages/auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new RegisterFormDto());
        return "pages/auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") RegisterFormDto form,
                               BindingResult result) {
        if (result.hasErrors()) {
            return "pages/auth/register";
        }

        try {
            // Encodage du mot de passe et verification d'unicite username/email
            // sont assures par UserService : le controleur ne manipule pas l'entite User.
            userService.register(
                    form.getFirstname(),
                    form.getLastname(),
                    form.getUsername(),
                    form.getEmail(),
                    form.getPassword()
            );
        } catch (UserAlreadyExistsException ex) {
            result.rejectValue(ex.getField(), "duplicate", ex.getMessage());
            return "pages/auth/register";
        }

        return "redirect:/login?registered";
    }
}
