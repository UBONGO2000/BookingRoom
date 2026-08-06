package com.springbootlearning.learningspringboot.bookingroom.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoHandlerFoundException.class)
    public String handleNotFound(NoHandlerFoundException ex, Model model) {
        model.addAttribute("errorMessage", "La page demandée n'existe pas.");
        return "error";
    }

    // RuntimeException porte des messages metier volontairement destines a l'utilisateur
    // (ex: "La salle est deja reservee pour cette plage horaire."), donc on peut les afficher.
    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException ex, Model model) {
        log.warn("Erreur metier : {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    // Toute autre exception (bug, panne DB, NPE...) est un detail d'implementation :
    // on la logge integralement cote serveur, mais on ne montre jamais son message brut
    // a l'utilisateur (risque de fuite d'infos internes : requetes SQL, chemins, etc.).
    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, Model model) {
        log.error("Erreur inattendue", ex);
        model.addAttribute("errorMessage", "Une erreur inattendue est survenue. Veuillez réessayer.");
        return "error";
    }
}