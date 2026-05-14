package com.springbootlearning.learningspringboot.bookingroom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void defaultAdminCanLoginWithFormLogin() throws Exception {
        mockMvc.perform(formLogin("/login").user("admin").password("Admin@123"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(authenticated().withUsername("admin"));
    }

    @Test
    void defaultUserCanLoginWithFormLogin() throws Exception {
        mockMvc.perform(formLogin("/login").user("user").password("User@123"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(authenticated().withUsername("user"));
    }
}
