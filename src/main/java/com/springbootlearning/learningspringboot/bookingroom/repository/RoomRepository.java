package com.springbootlearning.learningspringboot.bookingroom.repository;

import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Boolean existsByName(String name);
    Optional<Room> findByNameIgnoreCase(String name);
}
