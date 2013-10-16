package edu.cornell.kfs.module.purap.document.validation.impl;

import org.apache.commons.lang.StringUtils;
import org.kuali.kfs.module.purap.document.VendorCreditMemoDocument;
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
import edu.cornell.kfs.module.purap.businessobject.CreditMemoWireTransfer;

public class CreditMemoWireTransferValidation extends GenericValidation  {
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(CreditMemoWireTransferValidation.class);

    private AccountingDocument accountingDocumentForValidation;

    /**
     * @see org.kuali.kfs.sys.document.validation.Validation#validate(org.kuali.kfs.sys.document.validation.event.AttributedDocumentEvent)
     */
    public boolean validate(AttributedDocumentEvent event) {
        LOG.debug("validate start");
        boolean isValid = true;
        
        VendorCreditMemoDocument document = (VendorCreditMemoDocument) accountingDocumentForValidation;
//        DisbursementVoucherPayeeDetail payeeDetail = document.getDvPayeeDetail();
        CreditMemoWireTransfer wireTransfer = document.getCmWireTransfer();

        if (!PaymentMethod.PM_CODE_WIRE.equals(document.getPaymentMethodCode())) {
            return isValid;
        }

        MessageMap errors = GlobalVariables.getMessageMap(); 
        errors.addToErrorPath(KFSPropertyConstants.DOCUMENT);
        errors.addToErrorPath(CUPurapPropertyConstants.CM_WIRE_TRANSFER);


//        SpringContext.getBean(DictionaryValidationService.class).validateBusinessObject(wireTransfer);
        isValid &= isValid(wireTransfer.getCmBankName(), "Bank Name", "cmBankName");
        isValid &= isValid(wireTransfer.getCmBankCityName(), "Bank City", "cmBankCityName");
        isValid &= isValid(wireTransfer.getCmBankCountryCode(), "Bank Country", "cmBankCountryCode");
        isValid &= isValid(wireTransfer.getCmCurrencyTypeName(), "Currency", "cmCurrencyTypeName");
        isValid &= isValid(wireTransfer.getCmPayeeAccountNumber(), "Bank Account#", "cmPayeeAccountNumber");
        isValid &= isValid(wireTransfer.getCmPayeeAccountName(), "Bank Account Name", "cmPayeeAccountName");

        if (KFSConstants.COUNTRY_CODE_UNITED_STATES.equals(wireTransfer.getCmBankCountryCode()) && StringUtils.isBlank(wireTransfer.getCmBankRoutingNumber())) {
            errors.putError(CUPurapPropertyConstants.CM_BANK_ROUTING_NUMBER, KFSKeyConstants.ERROR_DV_BANK_ROUTING_NUMBER);
            isValid = false;
        }

        if (KFSConstants.COUNTRY_CODE_UNITED_STATES.equals(wireTransfer.getCmBankCountryCode()) && StringUtils.isBlank(wireTransfer.getCmBankStateCode())) {
            errors.putError(CUPurapPropertyConstants.CM_BANK_STATE_CODE, KFSKeyConstants.ERROR_REQUIRED, "Bank State");
            isValid = false;
        }

        /* cannot have attachment checked for wire transfer */
//        if (document.isDisbVchrAttachmentCode()) {
//            errors.putErrorWithoutFullErrorPath(KFSPropertyConstants.DOCUMENT + "." + KFSPropertyConstants.DISB_VCHR_ATTACHMENT_CODE, KFSKeyConstants.ERROR_DV_WIRE_ATTACHMENT);
//            isValid = false;
//        }

        errors.removeFromErrorPath(CUPurapPropertyConstants.CM_WIRE_TRANSFER);
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
