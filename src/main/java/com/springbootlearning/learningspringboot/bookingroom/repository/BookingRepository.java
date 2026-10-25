package com.springbootlearning.learningspringboot.bookingroom.repository;

import com.springbootlearning.learningspringboot.bookingroom.model.Booking;
import com.springbootlearning.learningspringboot.bookingroom.model.BookingStatus;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    List<Booking> findByRoomAndEndTimeAfterAndStatusOrderByStartTimeAsc(Room room, LocalDateTime now, BookingStatus status);

    @EntityGraph(attributePaths = {"room", "user"})
    List<Booking> findByUserUsername(String username);

    @EntityGraph(attributePaths = {"room", "user"})
    Page<Booking> findByUserUsername(String username, Pageable pageable);

    @EntityGraph(attributePaths = {"room", "user"})
    List<Booking> findByUserUsernameAndEndTimeAfterAndStatusOrderByStartTimeAsc(String username, LocalDateTime now, BookingStatus status);

    boolean existsByRoomIdAndEndTimeAfterAndStatus(Long roomId, LocalDateTime now, BookingStatus status);

    @EntityGraph(attributePaths = {"room", "user"})
    @Query("""
            SELECT b FROM Booking b
            WHERE LOWER(b.room.name) LIKE :roomFilter
              AND LOWER(b.user.username) LIKE :usernameFilter
            """)
    Page<Booking> searchBookings(@Param("roomFilter") String roomFilter,
                                 @Param("usernameFilter") String usernameFilter,
                                 Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.room.id = :roomId AND b.status = :status " +
           "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    List<Booking> findOverlappingBookings(@Param("roomId") Long roomId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime,
                                          @Param("status") BookingStatus status);
}
