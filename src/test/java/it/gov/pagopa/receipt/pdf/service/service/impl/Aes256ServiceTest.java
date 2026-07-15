package it.gov.pagopa.receipt.pdf.service.service.impl;

import it.gov.pagopa.receipt.pdf.service.exception.Aes256Exception;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class Aes256ServiceTest {

    private static final String TEST_KEY = "test-secret-key-must-be-long-enough-32";
    private static final String TEST_SALT = "test-salt-value";

    private final Aes256Service aes = new Aes256Service(TEST_KEY, TEST_SALT);

    @Test
    @DisplayName("Encryption and Decryption: standard success case")
    void encryptDecryptSuccess() throws Aes256Exception {
        String originalText = "FiscalCode123XYZ!";

        String encryptedText = aes.encrypt(originalText);

        assertNotNull(encryptedText, "Encrypted text should not be null");
        assertNotEquals(originalText, encryptedText, "Encrypted text should differ from the original text");

        String decryptedText = aes.decrypt(encryptedText);
        assertEquals(originalText, decryptedText, "Decrypted text must match the original input");
    }

    @Test
    @DisplayName("Stochastic encryption: same input should produce different outputs due to random IV")
    void encryptionProducesDifferentOutputs() throws Aes256Exception {
        String input = "random-iv-test";

        String cipher1 = aes.encrypt(input);
        String cipher2 = aes.encrypt(input);

        assertNotEquals(cipher1, cipher2, "Using a random IV must result in different Base64 strings for the same input");

        // Both must still decrypt to the same original value
        assertEquals(input, aes.decrypt(cipher1));
        assertEquals(input, aes.decrypt(cipher2));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            " ",
            "Special characters: àèìòù €",
            "{\"jsonKey\": \"jsonValue\"}",
            "A very long string used to test potential padding and block size limits during the encryption process."
    })
    @DisplayName("Encryption and Decryption: various string formats")
    void encryptDecryptVariousInputs(String input) throws Aes256Exception {
        String encrypted = aes.encrypt(input);
        assertEquals(input, aes.decrypt(encrypted), "Decryption failed for input: " + input);
    }

    @Test
    @DisplayName("Encryption exception: null input")
    void encryptNullThrowsException() {
        Aes256Exception exception = assertThrows(Aes256Exception.class, () -> aes.encrypt(null));
        assertEquals(701, exception.getStatusCode());
    }

    @Test
    @DisplayName("Decryption exception: invalid Base64 string")
    void decryptInvalidBase64() {
        String invalidBase64 = "!!!NotValidBase64!!!";

        Aes256Exception exception = assertThrows(Aes256Exception.class, () -> aes.decrypt(invalidBase64));
        assertEquals(701, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Unexpected error when decrypting"));
    }

    @Test
    @DisplayName("Decryption exception: input encrypted with different keys")
    void decryptWithWrongSetup() throws Aes256Exception {
        String encryptedWithOriginalKeys = aes.encrypt("confidential message");

        // Different instance with mismatched keys to simulate a configuration mismatch
        Aes256Service wrongAes = new Aes256Service("wrong-secret-key-value", "wrong-salt");

        assertThrows(Aes256Exception.class,
                () -> wrongAes.decrypt(encryptedWithOriginalKeys),
                "Decryption should fail when keys do not match");
    }
}

