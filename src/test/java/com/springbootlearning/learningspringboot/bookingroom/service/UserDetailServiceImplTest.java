package com.springbootlearning.learningspringboot.bookingroom.service;

import com.springbootlearning.learningspringboot.bookingroom.model.User;
import com.springbootlearning.learningspringboot.bookingroom.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailServiceImpl userDetailsService;

    private User userEntity;

    @BeforeEach
    void setUp() {
        userEntity = new User("Alice", "Doe", "alice", "alice@mail.com", "encodedPassword");
        userEntity.setId(1L);
    }

    @Test
    void loadUserByUsername_shouldReturnUserDetails_ifUserExists() {
        // GIVEN
        String username = "alice";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(userEntity));

        // WHEN
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // THEN
        // Vérification des infos de connexion
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("alice");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");


        assertThat(userDetails.getAuthorities()).hasSize(1);

        verify(userRepository).findByUsername(username);
    }

    @Test
    void loadUserByUsername_shouldThrowException_ifUserNotFound() {
        // GIVEN
        String unknownUsername = "ghost";

        when(userRepository.findByUsername(unknownUsername)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(unknownUsername))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found with username: " + unknownUsername);

        // Vérification que le repository a bien été consulté
        verify(userRepository).findByUsername(unknownUsername);
    }
}