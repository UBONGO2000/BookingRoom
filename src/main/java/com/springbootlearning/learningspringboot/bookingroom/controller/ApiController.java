package com.springbootlearning.learningspringboot.bookingroom.controller;

import com.springbootlearning.learningspringboot.bookingroom.dto.BookingResponse;
import com.springbootlearning.learningspringboot.bookingroom.dto.LoginRequest;
import com.springbootlearning.learningspringboot.bookingroom.dto.RoomResponse;
import com.springbootlearning.learningspringboot.bookingroom.dto.RoomSearchDto;
import com.springbootlearning.learningspringboot.bookingroom.dto.UserResponse;
import com.springbootlearning.learningspringboot.bookingroom.exception.BookingConflictException;
import com.springbootlearning.learningspringboot.bookingroom.model.Booking;
import com.springbootlearning.learningspringboot.bookingroom.model.BookingStatus;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import com.springbootlearning.learningspringboot.bookingroom.model.User;
import com.springbootlearning.learningspringboot.bookingroom.repository.BookingRepository;
import com.springbootlearning.learningspringboot.bookingroom.repository.RoomRepository;
import com.springbootlearning.learningspringboot.bookingroom.repository.UserRepository;
import com.springbootlearning.learningspringboot.bookingroom.security.LoginAttemptService;
import com.springbootlearning.learningspringboot.bookingroom.service.BookingService;
import com.springbootlearning.learningspringboot.bookingroom.service.RoomService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BookingService bookingService;
    private final RoomService roomService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final LoginAttemptService loginAttemptService;

    public ApiController(RoomRepository roomRepository,
                         BookingRepository bookingRepository,
                         UserRepository userRepository,
                         BookingService bookingService,
                         RoomService roomService,
                         AuthenticationManager authenticationManager,
                         SecurityContextRepository securityContextRepository,
                         LoginAttemptService loginAttemptService) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.bookingService = bookingService;
        this.roomService = roomService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.loginAttemptService = loginAttemptService;
    }

    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return ResponseEntity.ok(Map.of("token", token.getToken()));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest body,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response) {
        if (body == null || body.username() == null || body.username().isBlank()
                || body.password() == null || body.password().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Nom d'utilisateur et mot de passe requis"));
        }

        if (loginAttemptService.isBlocked(body.username())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Trop de tentatives échouées. Réessayez plus tard."));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(body.username(), body.password())
            );

            loginAttemptService.recordSuccess(body.username());

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            securityContextRepository.saveContext(context, request, response);
            request.changeSessionId();

            return ResponseEntity.ok(Map.of("username", authentication.getName()));
        } catch (AuthenticationException ex) {
            loginAttemptService.recordFailure(body.username());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Identifiants invalides"));
        }
    }

    @GetMapping("/rooms")
    public ResponseEntity<Page<RoomResponse>> getRooms(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(defaultValue = "false") boolean projector,
            @RequestParam(defaultValue = "false") boolean whiteboard,
            @RequestParam(defaultValue = "false") boolean videoconferencing,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        RoomSearchDto criteria = new RoomSearchDto(name, capacity, projector, whiteboard, videoconferencing);
        Page<RoomResponse> rooms = roomService.searchRooms(criteria, PageRequest.of(page, size))
                .map(RoomResponse::from);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/rooms/{id}")
    public ResponseEntity<Map<String, Object>> getRoomById(@PathVariable Long id) {
        return roomService.getRoomById(id)
                .map(room -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("room", RoomResponse.from(room));
                    // Le système d'évaluations n'est pas encore implémenté dans le modèle de données,
                    // donc nous retournons une liste vide pour l'instant comme placeholder.
                    response.put("evaluations", List.of()); 
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping({"/reservations/me", "/my-bookings"})
    public ResponseEntity<List<BookingResponse>> getMyBookings(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        
        List<BookingResponse> bookings = bookingService.getBookingsByUser(principal.getName())
                .stream()
                .map(BookingResponse::from)
                .toList();
        
        return ResponseEntity.ok(bookings);
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
        } catch (BookingConflictException e) {
            // Conflit de creneau : 409 rendu par l'ApiExceptionHandler.
            throw e;
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/stats/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getOverviewStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRooms", roomRepository.count());
        stats.put("totalBookings", bookingRepository.count());
        stats.put("totalUsers", userRepository.count());
        // Un COUNT en base : plus besoin de charger toutes les salles.
        stats.put("availableRooms", roomRepository.countByAvailableTrue());

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/rooms/available-now")
    public ResponseEntity<List<RoomResponse>> getRoomsAvailableNow() {
        // Une seule requete NOT EXISTS : le nombre de requetes ne depend plus du nombre de salles.
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        List<RoomResponse> availableRooms = roomRepository
                .findAvailableBetween(now, now.plusHours(1), BookingStatus.CONFIRMED)
                .stream()
                .map(RoomResponse::from)
                .toList();
        return ResponseEntity.ok(availableRooms);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        
        return userRepository.findByUsername(principal.getName())
                .map(user -> ResponseEntity.ok(UserResponse.from(user)))
                .orElse(ResponseEntity.notFound().build());
    }
}
