package com.springbootlearning.learningspringboot.bookingroom.service;
import com.springbootlearning.learningspringboot.bookingroom.model.User;
import com.springbootlearning.learningspringboot.bookingroom.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void register_shouldSaveUser_ifUsernameNotExists() {
        // GIVEN
        String username = "alice";
        String email = "alice@test.com";

        // On simule que l'utilisateur n'existe pas encore
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("encodedPassword");

        // On simule le comportement de la BDD : quand on sauve, ça renvoie l'objet (avec son ID généré par exemple)
        // .thenAnswer(i -> i.getArgument(0)) signifie "renvoie l'objet qu'on vient de me donner"
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        User result = userService.register("Alice", "Doe", username, email, "Password@123");

        // THEN
        // 1. Vérification du résultat
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getPassword()).isEqualTo("encodedPassword");

        // 2. Vérification des interactions avec la BDD
        verify(userRepository).existsByUsername(username);
        verify(userRepository).existsByEmail(email);
        verify(passwordEncoder).encode("Password@123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrowException_ifUsernameAlreadyExists() {
        // GIVEN
        String username = "bob";

        // On simule que l'utilisateur existe déjà dans la base
        when(userRepository.existsByUsername(username)).thenReturn(true);

        // WHEN & THEN
        assertThatThrownBy(() -> userService.register("Bob", "Smith", username, "bob@test.com", "Password@123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ce nom d'utilisateur est deja utilise");

        // 2. Vérification que la BDD a bien été interrogée pour vérifier l'existence
        verify(userRepository).existsByUsername(username);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_shouldThrowException_ifEmailAlreadyExists() {
        // GIVEN
        String username = "alice";
        String email = "alice@test.com";
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // WHEN & THEN
        assertThatThrownBy(() -> userService.register("Alice", "Doe", username, email, "Password@123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cette adresse email est deja utilisee");

        verify(userRepository).existsByUsername(username);
        verify(userRepository).existsByEmail(email);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void existByUsername_shouldReturnTrue_ifUserExists() {
        // GIVEN
        String username = "admin";
        when(userRepository.existsByUsername(username)).thenReturn(true);

        // WHEN
        Boolean result = userService.existByUsername(username);

        // THEN
        assertThat(result).isTrue();
        verify(userRepository).existsByUsername(username);
    }

    @Test
    void existByEmail_shouldReturnTrue_ifEmailExists() {
        // GIVEN
        String email = "admin@test.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // WHEN
        Boolean result = userService.existByEmail(email);

        // THEN
        assertThat(result).isTrue();
        verify(userRepository).existsByEmail(email);
    }

    @Test
    void existByUsername_shouldReturnFalse_ifUserNotExists() {
        // GIVEN
        String username = "ghost";
        when(userRepository.existsByUsername(username)).thenReturn(false);

        // WHEN
        Boolean result = userService.existByUsername(username);

        // THEN
        assertThat(result).isFalse();
        verify(userRepository).existsByUsername(username);
    }
}
