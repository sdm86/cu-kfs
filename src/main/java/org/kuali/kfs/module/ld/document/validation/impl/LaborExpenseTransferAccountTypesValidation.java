package org.kuali.kfs.module.ld.document.validation.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.kuali.kfs.module.ld.LaborConstants;
import org.kuali.kfs.module.ld.LaborKeyConstants;
import org.kuali.kfs.module.ld.document.LaborExpenseTransferDocumentBase;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.kfs.sys.KFSParameterKeyConstants.LdParameterConstants;
import org.kuali.kfs.sys.KFSPropertyConstants;
import org.kuali.kfs.sys.businessobject.AccountingLine;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.kfs.sys.document.validation.GenericValidation;
import org.kuali.kfs.sys.document.validation.event.AttributedDocumentEvent;
import org.kuali.rice.kim.service.RoleManagementService;
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
     * Validates before the document saves and routes 
     * @see org.kuali.kfs.validation.Validation#validate(java.lang.Object[])
     */
	@SuppressWarnings("unchecked")
	public boolean validate(AttributedDocumentEvent event) {
        boolean isValid = true;
           
    	ParameterService parameterService = KNSServiceLocator.getParameterService();
        if (parameterService.getIndicatorParameter(LaborConstants.LABOR_MODULE_CODE, ParameterConstants.DOCUMENT_COMPONENT,LdParameterConstants.VALIDATE_TRANSFER_ACCOUNT_TYPES_IND)
        		&& !hasExceptionRoles()) {
			Document documentForValidation = getDocumentForValidation();

			LaborExpenseTransferDocumentBase expenseTransferDocument = (LaborExpenseTransferDocumentBase) documentForValidation;

			List<AccountingLine> sourceLines = (List<AccountingLine>)expenseTransferDocument.getSourceAccountingLines();
			List<AccountingLine> targetLines = (List<AccountingLine>)expenseTransferDocument.getTargetAccountingLines();
			List<String> invalidAccountTypes = parameterService.getParameterValues(LaborConstants.LABOR_MODULE_CODE,
							ParameterConstants.DOCUMENT_COMPONENT,LdParameterConstants.INVALID_TRANSFER_ACCOUNT_TYPES);

			if (CollectionUtils.isNotEmpty(sourceLines) && CollectionUtils.isNotEmpty(targetLines) && CollectionUtils.isNotEmpty(invalidAccountTypes) && invalidAccountTypes.size() > 1) {
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

        Set<String> invalidTransferSourceAccountTypes = getInvalidTransferAccountTypes(sourceLines, invalidAccountTypes);

        Set<String> invalidTransferTargetAccountTypes = new HashSet<String>();
        if (CollectionUtils.isNotEmpty(invalidTransferSourceAccountTypes)) {
        	invalidTransferTargetAccountTypes = getInvalidTransferAccountTypes(targetLines, invalidAccountTypes);
        }

        return !(sourceHasNoInvalidTransferAccountTypes(invalidTransferSourceAccountTypes) || targetHasNoInvalidTransferAccountTypes(invalidTransferTargetAccountTypes)
        		|| isSingleInvalidTransferAccountTypeMatched(invalidTransferSourceAccountTypes, invalidTransferTargetAccountTypes));
    }
      
	/*
	 * get the invalid transfer account types
	 */
	private Set<String> getInvalidTransferAccountTypes(List<AccountingLine> accountLines, List<String> invalidAccountTypes) {
        Set <String> invalidTransferAccountTypes = new HashSet<String>();
       for (AccountingLine line : accountLines) {
            if (invalidAccountTypes.contains(line.getAccount().getAccountTypeCode())) {
                invalidTransferAccountTypes.add(line.getAccount().getAccountTypeCode());
            }
        }
       return invalidTransferAccountTypes;
		
	}
	
	/*
	 * if source has not invalid transfer account types
	 */
	private boolean sourceHasNoInvalidTransferAccountTypes(Set<String> invalidTransferSourceAccountTypes) {
		return CollectionUtils.isEmpty(invalidTransferSourceAccountTypes);
	}
	
	/*
	 * if target has no invalid transfer account types
	 */
	private boolean targetHasNoInvalidTransferAccountTypes(Set<String> invalidTransferTargetAccountTypes) {
		return CollectionUtils.isEmpty(invalidTransferTargetAccountTypes);
	}
	
	/*
	 * the source and target has only one invalid transfer account type and the account type is the same
	 */
	private boolean isSingleInvalidTransferAccountTypeMatched(Set<String> invalidTransferSourceAccountTypes, Set<String> invalidTransferTargetAccountTypes) {
		return invalidTransferSourceAccountTypes.size() == 1  && invalidTransferSourceAccountTypes.equals(invalidTransferTargetAccountTypes);
	}
		
	/*
	 * Check if user has "KFS-SYS Contracts & Grants Processor", "KFS-LD Labor Distribution Manager (cu)" roles
	 * if user has any of these roles, then user can bypass this rule validation.
	 * 
	 */
	private boolean hasExceptionRoles() {		
        RoleManagementService roleService = SpringContext.getBean(RoleManagementService.class);
        
        List<String> roleIds = new ArrayList<String>();
        roleIds.add(roleService.getRoleIdByName(KFSConstants.ParameterNamespaces.KFS, KFSConstants.SysKimConstants.CONTRACTS_AND_GRANTS_PROJECT_DIRECTOR));
        roleIds.add(roleService.getRoleIdByName(LaborConstants.LABOR_MODULE_CODE, LaborConstants.LABOR_DISTRIBUTION_MANAGER));
        return roleService.principalHasRole(GlobalVariables.getUserSession().getPrincipalId(), roleIds, null);
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
