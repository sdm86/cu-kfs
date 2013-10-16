package edu.cornell.kfs.module.purap.document.validation.impl;

import org.apache.commons.lang.StringUtils;
import org.kuali.kfs.module.purap.businessobject.PaymentRequestWireTransfer;
import org.kuali.kfs.module.purap.document.PaymentRequestDocument;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.kfs.sys.KFSKeyConstants;
import org.kuali.kfs.sys.KFSPropertyConstants;
import org.kuali.kfs.sys.document.AccountingDocument;
import org.kuali.kfs.sys.document.validation.GenericValidation;
import org.kuali.kfs.sys.document.validation.event.AttributedDocumentEvent;
import org.kuali.rice.kns.util.GlobalVariables;
import org.kuali.rice.kns.util.MessageMap;

import edu.cornell.kfs.fp.businessobject.PaymentMethod;
import edu.cornell.kfs.module.purap.CUPurapPropertyConstants;

public class PaymentRequestWireTransferValidation extends GenericValidation  {
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(PaymentRequestWireTransferValidation.class);

    private AccountingDocument accountingDocumentForValidation;

    /**
     * @see org.kuali.kfs.sys.document.validation.Validation#validate(org.kuali.kfs.sys.document.validation.event.AttributedDocumentEvent)
     */
    public boolean validate(AttributedDocumentEvent event) {
        LOG.debug("validate start");
        boolean isValid = true;
        
        PaymentRequestDocument document = (PaymentRequestDocument) accountingDocumentForValidation;
//        DisbursementVoucherPayeeDetail payeeDetail = document.getDvPayeeDetail();
        PaymentRequestWireTransfer wireTransfer = document.getPreqWireTransfer();

        if (!PaymentMethod.PM_CODE_WIRE.equals(document.getPaymentMethodCode())) {
            return isValid;
        }

        MessageMap errors = GlobalVariables.getMessageMap(); 
        errors.addToErrorPath(KFSPropertyConstants.DOCUMENT);
        errors.addToErrorPath(CUPurapPropertyConstants.PREQ_WIRE_TRANSFER);


//        SpringContext.getBean(DictionaryValidationService.class).validateBusinessObject(wireTransfer);
        isValid &= isValid(wireTransfer.getPreqBankName(), "Bank Name", "preqBankName");
        isValid &= isValid(wireTransfer.getPreqBankCityName(), "Bank City", "preqBankCityName");
        isValid &= isValid(wireTransfer.getPreqBankCountryCode(), "Bank Country", "preqBankCountryCode");
        isValid &= isValid(wireTransfer.getPreqCurrencyTypeName(), "Currency", "preqCurrencyTypeName");
        isValid &= isValid(wireTransfer.getPreqPayeeAccountNumber(), "Bank Account#", "preqPayeeAccountNumber");
        isValid &= isValid(wireTransfer.getPreqPayeeAccountName(), "Bank Account Name", "preqPayeeAccountName");

        if (KFSConstants.COUNTRY_CODE_UNITED_STATES.equals(wireTransfer.getPreqBankCountryCode()) && StringUtils.isBlank(wireTransfer.getPreqBankRoutingNumber())) {
            errors.putError(CUPurapPropertyConstants.PREQ_BANK_ROUTING_NUMBER, KFSKeyConstants.ERROR_DV_BANK_ROUTING_NUMBER);
            isValid = false;
        }

        if (KFSConstants.COUNTRY_CODE_UNITED_STATES.equals(wireTransfer.getPreqBankCountryCode()) && StringUtils.isBlank(wireTransfer.getPreqBankStateCode())) {
            errors.putError(CUPurapPropertyConstants.PREQ_BANK_STATE_CODE, KFSKeyConstants.ERROR_REQUIRED, "Bank State");
            isValid = false;
        }

        /* cannot have attachment checked for wire transfer */
//        if (document.isDisbVchrAttachmentCode()) {
//            errors.putErrorWithoutFullErrorPath(KFSPropertyConstants.DOCUMENT + "." + KFSPropertyConstants.DISB_VCHR_ATTACHMENT_CODE, KFSKeyConstants.ERROR_DV_WIRE_ATTACHMENT);
//            isValid = false;
//        }

        errors.removeFromErrorPath(CUPurapPropertyConstants.PREQ_WIRE_TRANSFER);
        errors.removeFromErrorPath(KFSPropertyConstants.DOCUMENT);
        
        return isValid;
    }

    private boolean isValid(String field, String label, String errorPropertyName) {

        // make sure it exists
        if (StringUtils.isBlank(field)) {
            GlobalVariables.getMessageMap().putError(errorPropertyName, KFSKeyConstants.ERROR_REQUIRED, label);
            return false;
        }
        return true;
    }
    /**
     * Gets the accountingDocumentForValidation attribute. 
     * @return Returns the accountingDocumentForValidation.
     */
    public AccountingDocument getAccountingDocumentForValidation() {
        return accountingDocumentForValidation;
    }

    /**
     * Sets the accountingDocumentForValidation attribute value.
     * 
     * @param accountingDocumentForValidation The accountingDocumentForValidation to set.
     */
    public void setAccountingDocumentForValidation(AccountingDocument accountingDocumentForValidation) {
        this.accountingDocumentForValidation = accountingDocumentForValidation;
    }

}
