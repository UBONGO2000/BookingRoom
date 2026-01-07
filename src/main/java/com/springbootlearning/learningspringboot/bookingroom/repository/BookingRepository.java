package com.springbootlearning.learningspringboot.bookingroom.repository;

import com.springbootlearning.learningspringboot.bookingroom.model.Booking;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByRoom(Room room);
    List<Booking> findByRoomAndEndTimeAfterOrderByStartTimeAsc(Room room, LocalDateTime now);
    List<Booking> findByUserUsername(String username);

    @Query("SELECT b FROM Booking b WHERE b.room.id = :roomId " +
           "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    List<Booking> findOverlappingBookings(@Param("roomId") Long roomId, 
                                          @Param("startTime") LocalDateTime startTime, 
                                          @Param("endTime") LocalDateTime endTime);
}
