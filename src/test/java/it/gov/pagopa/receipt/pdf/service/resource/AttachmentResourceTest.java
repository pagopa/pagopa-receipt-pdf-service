package it.gov.pagopa.receipt.pdf.service.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import it.gov.pagopa.receipt.pdf.service.exception.FiscalCodeNotAuthorizedException;
import it.gov.pagopa.receipt.pdf.service.model.Attachment;
import it.gov.pagopa.receipt.pdf.service.model.AttachmentsDetailsResponse;
import it.gov.pagopa.receipt.pdf.service.model.ErrorResponse;
import it.gov.pagopa.receipt.pdf.service.service.AttachmentsService;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.*;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@QuarkusTest
class AttachmentResourceTest {

    private static final String THIRD_PARTY_ID = "test-id";
    private static final String FISCAL_CODE = "AAAAAAAAAAAAAAAA";
    private static final String ATTACHMENT_URL = "url";

    @InjectMock(convertScopes = true)
    private AttachmentsService attachmentsServiceMock;

    @Inject
    private AttachmentResource sut;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SneakyThrows
    void getAttachmentDetailsSuccess() {

        AttachmentsDetailsResponse attachment = AttachmentsDetailsResponse.builder()
                .attachments(
                        Collections.singletonList(
                                Attachment.builder()
                                        .id(THIRD_PARTY_ID)
                                        .contentType("application/pdf")
                                        .name(UUID.randomUUID().toString())
                                        .url(UUID.randomUUID().toString())
                                        .build()
                        )
                ).build();

        doReturn(attachment).when(attachmentsServiceMock).getAttachmentsDetails(THIRD_PARTY_ID, FISCAL_CODE);

        String responseString =
                given()
                        .header("fiscal_code", FISCAL_CODE)
                        .when().get("/receipts/pdf/messages/" + THIRD_PARTY_ID)
                        .then()
                        .statusCode(200)
                        .contentType("application/json")
                        .extract()
                        .asString();


        assertNotNull(responseString);
        AttachmentsDetailsResponse response = objectMapper.readValue(responseString, AttachmentsDetailsResponse.class);
        assertNotNull(response);
        assertNotNull(response.getAttachments());
        assertEquals(1, response.getAttachments().size());
        assertEquals(THIRD_PARTY_ID, response.getAttachments().get(0).getId());

    }

    @Test
    @SneakyThrows
    void getAttachmentDetailsFailMissingFiscalCodeHeader() {
        String responseString =
                given()
                        .when().get("/receipts/pdf/messages/" + THIRD_PARTY_ID)
                        .then()
                        .statusCode(400)
                        .contentType("application/json")
                        .extract()
                        .asString();


        assertNotNull(responseString);
        ErrorResponse response = objectMapper.readValue(responseString, ErrorResponse.class);
        assertNotNull(response);
        assertEquals(PDFS_901.getErrorCode(), response.getAppErrorCode());
        assertEquals(BAD_REQUEST.getStatusCode(), response.getHttpStatusCode());
        assertEquals(BAD_REQUEST.getReasonPhrase(), response.getHttpStatusDescription());
        assertNotNull(response.getErrors());
        assertNotNull(response.getErrors().get(0));
        assertNotNull(response.getErrors().get(0).getMessage());

    }

    @Test
    @SneakyThrows
    void getAttachmentDetailsFailGetReceiptError() {
        doThrow(new FiscalCodeNotAuthorizedException(PDFS_700, "")).when(attachmentsServiceMock).getAttachmentsDetails(THIRD_PARTY_ID, FISCAL_CODE);

        String responseString =
                given()
                        .header("fiscal_code", FISCAL_CODE)
                        .when().get("/receipts/pdf/messages/" + THIRD_PARTY_ID)
                        .then()
                        .statusCode(500)
                        .contentType("application/json")
                        .extract()
                        .asString();


        assertNotNull(responseString);
        ErrorResponse response = objectMapper.readValue(responseString, ErrorResponse.class);
        assertNotNull(response);
        assertEquals(PDFS_700.getErrorCode(), response.getAppErrorCode());
        assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getHttpStatusCode());
        assertEquals(INTERNAL_SERVER_ERROR.getReasonPhrase(), response.getHttpStatusDescription());
        assertNotNull(response.getErrors());
        assertNotNull(response.getErrors().get(0));
        assertNotNull(response.getErrors().get(0).getMessage());

    }

    @Test
    @SneakyThrows
    void getAttachmentSuccess() {

        doReturn(null).when(attachmentsServiceMock).getAttachment(THIRD_PARTY_ID, FISCAL_CODE, ATTACHMENT_URL);

        byte[] response =
                given()
                        .header("fiscal_code", FISCAL_CODE)
                        .when().get(String.format("/receipts/pdf/messages/%s/%s", THIRD_PARTY_ID, ATTACHMENT_URL))
                        .then()
                        .statusCode(200)
                        .contentType("application/pdf")
                        .header("content-disposition", "attachment;")
                        .extract()
                        .asByteArray();


        assertNotNull(response);

    }

    @Test
    @SneakyThrows
    void getAttachmentFailMissingFiscalCodeHeader() {
        String responseString =
                given()
                        .when().get(String.format("/receipts/pdf/messages/%s/%s", THIRD_PARTY_ID, ATTACHMENT_URL))
                        .then()
                        .statusCode(400)
                        .contentType("application/json")
                        .extract()
                        .asString();


        assertNotNull(responseString);
        ErrorResponse response = objectMapper.readValue(responseString, ErrorResponse.class);
        assertNotNull(response);
        assertEquals(PDFS_901.getErrorCode(), response.getAppErrorCode());
        assertEquals(BAD_REQUEST.getStatusCode(), response.getHttpStatusCode());
        assertEquals(BAD_REQUEST.getReasonPhrase(), response.getHttpStatusDescription());
        assertNotNull(response.getErrors());
        assertNotNull(response.getErrors().get(0));
        assertNotNull(response.getErrors().get(0).getMessage());

    }

    @Test
    @SneakyThrows
    void getAttachmentFailGetReceiptError() {
        doThrow(new FiscalCodeNotAuthorizedException(PDFS_706, "")).when(attachmentsServiceMock).getAttachmentsDetails(THIRD_PARTY_ID, FISCAL_CODE);

        String responseString =
                given()
                        .header("fiscal_code", FISCAL_CODE)
                        .when().get("/receipts/pdf/messages/" + THIRD_PARTY_ID)
                        .then()
                        .statusCode(500)
                        .contentType("application/json")
                        .extract()
                        .asString();


        assertNotNull(responseString);
        ErrorResponse response = objectMapper.readValue(responseString, ErrorResponse.class);
        assertNotNull(response);
        assertEquals(PDFS_706.getErrorCode(), response.getAppErrorCode());
        assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getHttpStatusCode());
        assertEquals(INTERNAL_SERVER_ERROR.getReasonPhrase(), response.getHttpStatusDescription());
        assertNotNull(response.getErrors());
        assertNotNull(response.getErrors().get(0));
        assertNotNull(response.getErrors().get(0).getMessage());

    }
}