package it.gov.pagopa.receipt.pdf.service.model.receipt;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CartItem {

  private String subject;
  private String payeeName;
}
