package com.springbootlearning.learningspringboot.bookingroom.controller;

import com.springbootlearning.learningspringboot.bookingroom.model.Booking;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import com.springbootlearning.learningspringboot.bookingroom.model.User;
import com.springbootlearning.learningspringboot.bookingroom.repository.BookingRepository;
import com.springbootlearning.learningspringboot.bookingroom.repository.RoomRepository;
import com.springbootlearning.learningspringboot.bookingroom.service.UserService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserService userService;

    @Test
    void adminDashboard_shouldRedirectToLogin_whenAnonymous() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void adminDashboard_shouldBeForbidden_forNonAdmin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminDashboard_shouldSucceed_forAdmin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void saveRoom_shouldBeForbidden_forNonAdmin() throws Exception {
        mockMvc.perform(post("/admin/rooms/save")
                        .with(csrf())
                        .param("name", "SHOULD_NOT_EXIST")
                        .param("location", "Batiment Z")
                        .param("capacity", "10"))
                .andExpect(status().isForbidden());

        assertThat(roomRepository.existsByName("SHOULD_NOT_EXIST")).isFalse();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void saveRoom_shouldCreateRoom_forAdmin() throws Exception {
        // Reproduit exactement ce qu'envoie le vrai formulaire : les checkboxes
        // Thymeleaf (th:field) ajoutent toujours un champ cache _xxx, donc projector/
        // whiteboard/videoconferencing/available sont toujours presents, meme decoches.
        mockMvc.perform(post("/admin/rooms/save")
                        .with(csrf())
                        .param("name", "TEST_ROOM_ADMIN")
                        .param("location", "Batiment Z")
                        .param("capacity", "10")
                        .param("projector", "true")
                        .param("whiteboard", "true")
                        .param("videoconferencing", "false")
                        .param("available", "true"))
                .andExpect(status().is3xxRedirection());

        assertThat(roomRepository.existsByName("TEST_ROOM_ADMIN")).isTrue();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteRoom_shouldBeForbidden_viaGet() throws Exception {
        // La suppression doit passer par POST (protection deja corrigee) :
        // un GET ne doit pas etre route vers l'action de suppression.
        mockMvc.perform(get("/admin/rooms/delete/1"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void saveRoom_shouldShowErrors_whenFormIsInvalid() throws Exception {
        long before = roomRepository.count();

        mockMvc.perform(post("/admin/rooms/save")
                        .with(csrf())
                        .param("name", "")
                        .param("location", "")
                        .param("capacity", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/admin/room-form"))
                .andExpect(model().attributeHasFieldErrors("room", "name", "location", "capacity"));

        assertThat(roomRepository.count()).isEqualTo(before);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void saveRoom_shouldRejectDuplicateName() throws Exception {
        roomRepository.save(new Room("Salle Dupliquee", "desc", 10, "Batiment Z", false, false, false));

        mockMvc.perform(post("/admin/rooms/save")
                        .with(csrf())
                        .param("name", "Salle Dupliquee")
                        .param("location", "Batiment Y")
                        .param("capacity", "20")
                        .param("projector", "false")
                        .param("whiteboard", "false")
                        .param("videoconferencing", "false")
                        .param("available", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/admin/room-form"))
                .andExpect(model().attributeHasFieldErrors("room", "name"));

        assertThat(roomRepository.findAll().stream()
                .filter(r -> "Salle Dupliquee".equals(r.getName()))
                .count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void editRoomForm_shouldReturn404_forUnknownRoom() throws Exception {
        mockMvc.perform(get("/admin/rooms/edit/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteRoom_shouldBeRefused_whenFutureBookingExists() throws Exception {
        Room room = roomRepository.save(new Room(
                "Salle Reservee", "desc", 10, "Batiment Z", false, false, false));
        User owner = userService.register(
                "Delete", "Test", "delete_owner", "delete_owner@test.com", "Password@123");
        bookingRepository.save(new Booking(
                "Reservation future",
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(2).plusHours(1),
                owner,
                room));

        mockMvc.perform(post("/admin/rooms/delete/" + room.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("errorMessage",
                        "Impossible de supprimer cette salle : des réservations à venir existent."));

        assertThat(roomRepository.existsById(room.getId())).isTrue();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteRoom_shouldDeleteRoom_whenNoBookings() throws Exception {
        Room room = roomRepository.save(new Room(
                "Salle Vide", "desc", 10, "Batiment Z", false, false, false));

        mockMvc.perform(post("/admin/rooms/delete/" + room.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));

        assertThat(roomRepository.existsById(room.getId())).isFalse();
    }
}
