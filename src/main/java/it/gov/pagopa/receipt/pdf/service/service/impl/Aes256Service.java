package it.gov.pagopa.receipt.pdf.service.service.impl;

import it.gov.pagopa.receipt.pdf.service.exception.Aes256Exception;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * AES-256 (CBC/PKCS5Padding) encryption/decryption service.
 * <p>
 * Key material is injected via configuration properties {@code aes.secret.key} and {@code aes.salt},
 * both usually bound to environment variables {@code AES_SECRET_KEY} / {@code AES_SALT}.
 */
@ApplicationScoped
public class Aes256Service {

    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 65536;
    private static final int IV_LENGTH = 16;
    private static final int AES_UNEXPECTED_ERROR = 701;

    static final String PBKDF_2_WITH_HMAC_SHA_256 = "PBKDF2WithHmacSHA256";
    static final String AES_CBC_PKCS_5_PADDING = "AES/CBC/PKCS5Padding";
    static final String ALGORITHM = "AES";

    private final String secretKey;
    private final String salt;
    private final SecureRandom secureRandom = new SecureRandom();

    @Inject
    public Aes256Service(
            @ConfigProperty(name = "aes.secret.key", defaultValue = "") String secretKey,
            @ConfigProperty(name = "aes.salt", defaultValue = "") String salt
    ) {
        this.secretKey = secretKey;
        this.salt = salt;
    }

    public String encrypt(String strToEncrypt) throws Aes256Exception {
        if (strToEncrypt == null) {
            throw new Aes256Exception("Input to encrypt cannot be null", AES_UNEXPECTED_ERROR);
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            SecretKeySpec secretKeySpec = deriveKey();

            //Padding vulnerability rule java:S5542 ignored because encryption is used inside application workflow
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS_5_PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivspec);

            byte[] cipherText = cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8));
            byte[] encryptedData = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, encryptedData, 0, iv.length);
            System.arraycopy(cipherText, 0, encryptedData, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeySpecException
                 | InvalidKeyException | InvalidAlgorithmParameterException
                 | IllegalBlockSizeException | BadPaddingException e) {
            throw new Aes256Exception("Unexpected error when encrypting the given string", AES_UNEXPECTED_ERROR, e);
        }
    }

    public String decrypt(String strToDecrypt) throws Aes256Exception {
        if (strToDecrypt == null) {
            throw new Aes256Exception("Input to decrypt cannot be null", AES_UNEXPECTED_ERROR);
        }
        try {
            byte[] encryptedData = Base64.getDecoder().decode(strToDecrypt);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(encryptedData, 0, iv, 0, iv.length);
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            SecretKeySpec secretKeySpec = deriveKey();

            //Padding vulnerability rule java:S5542 ignored because decryption is used inside application workflow
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS_5_PADDING);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivspec);

            byte[] cipherText = new byte[encryptedData.length - IV_LENGTH];
            System.arraycopy(encryptedData, IV_LENGTH, cipherText, 0, cipherText.length);

            byte[] decryptedText = cipher.doFinal(cipherText);
            return new String(decryptedText, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | NoSuchAlgorithmException | NoSuchPaddingException
                 | InvalidKeySpecException | InvalidKeyException | InvalidAlgorithmParameterException
                 | IllegalBlockSizeException | BadPaddingException e) {
            throw new Aes256Exception("Unexpected error when decrypting the given string", AES_UNEXPECTED_ERROR, e);
        }
    }

    private SecretKeySpec deriveKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF_2_WITH_HMAC_SHA_256);
        KeySpec spec = new PBEKeySpec(secretKey.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), ITERATION_COUNT, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
    }
}

