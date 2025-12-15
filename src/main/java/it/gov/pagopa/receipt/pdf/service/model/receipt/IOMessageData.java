package it.gov.pagopa.receipt.pdf.service.model.receipt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IOMessageData {
  private String idMessageDebtor;
  private String idMessagePayer;
}
