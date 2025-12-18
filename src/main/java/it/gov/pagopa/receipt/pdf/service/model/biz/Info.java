package it.gov.pagopa.receipt.pdf.service.model.biz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Info {
    private String type;
    private String blurredNumber;
    private String holder;
    private String expireMonth;
    private String expireYear;
    private String brand;
    private String issuerAbi;
    private String issuerName;
    private String label;
}
