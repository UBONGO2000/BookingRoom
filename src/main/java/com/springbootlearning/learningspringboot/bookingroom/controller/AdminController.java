package com.springbootlearning.learningspringboot.bookingroom.controller;

import com.springbootlearning.learningspringboot.bookingroom.model.Booking;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import com.springbootlearning.learningspringboot.bookingroom.repository.UserRepository;
import com.springbootlearning.learningspringboot.bookingroom.service.BookingService;
import com.springbootlearning.learningspringboot.bookingroom.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin")
public class AdminController {

    // Tri deterministe : plus recent d'abord, id en departage.
    private static final Sort BOOKING_SORT = Sort.by(
            Sort.Order.desc("startTime"),
            Sort.Order.asc("id"));

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
                                 @RequestParam(required = false) String username,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size) {
        addUserToModel(model, principal);
        model.addAttribute("rooms", roomService.getAllRooms());

        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(safePage, safeSize, BOOKING_SORT);
        Page<Booking> bookings = bookingService.searchBookings(roomName, username, pageable);

        // Page hors limites (filtre change, URL ancienne...) : on revient sur la derniere page.
        if (bookings.getTotalPages() > 0 && safePage >= bookings.getTotalPages()) {
            safePage = bookings.getTotalPages() - 1;
            bookings = bookingService.searchBookings(
                    roomName, username, PageRequest.of(safePage, safeSize, BOOKING_SORT));
        }

        model.addAttribute("bookings", bookings);
        model.addAttribute("currentPage", bookings.getNumber());
        model.addAttribute("totalPages", bookings.getTotalPages());
        model.addAttribute("totalItems", bookings.getTotalElements());
        model.addAttribute("pageSize", bookings.getSize());
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
    public String saveRoom(@Valid @ModelAttribute("room") Room room,
                           BindingResult result,
                           Model model,
                           Principal principal) {
        // Les checkboxes Thymeleaf envoient toujours une valeur (champ cache _xxx),
        // mais un POST direct peut omettre ces champs : on evite un 500 SQL.
        if (room.getProjector() == null) {
            room.setProjector(false);
        }
        if (room.getWhiteboard() == null) {
            room.setWhiteboard(false);
        }
        if (room.getVideoconferencing() == null) {
            room.setVideoconferencing(false);
        }
        if (room.getAvailable() == null) {
            room.setAvailable(true);
        }

        String name = room.getName() == null ? null : room.getName().trim();
        room.setName(name);
        if (name != null && !name.isBlank()) {
            roomService.getRoomByName(name)
                    .filter(existing -> !existing.getId().equals(room.getId()))
                    .ifPresent(existing -> result.rejectValue(
                            "name", "duplicate", "Une salle porte déjà ce nom."));
        }

        if (result.hasErrors()) {
            addUserToModel(model, principal);
            return "pages/admin/room-form";
        }

        try {
            roomService.saveRoom(room);
        } catch (DataIntegrityViolationException e) {
            // Filet de securite si deux requetes concurrentes passent la verification ci-dessus.
            result.rejectValue("name", "duplicate", "Une salle porte déjà ce nom.");
            addUserToModel(model, principal);
            return "pages/admin/room-form";
        }
        return "redirect:/admin";
    }

    @GetMapping("/rooms/edit/{id}")
    public String editRoomForm(@PathVariable Long id, Model model, Principal principal) {
        addUserToModel(model, principal);
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cette salle n'existe pas."));
        model.addAttribute("room", room);
        return "pages/admin/room-form";
    }

    @PostMapping("/rooms/delete/{id}")
    public String deleteRoom(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            roomService.deleteRoom(id);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Impossible de supprimer cette salle : des réservations y sont rattachées. Désactivez-la plutôt.");
        }
        return "redirect:/admin";
    }

    @PostMapping("/rooms/toggle-availability/{id}")
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