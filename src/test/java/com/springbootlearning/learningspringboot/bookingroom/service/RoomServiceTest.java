package com.springbootlearning.learningspringboot.bookingroom.service;

import com.springbootlearning.learningspringboot.bookingroom.dto.RoomSearchDto;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import com.springbootlearning.learningspringboot.bookingroom.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomService roomService;

    private Room room;

    @BeforeEach
    void setUp() {
        // On crée une salle standard pour les tests
        room = new Room("Salle A", "bla bla bla", 40, "2 rue de la souveraineté", true, false, false);
        room.setId(10L);
        room.setAvailable(false);
    }

    // ==========================================
    // TESTS LECTURE
    // ==========================================

    @Test
    void getAllRooms_shouldReturnAllRooms() {
        // Given
        List<Room> rooms = Arrays.asList(room, new Room());
        when(roomRepository.findAll()).thenReturn(rooms);

        // When
        List<Room> result = roomService.getAllRooms();

        // Then
        assertThat(result).hasSize(2);
        verify(roomRepository).findAll();
    }

    @Test
    void getAllRoomsPageable_shouldReturnPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Room> page = new PageImpl<>(Arrays.asList(room));
        when(roomRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<Room> result = roomService.getAllRooms(pageable);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.getContent()).hasSize(1);
        verify(roomRepository).findAll(pageable);
    }

    @Test
    void getRoomById_shouldReturnRoom() {
        // Given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        // When
        Optional<Room> result = roomService.getRoomById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(room);
        verify(roomRepository).findById(1L);
    }

    @Test
    void getRoomByName_shouldReturnRoom() {
        // Given
        when(roomRepository.findByNameIgnoreCase("Salle A")).thenReturn(Optional.of(room));

        // When
        Optional<Room> result = roomService.getRoomByName("Salle A");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(room);
        verify(roomRepository).findByNameIgnoreCase("Salle A");
    }

    @Test
    void getRoomByName_shouldReturnEmpty_ifNameBlank() {
        // When
        Optional<Room> result = roomService.getRoomByName("   ");

        // Then
        assertThat(result).isEmpty();
        // On vérifie que le repository n'a même pas été appelé (optimisation)
        verify(roomRepository, never()).findByNameIgnoreCase(anyString());
    }

    // ==========================================
    // TEST RECHERCHE (FILTRAGE)
    // ==========================================

    @Test
    void searchRooms_shouldFilterByCapacity() {
        // Given : On prépare 3 salles avec des capacités différentes
        Room smallRoom = new Room("Petite", "desc", 10, "adr", true, false, false);
        Room mediumRoom = new Room("Moyenne", "desc", 20, "adr", true, false, false);
        Room bigRoom = new Room("Grande", "desc", 50, "adr", true, false, false);

        List<Room> allRooms = Arrays.asList(smallRoom, mediumRoom, bigRoom);
        when(roomRepository.findAll()).thenReturn(allRooms);

        RoomSearchDto criteria = new RoomSearchDto();
        criteria.setCapacity(25); // On veut au moins 25 places

        // When
        List<Room> result = roomService.searchRooms(criteria);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCapacity()).isEqualTo(50);
    }

    @Test
    void searchRoomsPageable_shouldReturnCorrectPage() {
        // Given : Une liste de 3 salles
        Room r1 = new Room("Room 1", "desc", 10, "adr", true, false, false);
        Room r2 = new Room("Room 2", "desc", 20, "adr", true, false, false);
        Room r3 = new Room("Room 3", "desc", 30, "adr", true, false, false);
        List<Room> allRooms = Arrays.asList(r1, r2, r3);

        when(roomRepository.findAll()).thenReturn(allRooms);

        // On demande la page 1 (0-based index = 0) avec une taille de 2
        Pageable pageable = PageRequest.of(0, 2);

        // When
        Page<Room> result = roomService.searchRooms(new RoomSearchDto(), pageable);

        // Then
        assertThat(result.getContent()).hasSize(2); // On doit avoir r1 et r2
        assertThat(result.getTotalElements()).isEqualTo(3); // Total est 3
    }

    // ==========================================
    // TESTS ECRITURE / MODIFICATION
    // ==========================================

    @Test
    void updateRoomAvailability() {
        // Given
        // On simule que le repository trouve la salle
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        // On simule le retour de save (pour éviter NullPointerException)
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        // When : On passe la disponibilité à TRUE
        roomService.updateRoomAvailability(10L, true);

        // Then : Vérifications
        verify(roomRepository).findById(10L);

        // On vérifie que 'save' a été appelé et on capture l'objet pour vérifier son état
        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(roomCaptor.capture());

        Room savedRoom = roomCaptor.getValue();
        assertThat(savedRoom.isAvailable()).isTrue(); // Vérifie que la valeur a changé
    }

    @Test
    void saveRoom_shouldSaveRoom() {
        // Given
        Room newRoom = new Room("Nouvelle Salle", "desc", 15, "adr", true, false, false);
        when(roomRepository.save(newRoom)).thenReturn(newRoom);

        // When
        Room result = roomService.saveRoom(newRoom);

        // Then
        assertThat(result).isNotNull();
        verify(roomRepository).save(newRoom);
    }

    @Test
    void deleteRoom_shouldDeleteRoom() {
        // When (Méthode void, pas de retour à vérifier)
        roomService.deleteRoom(5L);

        // Then : On vérifie que la méthode delete du repository a bien été appelée
        verify(roomRepository).deleteById(5L);
    }
}