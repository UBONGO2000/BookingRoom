package com.springbootlearning.learningspringboot.bookingroom.controller;

import com.springbootlearning.learningspringboot.bookingroom.dto.BookingFormDto;
import com.springbootlearning.learningspringboot.bookingroom.dto.RoomSearchDto;
import com.springbootlearning.learningspringboot.bookingroom.model.Booking;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import com.springbootlearning.learningspringboot.bookingroom.model.User;
import com.springbootlearning.learningspringboot.bookingroom.repository.UserRepository;
import com.springbootlearning.learningspringboot.bookingroom.service.BookingService;
import com.springbootlearning.learningspringboot.bookingroom.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

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
    public String booking(Model model, Principal principal,
                          @ModelAttribute("roomsearch") RoomSearchDto criteria,
                          @RequestParam(defaultValue = "0") int page) {
        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(user -> model.addAttribute("user", user));
        }

        int safePage = Math.max(page, 0);
        Page<Room> roomPage = roomService.searchRooms(criteria, PageRequest.of(safePage, 6));

        // Page hors limites (filtre modifie, ancienne URL...) : on revient sur la derniere page.
        if (roomPage.getTotalPages() > 0 && safePage >= roomPage.getTotalPages()) {
            safePage = roomPage.getTotalPages() - 1;
            roomPage = roomService.searchRooms(criteria, PageRequest.of(safePage, 6));
        }

        model.addAttribute("rooms", roomPage.getContent());
        model.addAttribute("currentPage", roomPage.getNumber());
        model.addAttribute("totalPages", roomPage.getTotalPages());
        return "pages/user/booking";
    }


    @GetMapping("/booking/{id}")
    public String bookingDetail(@PathVariable Long id, Model model, Principal principal) {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cette salle n'existe pas."));

        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(user -> model.addAttribute("user", user));
        }
        model.addAttribute("room", room);
        model.addAttribute("bookings", bookingService.getBookingsByRoom(room));
        if (!model.containsAttribute("bookingForm")) {
            model.addAttribute("bookingForm", new BookingFormDto(room.getId(), null, null, null));
        }
        return "pages/user/room-detail";
    }

    @PostMapping("/booking/confirm")
    public String confirmBooking(@Valid @ModelAttribute("bookingForm") BookingFormDto bookingForm,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes redirectAttributes,
                                 Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentification requise pour réserver une salle.");
        }

        if (result.hasErrors()) {
            if (bookingForm.roomId() == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Veuillez sélectionner une salle.");
                return "redirect:/booking";
            }
            Room room = roomService.getRoomById(bookingForm.roomId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cette salle n'existe pas."));
            userRepository.findByUsername(principal.getName()).ifPresent(user -> model.addAttribute("user", user));
            model.addAttribute("room", room);
            model.addAttribute("bookings", bookingService.getBookingsByRoom(room));
            return "pages/user/room-detail";
        }

        Room room = roomService.getRoomById(bookingForm.roomId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cette salle n'existe pas."));
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur inconnu."));

        try {
            bookingService.createBooking(bookingForm.title(), bookingForm.startTime(), bookingForm.endTime(), user, room);
        } catch (RuntimeException e) {
            // PRG : le message voyage en session, plus de ?error=... dans l'URL.
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/booking/" + room.getId();
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/booking/cancel/{id}")
    public String cancelBooking(@PathVariable Long id, Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentification requise pour annuler une réservation.");
        }

        Booking booking = bookingService.getBookingById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cette réservation n'existe pas."));

        // Vérifier si l'utilisateur est le propriétaire ou un admin
        boolean isAdmin = userRepository.findByUsername(principal.getName())
                .map(u -> u.getRole().name().equals("ADMIN"))
                .orElse(false);

        if (!booking.getUser().getUsername().equals(principal.getName()) && !isAdmin) {
            throw new AccessDeniedException("Vous ne pouvez pas annuler la réservation d'un autre utilisateur.");
        }

        bookingService.cancelBooking(id);
        return "redirect:/dashboard";
    }
}
