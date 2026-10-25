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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
// Le profil dev active le seeding (DataInitializer), necessaire pour la salle SOLEIL.
@ActiveProfiles({"dev", "test"})
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
    void cancelBooking_shouldBeForbidden_whenNotOwnerNorAdmin() throws Exception {
        User owner = userService.register("Alice", "Owner", "alice_owner_1", "alice_owner_1@test.com", "Password@123");
        Booking booking = createBookingFor(owner);

        mockMvc.perform(post("/booking/cancel/" + booking.getId()).with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(bookingRepository.findById(booking.getId())).isPresent();
    }

    @Test
    @WithMockUser(username = "alice_owner_2", roles = "USER")
    void cancelBooking_shouldCancel_whenOwner() throws Exception {
        User owner = userService.register("Alice", "Owner", "alice_owner_2", "alice_owner_2@test.com", "Password@123");
        Booking booking = createBookingFor(owner);

        mockMvc.perform(post("/booking/cancel/" + booking.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Annulation : la ligne est supprimee.
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
    void bookingDetail_shouldReturn404_forNonexistentRoom() throws Exception {
        // Un id inconnu ne redirige plus silencieusement : il repond un vrai 404.
        mockMvc.perform(get("/booking/999999"))
                .andExpect(status().isNotFound());
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

    @Test
    @WithMockUser(username = "dave_validation", roles = "USER")
    void confirmBooking_shouldRejectShortTitle_withoutCreatingBooking() throws Exception {
        userService.register("Dave", "Validation", "dave_validation", "dave_validation@test.com", "Password@123");
        LocalDateTime start = LocalDateTime.now().plusDays(3);

        mockMvc.perform(post("/booking/confirm")
                        .with(csrf())
                        .param("roomId", existingRoom.getId().toString())
                        .param("title", "AB")
                        .param("startTime", start.toString())
                        .param("endTime", start.plusHours(1).toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/user/room-detail"))
                .andExpect(model().attributeHasFieldErrors("bookingForm", "title"));
    }

    @Test
    @WithMockUser(username = "erin_validation", roles = "USER")
    void confirmBooking_shouldRejectEndBeforeStart() throws Exception {
        userService.register("Erin", "Validation", "erin_validation", "erin_validation@test.com", "Password@123");
        LocalDateTime start = LocalDateTime.now().plusDays(3);

        mockMvc.perform(post("/booking/confirm")
                        .with(csrf())
                        .param("roomId", existingRoom.getId().toString())
                        .param("title", "Creneau invalide")
                        .param("startTime", start.toString())
                        .param("endTime", start.minusHours(1).toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/user/room-detail"))
                .andExpect(model().attributeHasFieldErrors("bookingForm", "endAfterStart"));
    }

    @Test
    @WithMockUser(username = "frank_validation", roles = "USER")
    void confirmBooking_shouldRejectPastDates_beforeReachingTheDatabase() throws Exception {
        userService.register("Frank", "Validation", "frank_validation", "frank_validation@test.com", "Password@123");
        LocalDateTime start = LocalDateTime.now().minusDays(1);

        mockMvc.perform(post("/booking/confirm")
                        .with(csrf())
                        .param("roomId", existingRoom.getId().toString())
                        .param("title", "Dans le passe")
                        .param("startTime", start.toString())
                        .param("endTime", start.plusHours(1).toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/user/room-detail"))
                .andExpect(model().attributeHasFieldErrors("bookingForm", "startTime", "endTime"));
    }

    @Test
    @WithMockUser(username = "grace_validation", roles = "USER")
    void confirmBooking_shouldRedirectWithFlashError_whenSlotIsTaken() throws Exception {
        User owner = userService.register("Grace", "Owner", "grace_owner", "grace_owner@test.com", "Password@123");
        userService.register("Grace", "Validation", "grace_validation", "grace_validation@test.com", "Password@123");
        LocalDateTime start = LocalDateTime.now().plusDays(4).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(1);
        bookingService.createBooking("Deja pris", start, end, owner, existingRoom);

        mockMvc.perform(post("/booking/confirm")
                        .with(csrf())
                        .param("roomId", existingRoom.getId().toString())
                        .param("title", "Conflit de creneau")
                        .param("startTime", start.toString())
                        .param("endTime", end.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/booking/" + existingRoom.getId()))
                .andExpect(flash().attribute("errorMessage", "La salle est déjà réservée pour cette plage horaire."));
    }
}
