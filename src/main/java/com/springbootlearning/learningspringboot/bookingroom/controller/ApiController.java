package com.springbootlearning.learningspringboot.bookingroom.controller;

import com.springbootlearning.learningspringboot.bookingroom.dto.ApiErrorResponse;
import com.springbootlearning.learningspringboot.bookingroom.dto.BookingCreatedResponse;
import com.springbootlearning.learningspringboot.bookingroom.dto.BookingFormDto;
import com.springbootlearning.learningspringboot.bookingroom.dto.BookingResponse;
import com.springbootlearning.learningspringboot.bookingroom.dto.CsrfResponse;
import com.springbootlearning.learningspringboot.bookingroom.dto.LoginRequest;
import com.springbootlearning.learningspringboot.bookingroom.dto.LoginResponse;
import com.springbootlearning.learningspringboot.bookingroom.dto.RoomDetailResponse;
import com.springbootlearning.learningspringboot.bookingroom.dto.RoomResponse;
import com.springbootlearning.learningspringboot.bookingroom.dto.RoomSearchDto;
import com.springbootlearning.learningspringboot.bookingroom.dto.StatsResponse;
import com.springbootlearning.learningspringboot.bookingroom.dto.UserResponse;
import com.springbootlearning.learningspringboot.bookingroom.model.Booking;
import com.springbootlearning.learningspringboot.bookingroom.model.BookingStatus;
import com.springbootlearning.learningspringboot.bookingroom.model.Role;
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@Validated
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
    public ResponseEntity<CsrfResponse> csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return ResponseEntity.ok(new CsrfResponse(token.getToken()));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        if (body == null || body.username() == null || body.username().isBlank()
                || body.password() == null || body.password().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiErrorResponse("Nom d'utilisateur et mot de passe requis"));
        }

        if (loginAttemptService.isBlocked(body.username())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ApiErrorResponse("Trop de tentatives échouées. Réessayez plus tard."));
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

            return ResponseEntity.ok(new LoginResponse(authentication.getName()));
        } catch (AuthenticationException ex) {
            loginAttemptService.recordFailure(body.username());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiErrorResponse("Identifiants invalides"));
        }
    }

    @GetMapping("/rooms")
    public ResponseEntity<Page<RoomResponse>> getRooms(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(defaultValue = "false") boolean projector,
            @RequestParam(defaultValue = "false") boolean whiteboard,
            @RequestParam(defaultValue = "false") boolean videoconferencing,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        RoomSearchDto criteria = new RoomSearchDto(name, capacity, projector, whiteboard, videoconferencing);
        Page<RoomResponse> rooms = roomService.searchRooms(criteria, PageRequest.of(page, size))
                .map(RoomResponse::from);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/rooms/{id}")
    public ResponseEntity<RoomDetailResponse> getRoomById(@PathVariable Long id) {
        return roomService.getRoomById(id)
                .map(room -> ResponseEntity.ok(new RoomDetailResponse(
                        RoomResponse.from(room),
                        List.of())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping({"/reservations/me", "/my-bookings"})
    public ResponseEntity<Page<BookingResponse>> getMyBookings(
            Principal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Page<BookingResponse> bookings = bookingService
                .getBookingsByUser(principal.getName(), PageRequest.of(page, size))
                .map(BookingResponse::from);

        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/reservations/{id}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable Long id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<Booking> booking = bookingService.getBookingById(id);
        if (booking.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!canAccess(booking.get(), principal)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(BookingResponse.from(booking.get()));
    }

    @PostMapping("/reservations")
    public ResponseEntity<BookingCreatedResponse> createBooking(
            @Valid @RequestBody BookingFormDto request,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        Room room = roomService.getRoomById(request.roomId())
                .orElseThrow(() -> new RuntimeException("Salle non trouvée"));

        Booking booking = bookingService.createBooking(
                request.title(), request.startTime(), request.endTime(), user, room);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(booking.getId())
                .toUri();

        return ResponseEntity.created(location)
                .body(new BookingCreatedResponse(booking.getId(), "Réservation créée avec succès"));
    }

    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<Booking> booking = bookingService.getBookingById(id);
        if (booking.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!canAccess(booking.get(), principal)) {
            return ResponseEntity.status(403).build();
        }

        bookingService.cancelBooking(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StatsResponse> getOverviewStats() {
        return ResponseEntity.ok(new StatsResponse(
                roomRepository.count(),
                bookingRepository.count(),
                userRepository.count(),
                roomRepository.countByAvailableTrue()));
    }

    @GetMapping("/rooms/available-now")
    public ResponseEntity<List<RoomResponse>> getRoomsAvailableNow() {
        // Une seule requete NOT EXISTS : le nombre de requetes ne depend plus du nombre de salles.
        LocalDateTime now = LocalDateTime.now();
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

    private boolean canAccess(Booking booking, Principal principal) {
        return booking.getUser().getUsername().equals(principal.getName()) || isAdmin(principal);
    }

    private boolean isAdmin(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .map(user -> user.getRole() == Role.ADMIN)
                .orElse(false);
    }
}
