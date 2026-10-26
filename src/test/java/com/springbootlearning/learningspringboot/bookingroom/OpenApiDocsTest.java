package com.springbootlearning.learningspringboot.bookingroom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "springdoc.api-docs.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsExposeTheRealEndpointsAndSessionAuth() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("BookingRoom API"))
                .andExpect(jsonPath("$.paths['/api/rooms'].get").exists())
                .andExpect(jsonPath("$.paths['/api/reservations'].post").exists())
                .andExpect(jsonPath("$.paths['/api/reservations/{id}'].delete").exists())
                .andExpect(jsonPath("$.paths['/admin']").doesNotExist())
                .andExpect(jsonPath("$.components.securitySchemes.sessionAuth.type").value("apiKey"))
                .andExpect(jsonPath("$.components.securitySchemes.sessionAuth.in").value("cookie"))
                .andExpect(jsonPath("$.components.securitySchemes.sessionAuth.name").value("JSESSIONID"));
    }
}
