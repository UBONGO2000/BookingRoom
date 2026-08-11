package com.springbootlearning.learningspringboot.bookingroom.exception;

import com.springbootlearning.learningspringboot.bookingroom.controller.ApiController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// Scoped to ApiController so REST clients always get a JSON body on error,
// instead of falling through to GlobalExceptionHandler's HTML "error" view.
@RestControllerAdvice(assignableTypes = ApiController.class)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Identifiants invalides"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Accès refusé"));
    }

    // RuntimeException porte des messages metier volontairement destines a l'utilisateur
    // (ex: "La salle est deja reservee pour cette plage horaire."), donc on peut les exposer.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        log.warn("Erreur metier (API) : {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    // Toute autre exception (bug, panne DB, NPE...) est un detail d'implementation :
    // on la logge integralement cote serveur, mais on ne montre jamais son message brut au client.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception ex) {
        log.error("Erreur inattendue (API)", ex);
        return ResponseEntity.internalServerError().body(Map.of("error", "Une erreur inattendue est survenue."));
    }
}
