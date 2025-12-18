package it.gov.pagopa.receipt.pdf.service.model.biz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Psp {
    private String idPsp;
    private String idBrokerPsp;
    private String idChannel;
    private String psp;
    private String pspPartitaIVA;
    private String pspFiscalCode;
    private String channelDescription;
}
