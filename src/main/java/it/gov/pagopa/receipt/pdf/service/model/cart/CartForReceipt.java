package it.gov.pagopa.receipt.pdf.service.model.cart;

import it.gov.pagopa.receipt.pdf.service.model.receipt.ReasonError;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Data
public class CartForReceipt {

    private String eventId;
    private String id;
    private String version;
    private Payload payload;
    private CartStatusType status;
    private int numRetry;
    private int notificationNumRetry;
    private ReasonError reasonErr;
    private long inserted_at;
    private long generated_at;
    private long notified_at;
}
