package Fouss.moncvproback.exception;


/**
 * Levée quand le fichier fourni par l'utilisateur est invalide ou
 * inexploitable (ex: PDF scanné/image sans texte, fichier corrompu).
 *
 * Contrairement à AiServiceUnavailableException (la faute est du côté du
 * service/réseau, 503), ici la faute est du côté de l'entrée utilisateur :
 * mappée en 400 Bad Request par AiExceptionHandler.
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }

    public InvalidFileException(String message, Throwable cause) {
        super(message, cause);
    }
}