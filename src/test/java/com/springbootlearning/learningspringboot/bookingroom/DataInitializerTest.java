package com.springbootlearning.learningspringboot.bookingroom;

import com.springbootlearning.learningspringboot.bookingroom.model.Role;
import com.springbootlearning.learningspringboot.bookingroom.model.User;
import com.springbootlearning.learningspringboot.bookingroom.repository.RoomRepository;
import com.springbootlearning.learningspringboot.bookingroom.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:devdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
// `test` en dernier : les profils sont resolus de gauche a droite (le dernier gagne),
// donc la datasource H2 du profil test prime sur celle du profil dev.
@ActiveProfiles({"dev", "test"})
class DataInitializerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void devProfileSeedsDemoAccountsAndRooms() {
        User admin = userRepository.findByUsername("admin").orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(passwordEncoder.matches("Admin@123", admin.getPassword())).isTrue();

        User user = userRepository.findByUsername("user").orElseThrow();
        assertThat(user.getRole()).isEqualTo(Role.USER);

        assertThat(roomRepository.count()).isEqualTo(15);
    }
}
