package it.gov.pagopa.receipt.pdf.service.model;

import lombok.*;

import java.io.InputStream;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptPdfResponse {
    String attachmentName;
    InputStream pdfFile;
}
