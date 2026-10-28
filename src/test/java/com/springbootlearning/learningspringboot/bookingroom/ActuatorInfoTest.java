package com.springbootlearning.learningspringboot.bookingroom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrat utilise par le job de deploiement : /actuator/info doit etre public
 * et renvoyer le commit injecte par la plateforme (RENDER_GIT_COMMIT).
 */
@SpringBootTest(properties = {
        "management.endpoints.web.exposure.include=health,info",
        "management.info.env.enabled=true",
        "info.app.commit=test-sha"
})
@AutoConfigureMockMvc
class ActuatorInfoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void infoEndpointExposesDeployedCommitWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.commit").value("test-sha"));
    }
}
