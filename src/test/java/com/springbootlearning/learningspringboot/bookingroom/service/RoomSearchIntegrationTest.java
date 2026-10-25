package com.springbootlearning.learningspringboot.bookingroom.service;

import com.springbootlearning.learningspringboot.bookingroom.dto.RoomSearchDto;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import com.springbootlearning.learningspringboot.bookingroom.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RoomSearchIntegrationTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomRepository roomRepository;

    @Test
    void searchRooms_shouldPaginateInDatabase_withMoreThanHundredRooms() {
        // Given : 120 salles disponibles + une salle en maintenance.
        for (int i = 1; i <= 120; i++) {
            roomRepository.save(new Room(String.format("Volume %03d", i), "desc", 30,
                    "Batiment V", true, false, false));
        }
        Room maintenance = new Room("Volume 000", "maintenance", 30,
                "Batiment V", true, false, false);
        maintenance.setAvailable(false);
        roomRepository.save(maintenance);

        RoomSearchDto criteria = new RoomSearchDto();
        criteria.setName("volume");

        // When : seules 6 salles sont demandees, la base fait le filtrage et la pagination.
        Page<Room> first = roomService.searchRooms(criteria, PageRequest.of(0, 6));
        Page<Room> second = roomService.searchRooms(criteria, PageRequest.of(1, 6));

        // Then
        assertThat(first.getTotalElements()).isEqualTo(120);
        assertThat(first.getTotalPages()).isEqualTo(20);
        assertThat(first.getContent()).hasSize(6);
        assertThat(first.getContent()).extracting(Room::getName)
                .containsExactly("Volume 001", "Volume 002", "Volume 003",
                        "Volume 004", "Volume 005", "Volume 006");
        assertThat(second.getContent()).extracting(Room::getName)
                .containsExactly("Volume 007", "Volume 008", "Volume 009",
                        "Volume 010", "Volume 011", "Volume 012");
    }

    @Test
    void searchRooms_shouldApplyAllCriteriaInDatabase() {
        // Given
        Room matching = new Room("Filtre Equipee", "desc", 50, "Batiment F", true, true, true);
        Room tooSmall = new Room("Filtre Petite", "desc", 5, "Batiment F", true, true, true);
        Room withoutEquipment = new Room("Filtre Nue", "desc", 50, "Batiment F", false, false, false);
        Room maintenance = new Room("Filtre Fermee", "desc", 50, "Batiment F", true, true, true);
        maintenance.setAvailable(false);
        roomRepository.saveAll(java.util.List.of(matching, tooSmall, withoutEquipment, maintenance));

        RoomSearchDto criteria = new RoomSearchDto();
        criteria.setName("filtre");
        criteria.setCapacity(20);
        criteria.setProjector(true);
        criteria.setWhiteboard(true);
        criteria.setVideoconferencing(true);

        // When
        Page<Room> result = roomService.searchRooms(criteria, PageRequest.of(0, 10));

        // Then : seules la salle disponible, assez grande et equipee est retournee.
        assertThat(result.getContent()).extracting(Room::getName)
                .containsExactly("Filtre Equipee");
    }
}
