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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
                .andExpect(jsonPath("$[0].roomName").value("Salle API"))
                .andExpect(jsonPath("$[0].room").doesNotExist())
                .andExpect(jsonPath("$[0].user").doesNotExist());
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
                .andExpect(jsonPath("$.totalBookings").isNumber());
    }

    @Test
    void dashboardShouldHidePastBookings() throws Exception {
        User user = userRepository.findByUsername("apiuser").orElseThrow();
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
