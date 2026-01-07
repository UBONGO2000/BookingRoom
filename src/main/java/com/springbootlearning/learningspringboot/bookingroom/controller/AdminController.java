package com.springbootlearning.learningspringboot.bookingroom.controller;

import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import com.springbootlearning.learningspringboot.bookingroom.repository.UserRepository;
import com.springbootlearning.learningspringboot.bookingroom.service.BookingService;
import com.springbootlearning.learningspringboot.bookingroom.service.RoomService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final RoomService roomService;
    private final BookingService bookingService;
    private final UserRepository userRepository;

    public AdminController(RoomService roomService, BookingService bookingService, UserRepository userRepository) {
        this.roomService = roomService;
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String adminDashboard(Model model, Principal principal,
                                 @RequestParam(required = false) String roomName,
                                 @RequestParam(required = false) String username) {
        addUserToModel(model, principal);
        model.addAttribute("rooms", roomService.getAllRooms());
        
        if ((roomName != null && !roomName.isEmpty()) || (username != null && !username.isEmpty())) {
            model.addAttribute("bookings", bookingService.searchBookings(roomName, username));
        } else {
            model.addAttribute("bookings", bookingService.getAllBookings());
        }
        
        model.addAttribute("roomName", roomName);
        model.addAttribute("username", username);
        return "pages/admin/dashboard";
    }

    @GetMapping("/rooms/new")
    public String newRoomForm(Model model, Principal principal) {
        addUserToModel(model, principal);
        model.addAttribute("room", new Room());
        return "pages/admin/room-form";
    }

    @PostMapping("/rooms/save")
    public String saveRoom(@ModelAttribute Room room) {
        roomService.saveRoom(room);
        return "redirect:/admin";
    }

    @GetMapping("/rooms/edit/{id}")
    public String editRoomForm(@PathVariable Long id, Model model, Principal principal) {
        addUserToModel(model, principal);
        roomService.getRoomById(id).ifPresent(room -> model.addAttribute("room", room));
        return "pages/admin/room-form";
    }

    @GetMapping("/rooms/delete/{id}")
    public String deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return "redirect:/admin";
    }

    @GetMapping("/rooms/toggle-availability/{id}")
    public String toggleAvailability(@PathVariable Long id) {
        roomService.getRoomById(id).ifPresent(room -> {
            roomService.updateRoomAvailability(id, !room.getAvailable());
        });
        return "redirect:/admin";
    }

    private void addUserToModel(Model model, Principal principal) {
        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(user -> model.addAttribute("user", user));
        }
    }
}
