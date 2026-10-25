package com.springbootlearning.learningspringboot.bookingroom.repository;

import com.springbootlearning.learningspringboot.bookingroom.dto.RoomSearchDto;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtres de recherche des salles executes directement en base.
 * Le filtrage ne charge donc plus toutes les salles avant de paginer.
 */
public final class RoomSpecifications {

    private RoomSpecifications() {
    }

    public static Specification<Room> withCriteria(RoomSearchDto criteria) {
        RoomSearchDto c = criteria == null ? new RoomSearchDto() : criteria;
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (c.getName() != null && !c.getName().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")),
                        "%" + c.getName().trim().toLowerCase() + "%"));
            }
            if (c.getCapacity() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("capacity"), c.getCapacity()));
            }
            if (c.isProjector()) {
                predicates.add(cb.isTrue(root.get("projector")));
            }
            if (c.isWhiteboard()) {
                predicates.add(cb.isTrue(root.get("whiteboard")));
            }
            if (c.isVideoconferencing()) {
                predicates.add(cb.isTrue(root.get("videoconferencing")));
            }

            // Une salle en maintenance n'est jamais proposee a la reservation.
            predicates.add(cb.isTrue(root.get("available")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
