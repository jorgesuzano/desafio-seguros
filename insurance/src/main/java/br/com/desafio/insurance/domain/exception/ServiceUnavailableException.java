package br.com.desafio.insurance.domain.exception;

/**
 * Thrown when a downstream dependency (catalog service, DynamoDB, SQS) is
 * temporarily unavailable — typically because a circuit breaker is OPEN.
 * Controllers should map this to HTTP 503.
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

