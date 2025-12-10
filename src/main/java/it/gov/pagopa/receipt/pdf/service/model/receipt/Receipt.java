package it.gov.pagopa.receipt.pdf.service.model.receipt;

import it.gov.pagopa.receipt.pdf.service.enumeration.ReceiptStatusType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model class for the receipt
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Receipt {

    private String eventId;
    private String id;
    private String version;
    private EventData eventData;
    private IOMessageData ioMessageData;
    private ReceiptStatusType status;
    private ReceiptMetadata mdAttach;
    private ReceiptMetadata mdAttachPayer;
    private int numRetry;
    private ReasonError reasonErr;
    private ReasonError reasonErrPayer;
    @SuppressWarnings("java:S116")
    private long inserted_at;
    @SuppressWarnings("java:S116")
    private long generated_at;
    @SuppressWarnings("java:S116")
    private long notified_at;
    private Boolean isCart;
}
