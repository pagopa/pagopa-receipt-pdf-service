package it.gov.pagopa.receipt.pdf.service.enumeration;

public enum ReasonErrorCode {
    ERROR_QUEUE(902), ERROR_BLOB_STORAGE(901), ERROR_PDF_ENGINE(0);

    private int code;

    ReasonErrorCode(int code){
        this.code = code;
    }

    public int getCode(){
        return this.code;
    }

    public int getCustomCode(int customCode){
        return customCode;
    }
}
