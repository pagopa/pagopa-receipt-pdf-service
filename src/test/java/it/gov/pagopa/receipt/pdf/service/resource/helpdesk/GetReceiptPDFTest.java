package it.gov.pagopa.receipt.pdf.service.resource.helpdesk;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import it.gov.pagopa.receipt.pdf.service.exception.AttachmentNotFoundException;
import it.gov.pagopa.receipt.pdf.service.exception.BlobStorageClientException;
import it.gov.pagopa.receipt.pdf.service.service.AttachmentsService;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.PDFS_600;
import static it.gov.pagopa.receipt.pdf.service.enumeration.AppErrorCodeEnum.PDFS_602;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.when;

@QuarkusTest
class GetReceiptPDFTest {

    private static final String FILE_NAME = "receipt.pdf";

    @InjectMock
    private AttachmentsService attachmentsServiceMock;

    @Test
    void getReceiptPdf_BadRequest_FileNameIsBlank() {
        given()
                .pathParam("file-name", " ")
                .when()
                .get("/helpdesk/pdf-receipts/{file-name}")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("title", equalTo("BAD_REQUEST"))
                .body("detail", equalTo("Please pass a valid file name"));
    }

    @Test
    void getReceiptPdf_Success() throws Exception {
        byte[] expectedPdfContent = "PDF_DUMMY_CONTENT".getBytes();

        when(attachmentsServiceMock.getAttachmentBytesFromBlobStorage(FILE_NAME))
                .thenReturn(expectedPdfContent);

        byte[] responseBody = given()
                .pathParam("file-name", FILE_NAME)
                .when()
                .get("/helpdesk/pdf-receipts/{file-name}")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .asByteArray();

        assertArrayEquals(expectedPdfContent, responseBody);
    }

    @Test
    void getReceiptPdf_NotFound_BlobStorageException() throws Exception {
        String expectedMessage = String.format("Unable to retrieve the receipt pdf with file name %s", FILE_NAME);

        when(attachmentsServiceMock.getAttachmentBytesFromBlobStorage(FILE_NAME))
                .thenThrow(new BlobStorageClientException(PDFS_600, PDFS_600.getErrorMessage()));

        given()
                .pathParam("file-name", FILE_NAME)
                .when()
                .get("/helpdesk/pdf-receipts/{file-name}")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("title", equalTo("NOT_FOUND"))
                .body("detail", equalTo(expectedMessage));
    }

    @Test
    void getReceiptPdf_NotFound_AttachmentNotFound() throws Exception {
        String expectedMessage = String.format("Unable to retrieve the receipt pdf with file name %s", FILE_NAME);

        when(attachmentsServiceMock.getAttachmentBytesFromBlobStorage(FILE_NAME))
                .thenThrow(new AttachmentNotFoundException(PDFS_602, PDFS_602.getErrorMessage()));

        given()
                .pathParam("file-name", FILE_NAME)
                .when()
                .get("/helpdesk/pdf-receipts/{file-name}")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body("detail", equalTo(expectedMessage));
    }
}