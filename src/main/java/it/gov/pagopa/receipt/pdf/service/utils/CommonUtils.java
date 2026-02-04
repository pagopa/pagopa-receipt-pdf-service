package it.gov.pagopa.receipt.pdf.service.utils;

import it.gov.pagopa.receipt.pdf.service.model.ProblemJson;
import jakarta.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static it.gov.pagopa.receipt.pdf.service.utils.Constants.CART;

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

    public static boolean isCart(String thirdPartyId){
        return thirdPartyId.contains(CART);
    }

    public static String getPaymentId(String thirdPartyId){
        return thirdPartyId.split(CART)[0];
    }

    public static String getBizEventId(String thirdPartyId){
        String[] splittedId = thirdPartyId.split(CART);
        if(isCart(thirdPartyId)){
            return splittedId.length > 1 ? splittedId[1] : null;
        }
        return thirdPartyId;
    }
}
