package com.springbootlearning.learningspringboot.bookingroom.exception;

/**
 * Conflit metier : le creneau demande chevauche une reservation confirmee.
 * Traduit en HTTP 409 par {@link ApiExceptionHandler} ; le controleur web
 * l'affiche sous forme de message flash.
 */
public class BookingConflictException extends RuntimeException {

    public BookingConflictException(String message) {
        super(message);
    }
}
