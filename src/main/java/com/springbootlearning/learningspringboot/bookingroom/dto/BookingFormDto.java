package com.springbootlearning.learningspringboot.bookingroom.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * Formulaire de reservation valide avant d'atteindre la base.
 * Les contraintes sont verifiees dans le controleur (@Valid) ; le service garde
 * ses propres verifications metier (chevauchement, disponibilite de la salle).
 */
public record BookingFormDto(

        @NotNull(message = "La salle est obligatoire.")
        Long roomId,

        @NotBlank(message = "L'objet de la reunion est obligatoire.")
        @Size(min = 3, max = 200, message = "Le titre doit contenir entre 3 et 200 caracteres.")
        String title,

        @NotNull(message = "L'heure de debut est obligatoire.")
        @Future(message = "L'heure de debut doit etre dans le futur.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime startTime,

        @NotNull(message = "L'heure de fin est obligatoire.")
        @Future(message = "L'heure de fin doit etre dans le futur.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime endTime
) {

    @AssertTrue(message = "L'heure de fin doit etre apres l'heure de debut.")
    public boolean isEndAfterStart() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }
}
