package com.springbootlearning.learningspringboot.bookingroom.controller;

import com.springbootlearning.learningspringboot.bookingroom.dto.RoomSearchDto;
import com.springbootlearning.learningspringboot.bookingroom.model.Booking;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import com.springbootlearning.learningspringboot.bookingroom.model.User;
import com.springbootlearning.learningspringboot.bookingroom.repository.BookingRepository;
import com.springbootlearning.learningspringboot.bookingroom.repository.RoomRepository;
import com.springbootlearning.learningspringboot.bookingroom.repository.UserRepository;
import com.springbootlearning.learningspringboot.bookingroom.service.BookingService;
import com.springbootlearning.learningspringboot.bookingroom.service.RoomService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BookingService bookingService;
    private final RoomService roomService;
    private final AuthenticationManager authenticationManager;

    public ApiController(RoomRepository roomRepository, 
                         BookingRepository bookingRepository, 
                         UserRepository userRepository,
                         BookingService bookingService,
                         RoomService roomService,
                         AuthenticationManager authenticationManager) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.bookingService = bookingService;
        this.roomService = roomService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Authentification réussie");
        response.put("username", username);
        // Note: Dans une vraie application, on retournerait un JWT ici.
        // Ici, on utilise la session Spring Security.
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rooms")
    public ResponseEntity<Page<Room>> getRooms(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(defaultValue = "false") boolean projector,
            @RequestParam(defaultValue = "false") boolean whiteboard,
            @RequestParam(defaultValue = "false") boolean videoconferencing,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        RoomSearchDto criteria = new RoomSearchDto(name, capacity, projector, whiteboard, videoconferencing);
        Page<Room> rooms = roomService.searchRooms(criteria, PageRequest.of(page, size));
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/rooms/{id}")
    public ResponseEntity<Map<String, Object>> getRoomById(@PathVariable Long id) {
        return roomService.getRoomById(id)
                .map(room -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("room", room);
                    // Le système d'évaluations n'est pas encore implémenté dans le modèle de données,
                    // donc nous retournons une liste vide pour l'instant comme placeholder.
                    response.put("evaluations", List.of()); 
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping({"/reservations/me", "/my-bookings"})
    public ResponseEntity<List<Map<String, Object>>> getMyBookings(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        
        List<Booking> bookings = bookingService.getBookingsByUser(principal.getName());
        List<Map<String, Object>> result = bookings.stream().map(b -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId());
            map.put("title", b.getTitle());
            map.put("startTime", b.getStartTime());
            map.put("endTime", b.getEndTime());
            map.put("roomName", b.getRoom().getName());
            return map;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reservations")
    public ResponseEntity<?> createBooking(@RequestBody Map<String, Object> request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            String title = (String) request.get("title");
            LocalDateTime startTime = LocalDateTime.parse((String) request.get("startTime"));
            LocalDateTime endTime = LocalDateTime.parse((String) request.get("endTime"));
            Long roomId = Long.valueOf(request.get("roomId").toString());

            User user = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            Room room = roomService.getRoomById(roomId)
                    .orElseThrow(() -> new RuntimeException("Salle non trouvée"));

            Booking booking = bookingService.createBooking(title, startTime, endTime, user, room);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", booking.getId());
            response.put("message", "Réservation créée avec succès");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/stats/overview")
    public ResponseEntity<Map<String, Object>> getOverviewStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRooms", roomRepository.count());
        stats.put("totalBookings", bookingRepository.count());
        stats.put("totalUsers", userRepository.count());
        
        long availableRooms = roomRepository.findAll().stream()
                .filter(Room::getAvailable)
                .count();
        stats.put("availableRooms", availableRooms);
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/rooms/available-now")
    public ResponseEntity<List<Room>> getRoomsAvailableNow() {
        List<Room> availableRooms = roomRepository.findAll().stream()
                .filter(Room::getAvailable)
                .filter(room -> bookingService.isRoomAvailable(room.getId(), 
                        java.time.LocalDateTime.now(), 
                        java.time.LocalDateTime.now().plusHours(1)))
                .collect(Collectors.toList());
        return ResponseEntity.ok(availableRooms);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        
        return userRepository.findByUsername(principal.getName())
                .map(user -> {
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("username", user.getUsername());
                    userData.put("email", user.getEmail());
                    userData.put("firstname", user.getFirstname());
                    userData.put("lastname", user.getLastname());
                    userData.put("role", user.getRole());
                    return ResponseEntity.ok(userData);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
