package com.springbootlearning.learningspringboot.bookingroom.controller;

import com.springbootlearning.learningspringboot.bookingroom.repository.UserRepository;
import com.springbootlearning.learningspringboot.bookingroom.service.BookingService;
import com.springbootlearning.learningspringboot.bookingroom.service.RoomService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/")
public class HomeController {

    private final RoomService roomService;
    private final BookingService bookingService;
    private final UserRepository userRepository;

    public HomeController(RoomService roomService, BookingService bookingService, UserRepository userRepository){
        this.roomService = roomService;
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String home(Model model, Principal principal){
        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(user -> {
                model.addAttribute("user", user);
            });
        }
        return "pages/home";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal){
        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(user -> {
                model.addAttribute("user", user);
                model.addAttribute("bookings", bookingService.getBookingsByUser(user.getUsername()));
            });
        }
        return "pages/user/dashboard";
    }
}
