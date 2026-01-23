package it.gov.pagopa.receipt.pdf.service.exception;

/** Thrown in case no receipt message to IO is found in the CosmosDB container */
public class IoMessageNotFoundException extends Exception{

    /**
     * Constructs new exception with provided message and cause
     *
     * @param message Detail message
     */
    public IoMessageNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs new exception with provided message and cause
     *
     * @param message Detail message
     * @param cause Exception thrown
     */
    public IoMessageNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}