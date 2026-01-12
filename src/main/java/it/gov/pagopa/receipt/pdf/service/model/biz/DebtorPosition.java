package it.gov.pagopa.receipt.pdf.service.model.biz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DebtorPosition {
    private String modelType;
    private String noticeNumber;
    private String iuv;
    private String iur;
}
