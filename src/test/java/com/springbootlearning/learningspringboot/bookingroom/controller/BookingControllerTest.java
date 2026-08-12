package com.springbootlearning.learningspringboot.bookingroom.controller;

import com.springbootlearning.learningspringboot.bookingroom.model.Booking;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import com.springbootlearning.learningspringboot.bookingroom.model.User;
import com.springbootlearning.learningspringboot.bookingroom.repository.BookingRepository;
import com.springbootlearning.learningspringboot.bookingroom.repository.RoomRepository;
import com.springbootlearning.learningspringboot.bookingroom.service.BookingService;
import com.springbootlearning.learningspringboot.bookingroom.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    private Room existingRoom;

    @BeforeEach
    void setUp() {
        // Seedee par DataInitializer au demarrage de l'app.
        existingRoom = roomRepository.findByNameIgnoreCase("SOLEIL").orElseThrow();
    }

    private Booking createBookingFor(User owner) {
        return bookingService.createBooking(
                "Reunion de test",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1),
                owner,
                existingRoom
        );
    }

    @Test
    @WithMockUser(username = "mallory", roles = "USER")
    void cancelBooking_shouldNotCancel_whenNotOwnerNorAdmin() throws Exception {
        User owner = userService.register("Alice", "Owner", "alice_owner_1", "alice_owner_1@test.com", "Password@123");
        Booking booking = createBookingFor(owner);

        mockMvc.perform(post("/booking/cancel/" + booking.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(bookingRepository.findById(booking.getId())).isPresent();
    }

    @Test
    @WithMockUser(username = "alice_owner_2", roles = "USER")
    void cancelBooking_shouldCancel_whenOwner() throws Exception {
        User owner = userService.register("Alice", "Owner", "alice_owner_2", "alice_owner_2@test.com", "Password@123");
        Booking booking = createBookingFor(owner);

        mockMvc.perform(post("/booking/cancel/" + booking.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(bookingRepository.findById(booking.getId())).isEmpty();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cancelBooking_shouldCancel_whenAdmin() throws Exception {
        // "admin" est le compte seede par DataInitializer (role ADMIN reel en base) :
        // le controleur revalide le role depuis la BDD, pas seulement depuis le contexte de securite mocke.
        User owner = userService.register("Bob", "Owner", "bob_owner_1", "bob_owner_1@test.com", "Password@123");
        Booking booking = createBookingFor(owner);

        mockMvc.perform(post("/booking/cancel/" + booking.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(bookingRepository.findById(booking.getId())).isEmpty();
    }

    @Test
    @WithMockUser(roles = "USER")
    void bookingDetail_shouldReturn200_forExistingRoom() throws Exception {
        mockMvc.perform(get("/booking/" + existingRoom.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void bookingDetail_shouldRedirect_forNonexistentRoom() throws Exception {
        // Avant le fix : un id inconnu faisait planter le rendu du template avec une
        // SpelEvaluationException (room.available sur un attribut jamais peuple).
        mockMvc.perform(get("/booking/999999"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "carol", roles = "USER")
    void confirmBooking_shouldCreateBooking_forAuthenticatedUser() throws Exception {
        userService.register("Carol", "Test", "carol", "carol@test.com", "Password@123");
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end = start.plusHours(1);

        mockMvc.perform(post("/booking/confirm")
                        .with(csrf())
                        .param("roomId", existingRoom.getId().toString())
                        .param("title", "Point d'equipe")
                        .param("startTime", start.toString())
                        .param("endTime", end.toString()))
                .andExpect(status().is3xxRedirection());

        assertThat(bookingRepository.findByRoom(existingRoom))
                .anyMatch(b -> b.getUser().getUsername().equals("carol") && b.getTitle().equals("Point d'equipe"));
    }
}
