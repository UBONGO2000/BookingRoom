package com.springbootlearning.learningspringboot.bookingroom.controller;

import com.springbootlearning.learningspringboot.bookingroom.repository.UserRepository;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

/**
 * Charge l'utilisateur connecte une seule fois pour les vues Thymeleaf.
 * La navbar et les pages n'ont plus a repeter ce lookup dans chaque controleur.
 *
 * <p>L'advice est volontairement limite aux controleurs HTML : l'API REST ne doit
 * pas payer une requete supplementaire ni exposer une entite dans son modele.</p>
 */
@ControllerAdvice(assignableTypes = {
        HomeController.class,
        BookingController.class,
        AdminController.class
})
public class CurrentUserAdvice {

    private final UserRepository userRepository;

    public CurrentUserAdvice(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute
    public void addCurrentUser(Model model, Principal principal) {
        if (principal != null) {
            userRepository.findByUsername(principal.getName())
                    .ifPresent(user -> model.addAttribute("user", user));
        }
    }
}
