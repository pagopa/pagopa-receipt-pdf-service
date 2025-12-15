package it.gov.pagopa.receipt.pdf.service.utils;

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

    sanitizedInput =
        sanitizedInput
            .replace("&", "&amp;") // Deve essere il primo!
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
}
