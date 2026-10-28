package com.springbootlearning.learningspringboot.bookingroom.controller;

import com.springbootlearning.learningspringboot.bookingroom.service.BookingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/")
public class HomeController {

    private final BookingService bookingService;

    public HomeController(BookingService bookingService){
        this.bookingService = bookingService;
    }

    @GetMapping
    public String home(){
        // L'utilisateur courant est ajoute au modele par CurrentUserAdvice.
        return "pages/home";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal){
        if (principal != null) {
            model.addAttribute("bookings", bookingService.getUpcomingBookingsByUser(principal.getName()));
        }
        return "pages/user/dashboard";
    }
}
