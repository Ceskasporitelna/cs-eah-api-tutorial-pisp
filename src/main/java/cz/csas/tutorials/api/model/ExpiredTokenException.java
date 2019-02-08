package cz.csas.tutorials.api.model;

/**
 * Thrown when access token has been expired.
 */
public class ExpiredTokenException extends Exception {
    public ExpiredTokenException(String message) {
        super(message);
    }
}
