package com.springbootlearning.learningspringboot.bookingroom.model;

/**
 * Cycle de vie d'une reservation. Une annulation est logique : la ligne reste
 * en base pour l'historique, mais elle ne bloque plus les creneaux.
 */
public enum BookingStatus {
    CONFIRMED,
    CANCELLED
}
