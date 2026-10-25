package com.springbootlearning.learningspringboot.bookingroom.repository;

import com.springbootlearning.learningspringboot.bookingroom.model.BookingStatus;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {
    Boolean existsByName(String name);
    Optional<Room> findByNameIgnoreCase(String name);
    Page<Room> findByAvailableTrue(Pageable pageable);

    long countByAvailableTrue();

    /**
     * Salles libres sur un creneau donne, en une seule requete (NOT EXISTS).
     * Remplace le parcours de toutes les salles avec une requete par salle.
     */
    @Query("""
            SELECT r FROM Room r
            WHERE r.available = true
              AND NOT EXISTS (
                  SELECT b FROM Booking b
                  WHERE b.room = r
                    AND b.status = :status
                    AND b.startTime < :endTime
                    AND b.endTime > :startTime
              )
            ORDER BY LOWER(r.name), r.id
            """)
    List<Room> findAvailableBetween(@Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime,
                                    @Param("status") BookingStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);
}
