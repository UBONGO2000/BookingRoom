package com.springbootlearning.learningspringboot.bookingroom.controller;

import com.springbootlearning.learningspringboot.bookingroom.dto.RoomSearchDto;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import com.springbootlearning.learningspringboot.bookingroom.model.User;
import com.springbootlearning.learningspringboot.bookingroom.repository.UserRepository;
import com.springbootlearning.learningspringboot.bookingroom.service.BookingService;
import com.springbootlearning.learningspringboot.bookingroom.service.RoomService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping
public class BookingController {


    private final RoomService roomService;
    private final BookingService bookingService;
    private final UserRepository userRepository;

    public BookingController(UserRepository userRepository, RoomService roomService, BookingService bookingService) {
        this.roomService = roomService;
        this.userRepository = userRepository;
        this.bookingService = bookingService;
    }


    @GetMapping("/booking")
    public String booking(Model model, Principal principal){
        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(user -> model.addAttribute("user", user));
        }
        model.addAttribute("rooms",roomService.getAllRooms());
        model.addAttribute("roomsearch",new RoomSearchDto());
        return "pages/user/booking";
    }


    @PostMapping("/booking")
    public String searchRooms(Model model, @ModelAttribute("roomsearch") RoomSearchDto roomSearchDto, Principal principal) {
        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(user -> model.addAttribute("user", user));
        }

        List<Room> foundRooms = roomService.searchRooms(roomSearchDto);
        if (foundRooms.isEmpty()) {
            model.addAttribute("rooms", null); // Pour déclencher le message "aucune salle"
        } else if (foundRooms.size() == 1) {
            model.addAttribute("room", foundRooms.get(0));
        } else {
            model.addAttribute("rooms", foundRooms);
        }
        return "pages/user/booking";
    }


    @GetMapping("/booking/{id}")
    public String bookingDetail(@PathVariable Long id, Model model, Principal principal, @RequestParam(required = false) String error) {
        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(user -> model.addAttribute("user", user));
        }
        roomService.getRoomById(id).ifPresent(room -> {
            model.addAttribute("room", room);
            model.addAttribute("bookings", bookingService.getBookingsByRoom(room));
        });
        if (error != null) {
            model.addAttribute("errorMessage", error);
        }
        return "pages/user/room-detail";
    }

    @PostMapping("/booking/confirm")
    public String confirmBooking(@RequestParam Long roomId, 
                                 @RequestParam String title,
                                 @RequestParam String startTime,
                                 @RequestParam String endTime,
                                 Principal principal,
                                 Model model) {
        if (principal != null) {
            try {
                User user = userRepository.findByUsername(principal.getName()).orElseThrow();
                Room room = roomService.getRoomById(roomId).orElseThrow();
                
                LocalDateTime start = LocalDateTime.parse(startTime);
                LocalDateTime end = LocalDateTime.parse(endTime);
                
                bookingService.createBooking(title, start, end, user, room);
            } catch (Exception e) {
                // Encoder le message d'erreur pour l'URL
                String encodedError = java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
                return "redirect:/booking/booking/" + roomId + "?error=" + encodedError;
            }
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/booking/cancel/{id}")
    public String cancelBooking(@PathVariable Long id, Principal principal) {
        if (principal != null) {
            bookingService.getBookingById(id).ifPresent(booking -> {
                // Vérifier si l'utilisateur est le propriétaire ou un admin
                boolean isAdmin = userRepository.findByUsername(principal.getName())
                        .map(u -> u.getRole().name().equals("ADMIN"))
                        .orElse(false);
                
                if (booking.getUser().getUsername().equals(principal.getName()) || isAdmin) {
                    bookingService.cancelBooking(id);
                }
            });
        }
        return "redirect:/dashboard";
    }
}
