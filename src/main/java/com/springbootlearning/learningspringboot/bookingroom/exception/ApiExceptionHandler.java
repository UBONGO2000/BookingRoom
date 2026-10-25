package com.springbootlearning.learningspringboot.bookingroom.exception;

import com.springbootlearning.learningspringboot.bookingroom.controller.ApiController;
import com.springbootlearning.learningspringboot.bookingroom.dto.ApiErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Scoped to ApiController so REST clients always get a JSON body on error,
// instead of falling through to GlobalExceptionHandler's HTML "error" view.
@RestControllerAdvice(assignableTypes = ApiController.class)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse("Identifiants invalides"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse("Accès refusé"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("Requête invalide");
        return ResponseEntity.badRequest().body(new ApiErrorResponse(message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("Paramètre invalide");
        return ResponseEntity.badRequest().body(new ApiErrorResponse(message));
    }

    // Conflit de creneau : 409, plus precis qu'un 400 pour un client REST.
    @ExceptionHandler(BookingConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleBookingConflict(BookingConflictException ex) {
        log.warn("Conflit de reservation (API) : {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(ex.getMessage()));
    }

    // RuntimeException porte des messages metier volontairement destines a l'utilisateur
    // (ex: "La salle est deja reservee pour cette plage horaire."), donc on peut les exposer.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.warn("Erreur metier (API) : {}", ex.getMessage());
        return ResponseEntity.badRequest().body(new ApiErrorResponse(ex.getMessage()));
    }

    // Toute autre exception (bug, panne DB, NPE...) est un detail d'implementation :
    // on la logge integralement cote serveur, mais on ne montre jamais son message brut au client.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception ex) {
        log.error("Erreur inattendue (API)", ex);
        return ResponseEntity.internalServerError()
                .body(new ApiErrorResponse("Une erreur inattendue est survenue."));
    }
}
