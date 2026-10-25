package com.springbootlearning.learningspringboot.bookingroom.exception;

/**
 * Levee quand un compte existe deja (username ou email).
 * Le champ permet au controleur de rattacher l'erreur au bon input du formulaire.
 */
public class UserAlreadyExistsException extends IllegalArgumentException {

    private final String field;

    public UserAlreadyExistsException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
