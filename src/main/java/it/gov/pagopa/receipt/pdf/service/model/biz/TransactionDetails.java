package it.gov.pagopa.receipt.pdf.service.model.biz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class TransactionDetails {
    private Transaction transaction;
}
