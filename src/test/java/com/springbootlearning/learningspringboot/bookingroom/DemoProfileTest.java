package com.springbootlearning.learningspringboot.bookingroom;

import com.springbootlearning.learningspringboot.bookingroom.repository.RoomRepository;
import com.springbootlearning.learningspringboot.bookingroom.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le profil demo est utilise pour la vitrine Render : il doit creer les comptes
 * publics et les salles sans exposer la configuration de production.
 */
@SpringBootTest
@ActiveProfiles({"demo", "test"})
class DemoProfileTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Test
    void demoProfileSeedsPublicAccountsAndRooms() {
        assertThat(userRepository.findByUsername("admin")).isPresent();
        assertThat(userRepository.findByUsername("user")).isPresent();
        assertThat(roomRepository.count()).isGreaterThanOrEqualTo(15);
    }
}
