package cz.csas.tutorials.api.model;

/**
 * Thrown when access token has been expired.
 */
public class ExpiredAccessTokenException extends Exception {
    public ExpiredAccessTokenException(String message) {
        super(message);
    }
}
