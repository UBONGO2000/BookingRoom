package com.springbootlearning.learningspringboot.bookingroom.exception;

import com.springbootlearning.learningspringboot.bookingroom.controller.AdminController;
import com.springbootlearning.learningspringboot.bookingroom.controller.AuthController;
import com.springbootlearning.learningspringboot.bookingroom.controller.BookingController;
import com.springbootlearning.learningspringboot.bookingroom.controller.HomeController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

// Scoped to the HTML page controllers only - ApiController (REST) has its own
// ApiExceptionHandler so API clients get JSON errors instead of the "error" view.
@ControllerAdvice(assignableTypes = {
        AuthController.class,
        AdminController.class,
        BookingController.class,
        HomeController.class
})
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoHandlerFoundException ex, Model model) {
        model.addAttribute("errorMessage", "La page demandée n'existe pas.");
        return "error";
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public String handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, Model model) {
        model.addAttribute("errorMessage", "Cette action n'est pas autorisée avec cette méthode HTTP.");
        return "error";
    }

    // 404 (et autres statuts) explicitement leves par les controleurs : sans ce
    // handler, ResponseStatusException tomberait dans le handler RuntimeException
    // ci-dessous et repondrait 200 avec une page d'erreur.
    @ExceptionHandler(ResponseStatusException.class)
    public ModelAndView handleResponseStatusException(ResponseStatusException ex, Model model) {
        log.warn("Requete refusee : {} - {}", ex.getStatusCode().value(), ex.getReason());
        model.addAttribute("errorMessage",
                ex.getReason() != null ? ex.getReason() : "La ressource demandée est introuvable.");
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.setStatus(ex.getStatusCode());
        return modelAndView;
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        log.warn("Acces refuse : {}", ex.getMessage());
        model.addAttribute("errorMessage",
                ex.getMessage() != null ? ex.getMessage() : "Vous n'avez pas les droits nécessaires.");
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