package it.gov.pagopa.receipt.pdf.service.utils;

import it.gov.pagopa.receipt.pdf.service.model.ProblemJson;
import jakarta.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.io.IOException;

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

    public static ProblemJson createProblemJson(Response.Status status, String suffix, String message) {
        return ProblemJson.builder()
                .title(status.name())
                .detail(suffix.concat(message))
                .status(status.getStatusCode())
                .build();
    }

    public static void clearTempDirectory(java.nio.file.Path workingDirPath, Logger logger) {
        try {
            FileUtils.deleteDirectory(workingDirPath.toFile());
        } catch (IOException e) {
            logger.warn("Unable to clear working directory: {}", workingDirPath, e);
        }
    }
}
