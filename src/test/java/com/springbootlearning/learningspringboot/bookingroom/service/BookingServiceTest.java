package com.springbootlearning.learningspringboot.bookingroom.service;

import com.springbootlearning.learningspringboot.bookingroom.model.Booking;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import com.springbootlearning.learningspringboot.bookingroom.model.User;
import com.springbootlearning.learningspringboot.bookingroom.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingService bookingService;

    // Données de test réutilisables
    private User user;
    private Room room;
    private Booking booking;
    private LocalDateTime futureStartTime;
    private LocalDateTime futureEndTime;

    @BeforeEach
    void setUp() {
        user = new User("aline","ATANGANA","alice","alice@gmail.com","alice123");
        user.setId(1L);

        room = new Room("Salle A","bla bla bla",40,"2 rue de la souveraineté",true,false,false);
        room.setId(10L);
        room.setAvailable(true);

        // Dates dans le futur pour les tests valides
        futureStartTime = LocalDateTime.now().plusDays(1);
        futureEndTime = futureStartTime.plusHours(1);

        booking = new Booking("Réunion", futureStartTime, futureEndTime, user, room);
        booking.setId(100L);
    }

    // ==========================================
    // TEST : getAllBookings
    // ==========================================
    @Test
    void getAllBookings_shouldReturnList() {
        // Given
        List<Booking> expectedList = Arrays.asList(booking, new Booking());
        when(bookingRepository.findAll()).thenReturn(expectedList);

        // When
        List<Booking> result = bookingService.getAllBookings();

        // Then
        assertThat(result).hasSize(2);
        verify(bookingRepository).findAll();
    }

    // ==========================================
    // TEST : getBookingsByUser
    // ==========================================
    @Test
    void getBookingsByUser_shouldReturnFilteredList() {
        // Given
        String username = "alice";
        when(bookingRepository.findByUserUsername(username)).thenReturn(Collections.singletonList(booking));

        // When
        List<Booking> result = bookingService.getBookingsByUser(username);

        // Then
        assertThat(result).containsExactly(booking);
        verify(bookingRepository).findByUserUsername(username);
    }

    // ==========================================
    // TEST : getBookingsByRoom
    // ==========================================
    @Test
    void getBookingsByRoom_shouldCallRepositoryWithCurrentTime() {
        // Given
        when(bookingRepository.findByRoomAndEndTimeAfterOrderByStartTimeAsc(any(Room.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(booking));

        // When
        List<Booking> result = bookingService.getBookingsByRoom(room);

        // Then
        assertThat(result).hasSize(1);
        verify(bookingRepository).findByRoomAndEndTimeAfterOrderByStartTimeAsc(eq(room), any(LocalDateTime.class));
    }

    // ==========================================
    // TEST : searchBookings
    // ==========================================
    @Test
    void searchBookings_shouldFilterByRoomName() {
        // Given
        Booking booking1 = new Booking("T1", LocalDateTime.now(), LocalDateTime.now().plusHours(1), user, room);
        Room otherRoom = new Room("Salle Conf","bla bla bla",60,"1 rue de la liberté",true,false,false);
        Booking booking2 = new Booking("T2", LocalDateTime.now(), LocalDateTime.now().plusHours(1), user, otherRoom);

        List<Booking> allBookings = Arrays.asList(booking1, booking2);
        when(bookingRepository.findAll()).thenReturn(allBookings);

        // When (Recherche insensible à la casse)
        List<Booking> result = bookingService.searchBookings("salle conf", null);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRoom().getName()).isEqualTo("Salle Conf");
    }

    @Test
    void searchBookings_shouldFilterByUsername() {
        // Given
        User bob = new User("batelemie","OWONA","bob","bob@gmail.com","bob123");
        Booking bookingBob = new Booking("T1", LocalDateTime.now(), LocalDateTime.now().plusHours(1), bob, room);

        when(bookingRepository.findAll()).thenReturn(Collections.singletonList(bookingBob));

        // When
        List<Booking> result = bookingService.searchBookings(null, "BOB");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser().getUsername()).isEqualTo("bob");
    }

    @Test
    void searchBookings_withNullParams_shouldReturnAll() {
        // Given
        when(bookingRepository.findAll()).thenReturn(Collections.singletonList(booking));

        // When
        List<Booking> result = bookingService.searchBookings(null, null);

        // Then
        assertThat(result).hasSize(1);
    }

    // ==========================================
    // TEST : isRoomAvailable
    // ==========================================
    @Test
    void isRoomAvailable_shouldReturnFalse_ifStartIsAfterEnd() {
        // When
        boolean available = bookingService.isRoomAvailable(1L, futureEndTime, futureStartTime);

        // Then
        assertThat(available).isFalse();
        // On vérifie que la base de données n'a même pas été interrogée (court-circuit)
        verify(bookingRepository, never()).findOverlappingBookings(anyLong(), any(), any());
    }

    @Test
    void isRoomAvailable_shouldReturnTrue_ifNoOverlap() {
        // Given
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any())).thenReturn(Collections.emptyList());

        // When
        boolean available = bookingService.isRoomAvailable(1L, futureStartTime, futureEndTime);

        // Then
        assertThat(available).isTrue();
    }

    @Test
    void isRoomAvailable_shouldReturnFalse_ifOverlapExists() {
        // Given
        Booking overlappingBooking = new Booking();
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any())).thenReturn(Collections.singletonList(overlappingBooking));

        // When
        boolean available = bookingService.isRoomAvailable(1L, futureStartTime, futureEndTime);

        // Then
        assertThat(available).isFalse();
    }

    // ==========================================
    // TEST : createBooking (Complex Logic)
    // ==========================================

    @Test
    void createBooking_shouldThrowException_ifStartTimeIsInPast() {
        // Given
        LocalDateTime pastTime = LocalDateTime.now().minusDays(1);

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking("Titre", pastTime, futureEndTime, user, room))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Vous ne pouvez pas réserver dans le passé.");
    }

    @Test
    void createBooking_shouldThrowException_ifRoomNotAvailable() {
        // Given
        // On simule que la méthode findOverlappingBookings renvoie une réservation (conflit)
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any())).thenReturn(Collections.singletonList(new Booking()));

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking("Titre", futureStartTime, futureEndTime, user, room))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("La salle est déjà réservée pour cette plage horaire.");
    }

    @Test
    void createBooking_shouldThrowException_ifRoomFlagIsFalse() {
        // Given
        room.setAvailable(false);
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any())).thenReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking("Titre", futureStartTime, futureEndTime, user, room))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("La salle n'est pas disponible pour le moment.");
    }

    @Test
    void createBooking_shouldThrowException_ifStartTimeBeforeRoomAvailableFrom() {
        // Given
        room.setAvailableFrom(futureStartTime.plusHours(2)); // La salle ouvre plus tard que la demande
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any())).thenReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking("Titre", futureStartTime, futureEndTime, user, room))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("La salle n'est disponible qu'à partir du");
    }

    @Test
    void createBooking_shouldThrowException_ifEndTimeAfterRoomAvailableUntil() {
        // Given
        room.setAvailableUntil(futureStartTime.minusHours(1)); // La salle ferme avant le début
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any())).thenReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking("Titre", futureStartTime, futureEndTime, user, room))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("La salle n'est plus disponible après le");
    }

    @Test
    void createBooking_shouldSuccess_andSaveBooking() {
        // Given
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any())).thenReturn(Collections.emptyList());

        // On simule le retour du save (généralement la BD renvoie l'objet avec l'ID généré)
        Booking savedBooking = new Booking("Titre", futureStartTime, futureEndTime, user, room);
        savedBooking.setId(99L);
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        // When
        Booking result = bookingService.createBooking("Titre", futureStartTime, futureEndTime, user, room);

        // Then - Vérification du résultat
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(99L);

        // Then - Vérification que save a été appelé avec les bons arguments
        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());

        Booking capturedBooking = bookingCaptor.getValue();
        assertThat(capturedBooking.getTitle()).isEqualTo("Titre");
        assertThat(capturedBooking.getUser()).isEqualTo(user);
        assertThat(capturedBooking.getRoom()).isEqualTo(room);
        assertThat(capturedBooking.getCreatedAt()).isNotNull(); // Vérifie que le constructeur a bien initialisé la date
    }

    // ==========================================
    // TEST : cancelBooking
    // ==========================================
    @Test
    void cancelBooking_shouldCallRepositoryDelete() {
        // When
        bookingService.cancelBooking(1L);

        // Then
        verify(bookingRepository).deleteById(1L);
    }

    // ==========================================
    // TEST : getBookingById
    // ==========================================
    @Test
    void getBookingById_shouldReturnOptional() {
        // Given
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        // When
        Optional<Booking> result = bookingService.getBookingById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(booking);
    }
}