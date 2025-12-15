package it.gov.pagopa.receipt.pdf.service.model.cart;

import it.gov.pagopa.receipt.pdf.service.model.receipt.ReasonError;
import it.gov.pagopa.receipt.pdf.service.model.receipt.ReceiptMetadata;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Payload {
  private String payerFiscalCode;
  private String transactionCreationDate;
  private int totalNotice;
  private String totalAmount;
  private ReceiptMetadata mdAttachPayer;
  private MessageData messagePayer;
  private List<CartPayment> cart;
  private ReasonError reasonErrPayer;
}
