package it.gov.pagopa.receipt.pdf.service.model.cart;

import it.gov.pagopa.receipt.pdf.service.model.receipt.ReceiptErrorStatusType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartReceiptError {

    private String id;
    private String messagePayload;
    private String messageError;
    private ReceiptErrorStatusType status;
}
