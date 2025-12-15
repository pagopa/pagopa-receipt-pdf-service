package it.gov.pagopa.receipt.pdf.service.model.receipt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReasonError {
  private int code;
  private String message;
}
