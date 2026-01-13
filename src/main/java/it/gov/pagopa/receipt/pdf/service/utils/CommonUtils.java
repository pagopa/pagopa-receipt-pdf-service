package it.gov.pagopa.receipt.pdf.service.utils;

import it.gov.pagopa.receipt.pdf.service.model.ProblemJson;
import jakarta.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonUtils {


    /**
     * This method sanitizes the input string by escaping special characters to prevent XSS attacks.
     *
     * @param input the input string to be sanitized
     * @return the sanitized string
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }

        String sanitizedInput = input.trim();

        sanitizedInput = sanitizedInput
                .replace("&", "&amp;")  // Deve essere il primo!
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;")
                .replaceAll("[\\r\\n\\t]", "")
                .replaceAll("[\n\r]", "");

        sanitizedInput = sanitizedInput.replace("\0", "");

        return sanitizedInput;
    }

    /**
     * This method sanitizes input intended to be written to logs by removing control characters
     * (such as newlines, carriage returns, tabs, and null characters) that could be used to
     * forge or obfuscate log entries.
     *
     * @param input the input string to be sanitized for logging
     * @return the sanitized string safe for logging, or {@code null} if input is null
     */
    public static String sanitizeForLog(String input) {
        if (input == null) {
            return null;
        }

        String sanitizedInput = input.trim();

        // Remove all ASCII control characters (0x00-0x1F), including \r, \n, \t and NUL
        sanitizedInput = sanitizedInput.replaceAll("[\\x00-\\x1F]", "");

        return sanitizedInput;
    }

    public static ProblemJson createProblemJson(Response.Status status, String message) {
        return ProblemJson.builder()
                .title(status.name())
                .detail(message)
                .status(status.getStatusCode())
                .build();
    }
}
