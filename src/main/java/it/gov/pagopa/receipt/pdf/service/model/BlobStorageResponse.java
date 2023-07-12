package it.gov.pagopa.receipt.pdf.service.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Model class for Blob Storage client's response
 */
@Getter
@Setter
@NoArgsConstructor
public class BlobStorageResponse {

    String documentUrl;
    String documentName;
    int statusCode;
}
