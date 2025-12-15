package it.gov.pagopa.receipt.pdf.service.model.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageData {

  private String id;
  private String subject;
  private String markdown;
}
