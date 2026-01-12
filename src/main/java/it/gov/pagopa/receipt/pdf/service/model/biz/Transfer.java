package it.gov.pagopa.receipt.pdf.service.model.biz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transfer {
    private String idTransfer;
    private String fiscalCodePA;
    private String companyName;
    private String amount;
    private String transferCategory;
    private String remittanceInformation;
    //	@JsonProperty(value="IBAN") -
    private String IBAN;
    //	@JsonProperty(value="MBD") -
//	private MBD mbd; -
    private String MBDAttachment;
    private List<MapEntry> metadata;
}
