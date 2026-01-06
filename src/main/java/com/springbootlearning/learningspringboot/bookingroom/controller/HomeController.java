package com.springbootlearning.learningspringboot.bookingroom.controller;

import com.springbootlearning.learningspringboot.bookingroom.RoomService;
import com.springbootlearning.learningspringboot.bookingroom.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/")
public class HomeController {

    private final RoomService roomService;
    private final UserRepository userRepository;

    public HomeController(RoomService roomService, UserRepository userRepository){
        this.roomService = roomService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String home(Model model, Principal principal){
        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(user -> model.addAttribute("user", user));
        }
        return "pages/home";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal){
        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(user -> model.addAttribute("user", user));
        }
        model.addAttribute("rooms",roomService.getAllRooms());
        return "pages/user/dashboard";
    }

    @GetMapping("/booking")
    public String booking(Model model, Principal principal){
        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(user -> model.addAttribute("user", user));
        }
        model.addAttribute("rooms",roomService.getAllRooms());
        return "pages/user/booking";
    }

}
