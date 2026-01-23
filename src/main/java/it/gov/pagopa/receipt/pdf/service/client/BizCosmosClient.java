package it.gov.pagopa.receipt.pdf.service.client;

import it.gov.pagopa.receipt.pdf.service.exception.BizEventNotFoundException;
import it.gov.pagopa.receipt.pdf.service.model.biz.BizEvent;

public interface BizCosmosClient {
    BizEvent getBizEventDocumentByOrganizationFiscalCodeAndIUV(String organizationFiscalCode, String iuv) throws BizEventNotFoundException;
}
