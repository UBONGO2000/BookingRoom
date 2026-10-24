package com.springbootlearning.learningspringboot.bookingroom.repository;

import com.springbootlearning.learningspringboot.bookingroom.model.Booking;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Override
    @EntityGraph(attributePaths = {"room", "user"})
    List<Booking> findAll();

    @Override
    @EntityGraph(attributePaths = {"room", "user"})
    Optional<Booking> findById(Long id);

    List<Booking> findByRoom(Room room);

    @EntityGraph(attributePaths = {"room", "user"})
    List<Booking> findByRoomAndEndTimeAfterOrderByStartTimeAsc(Room room, LocalDateTime now);

    @EntityGraph(attributePaths = {"room", "user"})
    List<Booking> findByUserUsername(String username);

    @EntityGraph(attributePaths = {"room", "user"})
    List<Booking> findByUserUsernameAndEndTimeAfterOrderByStartTimeAsc(String username, LocalDateTime now);

    @Query("SELECT b FROM Booking b WHERE b.room.id = :roomId " +
           "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    List<Booking> findOverlappingBookings(@Param("roomId") Long roomId, 
                                          @Param("startTime") LocalDateTime startTime, 
                                          @Param("endTime") LocalDateTime endTime);
}
