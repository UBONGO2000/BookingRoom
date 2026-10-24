package com.springbootlearning.learningspringboot.bookingroom.controller;

import com.springbootlearning.learningspringboot.bookingroom.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoomRepository roomRepository;

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
}
