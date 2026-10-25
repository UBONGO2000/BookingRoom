package com.springbootlearning.learningspringboot.bookingroom.controller;

import com.springbootlearning.learningspringboot.bookingroom.repository.UserRepository;
import com.springbootlearning.learningspringboot.bookingroom.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void register_shouldRedirectToLogin_whenFormIsValid() throws Exception {
        mockMvc.perform(registerRequest("alice_p19", "alice_p19@test.com", "Password@123", "Password@123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        assertThat(userRepository.findByUsername("alice_p19")).isPresent();
    }

    @Test
    void register_shouldShowError_whenPasswordsDiffer() throws Exception {
        mockMvc.perform(registerRequest("alice_p19", "alice_p19@test.com", "Password@123", "Password@124"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeHasFieldErrors("user", "passwordConfirmed"))
                .andExpect(content().string(containsString("Les mots de passe ne correspondent pas")))
                .andExpect(content().string(containsString("href=\"/css/auth.css\"")))
                .andExpect(content().string(containsString("/js/theme-init.js")))
                .andExpect(content().string(containsString("class=\"skip-link\"")))
                .andExpect(content().string(containsString("id=\"main-content\"")));

        assertThat(userRepository.findByUsername("alice_p19")).isEmpty();
    }

    @Test
    void register_shouldShowError_whenConfirmPasswordIsMissing() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("firstname", "Alice")
                        .param("lastname", "Martin")
                        .param("username", "alice_p19")
                        .param("email", "alice_p19@test.com")
                        .param("password", "Password@123"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeHasFieldErrors("user", "confirmPassword"));

        assertThat(userRepository.findByUsername("alice_p19")).isEmpty();
    }

    @Test
    void register_shouldShowError_whenUsernameAlreadyExists() throws Exception {
        userService.register("Bob", "Martin", "bob_p19", "bob_p19@test.com", "Password@123");

        mockMvc.perform(registerRequest("bob_p19", "other_p19@test.com", "Password@123", "Password@123"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeHasFieldErrors("user", "username"));

        assertThat(userRepository.findByUsername("bob_p19")).isPresent();
        assertThat(userRepository.findByEmail("other_p19@test.com")).isEmpty();
    }

    @Test
    void register_shouldShowError_whenEmailAlreadyExists() throws Exception {
        userService.register("Bob", "Martin", "bob_p19", "bob_p19@test.com", "Password@123");

        mockMvc.perform(registerRequest("other_p19", "bob_p19@test.com", "Password@123", "Password@123"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeHasFieldErrors("user", "email"));

        assertThat(userRepository.findByUsername("other_p19")).isEmpty();
    }

    private MockHttpServletRequestBuilder registerRequest(String username,
                                                          String email,
                                                          String password,
                                                          String confirmPassword) {
        return post("/register").with(csrf())
                .param("firstname", "Alice")
                .param("lastname", "Martin")
                .param("username", username)
                .param("email", email)
                .param("password", password)
                .param("confirmPassword", confirmPassword);
    }
}
