package it.gov.pagopa.receipt.pdf.service.service.impl;

import io.quarkus.test.junit.QuarkusTest;
import it.gov.pagopa.receipt.pdf.service.client.CartReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.Aes256Exception;
import it.gov.pagopa.receipt.pdf.service.exception.CartNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.IoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.IOMessage;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.service.model.cart.CartReceiptError;
import it.gov.pagopa.receipt.pdf.service.utils.Aes256Utils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class CartReceiptCosmosServiceTest {

    private CartReceiptCosmosClient cartReceiptCosmosClientMock;
    private CartReceiptCosmosService sut; // System Under Test

    private static final String CART_ID = "cart-123";
    private static final String MESSAGE_ID = "msg-456";
    private static final String TEST_KEY = "test-secret-key-32-chars-long-!!";
    private static final String TEST_SALT = "test-salt";

    @BeforeAll
    static void globalSetup() {
        // Initialize Aes256Utils with test keys so internal decryption calls don't fail
        Aes256Utils.setKeys(TEST_KEY, TEST_SALT);
    }

    @BeforeEach
    void setUp() {
        cartReceiptCosmosClientMock = mock(CartReceiptCosmosClient.class);
        sut = new CartReceiptCosmosService(cartReceiptCosmosClientMock);
    }

    // --- getCartReceipt Tests ---

    @Test
    @DisplayName("getCartReceipt: Success")
    void getCartReceiptSuccess() throws CartNotFoundException {
        CartForReceipt expectedReceipt = new CartForReceipt();
        expectedReceipt.setId(CART_ID);
        when(cartReceiptCosmosClientMock.getCartForReceiptDocument(CART_ID)).thenReturn(expectedReceipt);

        CartForReceipt result = sut.getCartReceipt(CART_ID);

        assertNotNull(result);
        assertEquals(CART_ID, result.getId());
        verify(cartReceiptCosmosClientMock).getCartForReceiptDocument(CART_ID);
    }

    @Test
    @DisplayName("getCartReceipt: Throws CartNotFoundException when client throws it")
    void getCartReceiptClientThrowsException() throws CartNotFoundException {
        when(cartReceiptCosmosClientMock.getCartForReceiptDocument(CART_ID))
                .thenThrow(new CartNotFoundException(AppErrorCodeEnum.PDFS_800, "Not found"));

        assertThrows(CartNotFoundException.class, () -> sut.getCartReceipt(CART_ID));
    }

    @Test
    @DisplayName("getCartReceipt: Throws CartNotFoundException when client returns null")
    void getCartReceiptReturnsNull() throws CartNotFoundException {
        when(cartReceiptCosmosClientMock.getCartForReceiptDocument(CART_ID)).thenReturn(null);

        assertThrows(CartNotFoundException.class, () -> sut.getCartReceipt(CART_ID));
    }

    // --- getCartReceiptMessage Tests ---

    @Test
    @DisplayName("getCartReceiptMessage: Success")
    void getCartReceiptMessageSuccess() throws IoMessageNotFoundException {
        IOMessage expectedMessage = new IOMessage();
        expectedMessage.setMessageId(MESSAGE_ID);
        when(cartReceiptCosmosClientMock.getCartIoMessage(MESSAGE_ID)).thenReturn(expectedMessage);

        IOMessage result = sut.getCartReceiptMessage(MESSAGE_ID);

        assertNotNull(result);
        assertEquals(MESSAGE_ID, result.getMessageId());
    }

    @Test
    @DisplayName("getCartReceiptMessage: Throws IoMessageNotFoundException when client returns null")
    void getCartReceiptMessageReturnsNull() throws IoMessageNotFoundException {
        when(cartReceiptCosmosClientMock.getCartIoMessage(MESSAGE_ID)).thenReturn(null);

        assertThrows(IoMessageNotFoundException.class, () -> sut.getCartReceiptMessage(MESSAGE_ID));
    }

    // --- getCartReceiptError Tests ---

    @Test
    @DisplayName("getCartReceiptError: Success with Payload Decryption")
    void getCartReceiptErrorSuccessWithDecryption() throws CartNotFoundException, Aes256Exception {
        String originalPayload = "SecretData";
        String encryptedPayload = Aes256Utils.encrypt(originalPayload);

        CartReceiptError receiptError = new CartReceiptError();
        receiptError.setMessagePayload(encryptedPayload);

        when(cartReceiptCosmosClientMock.getCartReceiptError(CART_ID)).thenReturn(receiptError);

        CartReceiptError result = sut.getCartReceiptError(CART_ID);

        assertNotNull(result);
        assertEquals(originalPayload, result.getMessagePayload(), "Payload should be decrypted correctly");
    }

    @Test
    @DisplayName("getCartReceiptError: Should ignore decryption error and return original payload")
    void getCartReceiptErrorDecryptionFails() throws CartNotFoundException {
        String invalidPayload = "not-encrypted-string";
        CartReceiptError receiptError = new CartReceiptError();
        receiptError.setMessagePayload(invalidPayload);

        when(cartReceiptCosmosClientMock.getCartReceiptError(CART_ID)).thenReturn(receiptError);

        // This should not throw an exception because of the 'ignored' catch block in service
        CartReceiptError result = sut.getCartReceiptError(CART_ID);

        assertNotNull(result);
        assertEquals(invalidPayload, result.getMessagePayload(), "Payload should remain unchanged if decryption fails");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("getCartReceiptError: Throws IllegalArgumentException for invalid cartId")
    void getCartReceiptErrorInvalidInput(String invalidCartId) {
        assertThrows(IllegalArgumentException.class, () -> sut.getCartReceiptError(invalidCartId));
    }
}