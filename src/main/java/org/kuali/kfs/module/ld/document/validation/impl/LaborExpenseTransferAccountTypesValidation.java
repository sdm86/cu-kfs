package org.kuali.kfs.module.ld.document.validation.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.kuali.kfs.module.ld.LaborConstants;
import org.kuali.kfs.module.ld.LaborKeyConstants;
import org.kuali.kfs.module.ld.document.LaborExpenseTransferDocumentBase;
import org.kuali.kfs.sys.KFSParameterKeyConstants.LdParameterConstants;
import org.kuali.kfs.sys.KFSPropertyConstants;
import org.kuali.kfs.sys.businessobject.AccountingLine;
import org.kuali.kfs.sys.document.validation.GenericValidation;
import org.kuali.kfs.sys.document.validation.event.AttributedDocumentEvent;
import org.kuali.rice.kns.document.Document;
import org.kuali.rice.kns.service.KNSServiceLocator;
import org.kuali.rice.kns.service.ParameterConstants;
import org.kuali.rice.kns.service.ParameterService;
import org.kuali.rice.kns.util.GlobalVariables;

/**
 * 
 * This class is to validate if expense transfer is valid between account types.
 *
 */
public class LaborExpenseTransferAccountTypesValidation extends GenericValidation {
    private Document documentForValidation;
    
    /**
     * Validates before the document routes 
     * @see org.kuali.kfs.validation.Validation#validate(java.lang.Object[])
     */
	@SuppressWarnings("unchecked")
	public boolean validate(AttributedDocumentEvent event) {
        boolean isValid = true;
           
    	ParameterService parameterService = KNSServiceLocator.getParameterService();
        if (parameterService.getIndicatorParameter(LaborConstants.LABOR_MODULE_CODE, ParameterConstants.DOCUMENT_COMPONENT,LdParameterConstants.VALIDATE_TRANSFER_ACCOUNT_TYPES_IND)) {
			Document documentForValidation = getDocumentForValidation();

			LaborExpenseTransferDocumentBase expenseTransferDocument = (LaborExpenseTransferDocumentBase) documentForValidation;

			List<AccountingLine> sourceLines = (List<AccountingLine>)expenseTransferDocument.getSourceAccountingLines();
			List<AccountingLine> targetLines = (List<AccountingLine>)expenseTransferDocument.getTargetAccountingLines();
			List<String> invalidAccountTypes = parameterService.getParameterValues(LaborConstants.LABOR_MODULE_CODE,
							ParameterConstants.DOCUMENT_COMPONENT,LdParameterConstants.INVALID_TRANSFER_ACCOUNT_TYPES);

			if (CollectionUtils.isNotEmpty(sourceLines) && CollectionUtils.isNotEmpty(targetLines)) {
		    	if (isInvalidTransferBetweenAccountTypes(sourceLines, targetLines, invalidAccountTypes)) {
				    GlobalVariables.getMessageMap().putError(KFSPropertyConstants.SOURCE_ACCOUNTING_LINES, LaborKeyConstants.INVALID_ACCOUNTTRANSFER_ERROR);
				    isValid = false;
			    }
			}
        }

        return isValid;       
    }

    /**
     * This method checks if the account transfer is invalid between account types.
     * 
     * @param sourceLines
     * @param targetLines
     * @return
     */
	private boolean isInvalidTransferBetweenAccountTypes(List<AccountingLine> sourceLines, List<AccountingLine> targetLines, List<String> invalidAccountTypes) {

        List<String> sourceAccountTypes = new ArrayList<String>();

        boolean isInvalid = false;
        for (AccountingLine line : sourceLines) {
            if (invalidAccountTypes.contains(line.getAccount().getAccountTypeCode()) && !sourceAccountTypes.contains(line.getAccount().getAccountTypeCode())) {
                sourceAccountTypes.add(line.getAccount().getAccountTypeCode());
            }
        }

        for (AccountingLine line : (List<AccountingLine>)targetLines) {
            if (invalidAccountTypes.contains(line.getAccount().getAccountTypeCode()) && isSourceAccountTypeDifferent(sourceAccountTypes, line.getAccount().getAccountTypeCode())) {
            	isInvalid = true;
            	break;
            }
        }

        return isInvalid;
    }
       
	/*
	 * check if there is different account types found in source account types list
	 */
	private boolean isSourceAccountTypeDifferent (List<String> sourceAccountTypes, String targetAccountTypes) {
		boolean isDifferent = false;
		for (String sourceAccountType : sourceAccountTypes) {
			if (!StringUtils.equals(sourceAccountType, targetAccountTypes)) {
				isDifferent = true;
				break;
			}
		}
		return isDifferent;
	}
	
    /**
     * Gets the documentForValidation attribute. 
     * @return Returns the documentForValidation.
     */
    public Document getDocumentForValidation() {
        return documentForValidation;
    }

    /**
     * Sets the accountingDocumentForValidation attribute value.
     * @param documentForValidation The documentForValidation to set.
     */
    public void setDocumentForValidation(Document documentForValidation) {
        this.documentForValidation = documentForValidation;
    }    

}
