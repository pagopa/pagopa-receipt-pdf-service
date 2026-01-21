package it.gov.pagopa.receipt.pdf.service.exception;

import lombok.Getter;

/**
 * Thrown in case an error occur when encrypting or decrypting a BizEvent
 */
@Getter
public class Aes256Exception extends Exception{
    private final int statusCode;

    /**
     * Constructs new exception with provided message
     *
     * @param message Detail message
     * @param statusCode status code
     */
    public Aes256Exception(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Constructs new exception with provided message
     *
     * @param message Detail message
     * @param statusCode status code
     * @param cause Exception causing the constructed one
     */
    public Aes256Exception(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }
}
