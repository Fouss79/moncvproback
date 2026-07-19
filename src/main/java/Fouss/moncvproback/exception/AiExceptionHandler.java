package Fouss.moncvproback.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Intercepte les erreurs liées au service IA pour renvoyer un message
 * clair et exploitable côté frontend (ex: error.response.data.message),
 * au lieu de laisser remonter un 500 générique avec la stack trace brute.
 */
@RestControllerAdvice
public class AiExceptionHandler {

    @ExceptionHandler(AiServiceUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleAiUnavailable(AiServiceUnavailableException ex) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE) // 503 : la faute n'est pas côté client
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<Map<String, String>> handleInvalidFile(InvalidFileException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400 : le fichier fourni est en cause
                .body(Map.of("message", ex.getMessage()));
    }
}