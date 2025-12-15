package it.gov.pagopa.receipt.pdf.service.model.receipt;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventData {
  private String payerFiscalCode;
  private String debtorFiscalCode;
  private String transactionCreationDate;
  private String amount;
  private List<CartItem> cart;
}
