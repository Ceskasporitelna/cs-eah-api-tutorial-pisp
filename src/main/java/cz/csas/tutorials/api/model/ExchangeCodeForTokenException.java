package cz.csas.tutorials.api.model;

/**
 * Thrown when anything bad happens during exchanging code for tokens.
 */
public class ExchangeCodeForTokenException extends Exception {
    public ExchangeCodeForTokenException(String message) {
        super(message);
    }
}
