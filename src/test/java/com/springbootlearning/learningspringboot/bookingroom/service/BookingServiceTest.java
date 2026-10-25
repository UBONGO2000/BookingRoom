package com.springbootlearning.learningspringboot.bookingroom.service;

import com.springbootlearning.learningspringboot.bookingroom.model.Booking;
import com.springbootlearning.learningspringboot.bookingroom.model.BookingStatus;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import com.springbootlearning.learningspringboot.bookingroom.model.User;
import com.springbootlearning.learningspringboot.bookingroom.repository.BookingRepository;
import com.springbootlearning.learningspringboot.bookingroom.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
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

    @Mock
    private RoomRepository roomRepository;

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
    // TEST : searchBookings (pagination en base)
    // ==========================================
    @Test
    void searchBookings_shouldQueryDatabaseWithNormalizedFilters() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Booking> expected = new PageImpl<>(List.of(booking), pageable, 1);
        when(bookingRepository.searchBookings("%salle conf%", "%bob%", pageable)).thenReturn(expected);

        // When
        Page<Booking> result = bookingService.searchBookings("  Salle CONf ", "BOB", pageable);

        // Then
        assertThat(result.getContent()).containsExactly(booking);
        verify(bookingRepository).searchBookings("%salle conf%", "%bob%", pageable);
    }

    @Test
    void searchBookings_withBlankFilters_shouldUseWildcard() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(bookingRepository.searchBookings("%", "%", pageable))
                .thenReturn(new PageImpl<>(List.of(booking), pageable, 1));

        // When
        bookingService.searchBookings("  ", null, pageable);

        // Then
        verify(bookingRepository).searchBookings("%", "%", pageable);
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

    @Test
    void getUpcomingBookingsByUser_shouldCallRepositoryWithCurrentTime() {
        // Given
        when(bookingRepository.findByUserUsernameAndEndTimeAfterAndStatusOrderByStartTimeAsc(
                eq("alice"), any(LocalDateTime.class), eq(BookingStatus.CONFIRMED)))
                .thenReturn(Collections.singletonList(booking));

        // When
        List<Booking> result = bookingService.getUpcomingBookingsByUser("alice");

        // Then
        assertThat(result).containsExactly(booking);
        verify(bookingRepository)
                .findByUserUsernameAndEndTimeAfterAndStatusOrderByStartTimeAsc(
                        eq("alice"), any(LocalDateTime.class), eq(BookingStatus.CONFIRMED));
    }

    // ==========================================
    // TEST : getBookingsByRoom
    // ==========================================
    @Test
    void getBookingsByRoom_shouldCallRepositoryWithCurrentTime() {
        // Given
        when(bookingRepository.findByRoomAndEndTimeAfterAndStatusOrderByStartTimeAsc(
                any(Room.class), any(LocalDateTime.class), eq(BookingStatus.CONFIRMED)))
                .thenReturn(Collections.singletonList(booking));

        // When
        List<Booking> result = bookingService.getBookingsByRoom(room);

        // Then
        assertThat(result).hasSize(1);
        verify(bookingRepository).findByRoomAndEndTimeAfterAndStatusOrderByStartTimeAsc(
                eq(room), any(LocalDateTime.class), eq(BookingStatus.CONFIRMED));
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
        verify(bookingRepository, never()).findOverlappingBookings(anyLong(), any(), any(), any());
    }

    @Test
    void isRoomAvailable_shouldReturnTrue_ifNoOverlap() {
        // Given
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any(), eq(BookingStatus.CONFIRMED)))
                .thenReturn(Collections.emptyList());

        // When
        boolean available = bookingService.isRoomAvailable(1L, futureStartTime, futureEndTime);

        // Then
        assertThat(available).isTrue();
    }

    @Test
    void isRoomAvailable_shouldReturnFalse_ifOverlapExists() {
        // Given
        Booking overlappingBooking = new Booking();
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any(), eq(BookingStatus.CONFIRMED)))
                .thenReturn(Collections.singletonList(overlappingBooking));

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
        verify(roomRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void createBooking_shouldThrowException_ifRoomNotAvailable() {
        // Given
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
        // On simule que la méthode findOverlappingBookings renvoie une réservation (conflit)
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any(), eq(BookingStatus.CONFIRMED)))
                .thenReturn(Collections.singletonList(new Booking()));

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking("Titre", futureStartTime, futureEndTime, user, room))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("La salle est déjà réservée pour cette plage horaire.");
    }

    @Test
    void createBooking_shouldThrowException_ifRoomFlagIsFalse() {
        // Given
        room.setAvailable(false);
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any(), eq(BookingStatus.CONFIRMED)))
                .thenReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking("Titre", futureStartTime, futureEndTime, user, room))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("La salle n'est pas disponible pour le moment.");
    }

    @Test
    void createBooking_shouldThrowException_ifStartTimeBeforeRoomAvailableFrom() {
        // Given
        room.setAvailableFrom(futureStartTime.plusHours(2)); // La salle ouvre plus tard que la demande
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any(), eq(BookingStatus.CONFIRMED)))
                .thenReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking("Titre", futureStartTime, futureEndTime, user, room))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("La salle n'est disponible qu'à partir du");
    }

    @Test
    void createBooking_shouldThrowException_ifEndTimeAfterRoomAvailableUntil() {
        // Given
        room.setAvailableUntil(futureStartTime.minusHours(1)); // La salle ferme avant le début
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any(), eq(BookingStatus.CONFIRMED)))
                .thenReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking("Titre", futureStartTime, futureEndTime, user, room))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("La salle n'est plus disponible après le");
    }

    @Test
    void createBooking_shouldSuccess_andSaveBooking() {
        // Given
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
        when(bookingRepository.findOverlappingBookings(anyLong(), any(), any(), eq(BookingStatus.CONFIRMED)))
                .thenReturn(Collections.emptyList());

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
    // TEST : cancelBooking (annulation logique)
    // ==========================================
    @Test
    void cancelBooking_shouldMarkBookingAsCancelled() {
        // Given
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        // When
        bookingService.cancelBooking(1L);

        // Then
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancelledAt()).isNotNull();
        verify(bookingRepository).save(booking);
    }

    @Test
    void cancelBooking_shouldBeIdempotent_whenAlreadyCancelled() {
        // Given
        booking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        // When
        bookingService.cancelBooking(1L);

        // Then
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void cancelBooking_shouldThrowException_whenBookingDoesNotExist() {
        // Given
        when(bookingRepository.findById(404L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookingService.cancelBooking(404L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cette réservation n'existe pas.");
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