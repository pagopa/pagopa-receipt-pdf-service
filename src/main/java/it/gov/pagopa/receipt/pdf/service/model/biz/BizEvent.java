package it.gov.pagopa.receipt.pdf.service.model.biz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class BizEvent {
    private String id;
    private String version;
    private String idPaymentManager;
    private String complete;
    private String receiptId;
    private List<String> missingInfo;
    private DebtorPosition debtorPosition;
    private Creditor creditor;
    private Psp psp;
    private Debtor debtor;
    private Payer payer;
    private PaymentInfo paymentInfo;
    private List<Transfer> transferList;
    private TransactionDetails transactionDetails;
    private Long timestamp;
    private Map<String, Object> properties;

    // internal management field
    @Builder.Default
    private BizEventStatusType eventStatus = BizEventStatusType.NA;
    @Builder.Default
    private Integer eventRetryEnrichmentCount = 0;
    @Builder.Default
    private Boolean eventTriggeredBySchedule = Boolean.FALSE;
    private String eventErrorMessage;

    @Builder.Default
    private Boolean attemptedPoisonRetry = Boolean.FALSE;

}
