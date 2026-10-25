package com.springbootlearning.learningspringboot.bookingroom;

import com.springbootlearning.learningspringboot.bookingroom.model.Booking;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import com.springbootlearning.learningspringboot.bookingroom.model.User;
import com.springbootlearning.learningspringboot.bookingroom.repository.BookingRepository;
import com.springbootlearning.learningspringboot.bookingroom.repository.RoomRepository;
import com.springbootlearning.learningspringboot.bookingroom.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "apiuser", roles = "USER")
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUpData() {
        User user = userRepository.findByUsername("apiuser")
                .orElseGet(() -> userRepository.save(new User(
                        "Api",
                        "User",
                        "apiuser",
                        "apiuser@example.com",
                        passwordEncoder.encode("Api@1234")
                )));

        Room room = roomRepository.findByNameIgnoreCase("Salle API")
                .orElseGet(() -> roomRepository.save(new Room(
                        "Salle API",
                        "Salle de test API",
                        10,
                        "Batiment Test",
                        true,
                        true,
                        true
                )));

        if (bookingRepository.count() == 0) {
            bookingRepository.save(new Booking(
                    "Reunion API",
                    LocalDateTime.now().plusDays(1),
                    LocalDateTime.now().plusDays(1).plusHours(1),
                    user,
                    room
            ));
        }
    }

    @Test
    void roomsEndpointReturnsDtoWithoutLazyAssociations() throws Exception {
        mockMvc.perform(get("/api/rooms").param("name", "Salle API"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Salle API"))
                .andExpect(jsonPath("$.content[0].bookings").doesNotExist())
                .andExpect(jsonPath("$.content[0].available").value(true));
    }

    @Test
    void roomDetailReturnsDto() throws Exception {
        Long roomId = roomRepository.findByNameIgnoreCase("Salle API").orElseThrow().getId();

        mockMvc.perform(get("/api/rooms/{id}", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.room.name").value("Salle API"))
                .andExpect(jsonPath("$.room.bookings").doesNotExist());
    }

    @Test
    void currentUserDoesNotExposePassword() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("apiuser"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void myBookingsReturnsDtoWithRoomName() throws Exception {
        mockMvc.perform(get("/api/reservations/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].roomName").value("Salle API"))
                .andExpect(jsonPath("$.content[0].room").doesNotExist())
                .andExpect(jsonPath("$.content[0].user").doesNotExist())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void myBookingsRejectInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/reservations/me").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void userDashboardRendersWithOpenSessionInViewDisabled() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Salle API")));
    }

    @Test
    @WithMockUser(username = "apiuser", roles = "ADMIN")
    void adminDashboardRendersWithOpenSessionInViewDisabled() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Salle API")));
    }

    @Test
    @WithMockUser(username = "apiuser", roles = "USER")
    void statsOverviewShouldBeForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/stats/overview"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Accès refusé"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void statsOverviewShouldSucceedForAdmin() throws Exception {
        mockMvc.perform(get("/api/stats/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRooms").isNumber())
                .andExpect(jsonPath("$.totalBookings").isNumber())
                .andExpect(jsonPath("$.availableRooms").isNumber());
    }

    @Test
    void availableNowShouldExcludeBusyAndUnavailableRooms() throws Exception {
        User user = userRepository.findByUsername("apiuser").orElseThrow();
        Room busy = roomRepository.save(new Room(
                "Salle Occupee API", "desc", 10, "Batiment Test", true, true, true));
        Room free = roomRepository.save(new Room(
                "Salle Libre API", "desc", 10, "Batiment Test", true, true, true));
        Room closed = new Room("Salle Fermee API", "desc", 10, "Batiment Test", true, true, true);
        closed.setAvailable(false);
        closed = roomRepository.save(closed);

        LocalDateTime now = LocalDateTime.now();
        Booking current = bookingRepository.save(new Booking(
                "Reunion en cours", now.minusMinutes(10), now.plusMinutes(50), user, busy));

        try {
            mockMvc.perform(get("/api/rooms/available-now"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.name == 'Salle Libre API')]").isNotEmpty())
                    .andExpect(jsonPath("$[?(@.name == 'Salle Occupee API')]").isEmpty())
                    .andExpect(jsonPath("$[?(@.name == 'Salle Fermee API')]").isEmpty());
        } finally {
            bookingRepository.delete(current);
            roomRepository.deleteAll(List.of(busy, free, closed));
        }
    }

    @Test
    void bookingConflictReturns409() throws Exception {
        Room room = roomRepository.findByNameIgnoreCase("Salle API").orElseThrow();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        String payload = """
                {"title":"Conflit","startTime":"%s","endTime":"%s","roomId":%d}
                """.formatted(start, start.plusHours(1), room.getId());

        mockMvc.perform(post("/api/reservations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("La salle est déjà réservée pour cette plage horaire."));
    }

    @Test
    void bookingValidationReturns400() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void createBookingReturns201WithLocationAndCanBeReadAndCancelled() throws Exception {
        Long bookingId = null;
        try {
            Room room = roomRepository.findByNameIgnoreCase("Salle API").orElseThrow();
            LocalDateTime start = LocalDateTime.now().plusDays(3);
            String payload = """
                    {"title":"Nouvelle reunion","startTime":"%s","endTime":"%s","roomId":%d}
                    """.formatted(start, start.plusHours(1), room.getId());

            MvcResult created = mockMvc.perform(post("/api/reservations")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.message").value("Réservation créée avec succès"))
                    .andReturn();

            String location = created.getResponse().getHeader("Location");
            bookingId = Long.valueOf(location.substring(location.lastIndexOf('/') + 1));

            mockMvc.perform(get("/api/reservations/{id}", bookingId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(bookingId))
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));

            mockMvc.perform(delete("/api/reservations/{id}", bookingId).with(csrf()))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/reservations/{id}", bookingId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        } finally {
            if (bookingId != null) {
                bookingRepository.deleteById(bookingId);
            }
        }
    }

    @Test
    @WithMockUser(username = "other", roles = "USER")
    void bookingDetailAndCancellationShouldBeForbiddenForAnotherUser() throws Exception {
        User owner = userRepository.findByUsername("apiuser").orElseThrow();
        Room room = roomRepository.findByNameIgnoreCase("Salle API").orElseThrow();
        LocalDateTime start = LocalDateTime.now().plusDays(4);
        Booking booking = bookingRepository.save(new Booking(
                "Reservation privee", start, start.plusHours(1), owner, room));

        try {
            mockMvc.perform(get("/api/reservations/{id}", booking.getId()))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete("/api/reservations/{id}", booking.getId()).with(csrf()))
                    .andExpect(status().isForbidden());
        } finally {
            bookingRepository.delete(booking);
        }
    }

    @Test
    void dashboardShouldHidePastBookings() throws Exception {        User user = userRepository.findByUsername("apiuser").orElseThrow();
        Room room = roomRepository.findByNameIgnoreCase("Salle API").orElseThrow();
        LocalDateTime pastStart = LocalDateTime.now().minusDays(2);
        Booking pastBooking = bookingRepository.save(new Booking(
                "Ancienne reunion", pastStart, pastStart.plusHours(1), user, room));

        try {
            mockMvc.perform(get("/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Reunion API")))
                    .andExpect(content().string(not(containsString("Ancienne reunion"))));
        } finally {
            bookingRepository.delete(pastBooking);
        }
    }
}
