package it.gov.pagopa.receipt.pdf.service.service.impl;

import io.quarkus.test.junit.QuarkusTest;
import it.gov.pagopa.receipt.pdf.service.client.ReceiptCosmosClient;
import it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum;
import it.gov.pagopa.receipt.pdf.service.exception.Aes256Exception;
import it.gov.pagopa.receipt.pdf.service.exception.IoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.IOMessage;
import it.gov.pagopa.receipt.pdf.service.model.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.service.model.receipt.ReceiptError;
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
class ReceiptCosmosServiceTest {

    private ReceiptCosmosClient receiptCosmosClientMock;
    private ReceiptCosmosService sut; // System Under Test

    private static final String EVENT_ID = "event-123";
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
        receiptCosmosClientMock = mock(ReceiptCosmosClient.class);
        sut = new ReceiptCosmosService(receiptCosmosClientMock);
    }

    // --- getReceipt Tests ---

    @Test
    @DisplayName("getReceipt: Success")
    void getReceiptSuccess() throws ReceiptNotFoundException {
        Receipt expectedReceipt = new Receipt();
        expectedReceipt.setEventId(EVENT_ID);
        when(receiptCosmosClientMock.getReceiptDocument(EVENT_ID)).thenReturn(expectedReceipt);

        Receipt result = sut.getReceipt(EVENT_ID);

        assertNotNull(result);
        assertEquals(EVENT_ID, result.getEventId());
        verify(receiptCosmosClientMock).getReceiptDocument(EVENT_ID);
    }

    @Test
    @DisplayName("getReceipt: Throws ReceiptNotFoundException when client throws it")
    void getReceiptClientThrowsException() throws ReceiptNotFoundException {
        when(receiptCosmosClientMock.getReceiptDocument(EVENT_ID))
                .thenThrow(new ReceiptNotFoundException(AppErrorCodeEnum.PDFS_800, "Not found"));

        assertThrows(ReceiptNotFoundException.class, () -> sut.getReceipt(EVENT_ID));
    }

    @Test
    @DisplayName("getReceipt: Throws ReceiptNotFoundException when client returns null")
    void getReceiptReturnsNull() throws ReceiptNotFoundException {
        when(receiptCosmosClientMock.getReceiptDocument(EVENT_ID)).thenReturn(null);

        assertThrows(ReceiptNotFoundException.class, () -> sut.getReceipt(EVENT_ID));
    }

    // --- getReceiptMessage Tests ---

    @Test
    @DisplayName("getReceiptMessage: Success")
    void getReceiptMessageSuccess() throws IoMessageNotFoundException {
        IOMessage expectedMessage = new IOMessage();
        expectedMessage.setMessageId(MESSAGE_ID);
        when(receiptCosmosClientMock.getIoMessage(MESSAGE_ID)).thenReturn(expectedMessage);

        IOMessage result = sut.getReceiptMessage(MESSAGE_ID);

        assertNotNull(result);
        assertEquals(MESSAGE_ID, result.getMessageId());
    }

    @Test
    @DisplayName("getReceiptMessage: Throws IoMessageNotFoundException when client returns null")
    void getReceiptMessageReturnsNull() throws IoMessageNotFoundException {
        when(receiptCosmosClientMock.getIoMessage(MESSAGE_ID)).thenReturn(null);

        assertThrows(IoMessageNotFoundException.class, () -> sut.getReceiptMessage(MESSAGE_ID));
    }

    // --- getReceiptError Tests ---

    @Test
    @DisplayName("getReceiptError: Success with Payload Decryption")
    void getReceiptErrorSuccessWithDecryption() throws ReceiptNotFoundException, Aes256Exception {
        String originalPayload = "SecretData";
        String encryptedPayload = Aes256Utils.encrypt(originalPayload);

        ReceiptError receiptError = new ReceiptError();
        receiptError.setMessagePayload(encryptedPayload);

        when(receiptCosmosClientMock.getReceiptError(EVENT_ID)).thenReturn(receiptError);

        ReceiptError result = sut.getReceiptError(EVENT_ID);

        assertNotNull(result);
        assertEquals(originalPayload, result.getMessagePayload(), "Payload should be decrypted correctly");
    }

    @Test
    @DisplayName("getReceiptError: Should ignore decryption error and return original payload")
    void getReceiptErrorDecryptionFails() throws ReceiptNotFoundException {
        String invalidPayload = "not-encrypted-string";
        ReceiptError receiptError = new ReceiptError();
        receiptError.setMessagePayload(invalidPayload);

        when(receiptCosmosClientMock.getReceiptError(EVENT_ID)).thenReturn(receiptError);

        // This should not throw an exception because of the 'ignored' catch block in service
        ReceiptError result = sut.getReceiptError(EVENT_ID);

        assertNotNull(result);
        assertEquals(invalidPayload, result.getMessagePayload(), "Payload should remain unchanged if decryption fails");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("getReceiptError: Throws IllegalArgumentException for invalid eventId")
    void getReceiptErrorInvalidInput(String invalidEventId) {
        assertThrows(IllegalArgumentException.class, () -> sut.getReceiptError(invalidEventId));
    }
}