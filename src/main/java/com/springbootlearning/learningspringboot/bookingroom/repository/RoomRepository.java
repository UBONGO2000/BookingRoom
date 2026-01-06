package com.springbootlearning.learningspringboot.bookingroom.repository;

import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Boolean existsByName(String name);
}
