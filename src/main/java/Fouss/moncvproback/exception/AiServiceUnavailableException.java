package Fouss.moncvproback.exception;

/**
 * Levée quand l'appel au service IA (Mistral) échoue pour une raison qui
 * n'est pas imputable à l'utilisateur : problème réseau/DNS, timeout,
 * erreur HTTP renvoyée par Mistral (401, 429, 5xx...), réponse vide, etc.
 *
 * Le message porté par cette exception est déjà rédigé pour être affiché
 * tel quel à l'utilisateur (voir AiExceptionHandler).
 */
public class AiServiceUnavailableException extends RuntimeException {

    public AiServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiServiceUnavailableException(String message) {
        super(message);
    }
}