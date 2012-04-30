/*
 * Copyright 2007 The Kuali Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rsmart.kuali.kfs.cr.document.validation.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.kuali.kfs.sys.KFSKeyConstants;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.rice.kns.document.MaintenanceDocument;
import org.kuali.rice.kns.maintenance.rules.MaintenanceDocumentRuleBase;
import org.kuali.rice.kns.service.BusinessObjectService;

import com.rsmart.kuali.kfs.cr.businessobject.CheckReconciliation;


/**
 * Business rule(s) applicable to Check Reconciliation documents.
 */
public class CheckReconciliationRule extends MaintenanceDocumentRuleBase {

    protected static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(CheckReconciliationRule.class);

    private CheckReconciliation oldCheckReconciliation;
    
    private CheckReconciliation newCheckReconciliation;

    public CheckReconciliationRule() {
    }

    /**
     * This method sets the convenience objects like newAccount and oldAccount, so you have short and easy handles to the new and
     * old objects contained in the maintenance document. It also calls the BusinessObjectBase.refresh(), which will attempt to load
     * all sub-objects from the DB by their primary keys, if available.
     * 
     * @param document - the maintenanceDocument being evaluated
     */
    public void setupConvenienceObjects() {

        // setup oldAccountingPeriod convenience objects, make sure all possible sub-objects are populated
        oldCheckReconciliation = (CheckReconciliation) super.getOldBo();

        // setup newAccountingPeriod convenience objects, make sure all possible sub-objects are populated
        newCheckReconciliation = (CheckReconciliation) super.getNewBo();
    }

    /**
     * This method checks the following rules: calls processCustomRouteDocumentBusinessRules but does not fail if any of them fail
     * (this only happens on routing)
     * 
     * @see org.kuali.core.maintenance.rules.MaintenanceDocumentRuleBase#processCustomSaveDocumentBusinessRules(org.kuali.core.document.MaintenanceDocument)
     */
    protected boolean processCustomSaveDocumentBusinessRules(MaintenanceDocument document) {

        LOG.info("processCustomSaveDocumentBusinessRules called");
        // call the route rules to report all of the messages, but ignore the result
        processCustomRouteDocumentBusinessRules(document);

        // Save always succeeds, even if there are business rule failures
        return true;
    }

    /**
     * 
     * @see org.kuali.core.maintenance.rules.MaintenanceDocumentRuleBase#processCustomRouteDocumentBusinessRules(org.kuali.core.document.MaintenanceDocument)
     */
    protected boolean processCustomRouteDocumentBusinessRules(MaintenanceDocument document) {
        LOG.info("processCustomRouteDocumentBusinessRules called");
        setupConvenienceObjects();

        boolean valid = true;
        
        if( newCheckReconciliation.getAmount() == null ) {
            putFieldError("amount", KFSKeyConstants.ERROR_ZERO_AMOUNT);
            valid = false;            
        }
        else if( newCheckReconciliation.getAmount().isZero() ) {
            putFieldError("amount", KFSKeyConstants.ERROR_ZERO_AMOUNT);
            valid = false;
        }
        else if( newCheckReconciliation.getAmount().isNegative() ) {
            putFieldError("amount", KFSKeyConstants.ERROR_NEGATIVE_AMOUNT);
            valid = false;
        }
        
        BusinessObjectService businessObjectService = SpringContext.getBean(BusinessObjectService.class);
        
        Map<Object,Object> fieldValues = new HashMap<Object,Object>();
        fieldValues.put("checkNumber", newCheckReconciliation.getCheckNumber());
        fieldValues.put("bankAccountNumber", newCheckReconciliation.getBankAccountNumber());
        
        Collection<CheckReconciliation> checks = businessObjectService.findMatching(CheckReconciliation.class, fieldValues);
        
        boolean isEdit = document.isEdit();
        
        if( checks.size() > 0 && !isEdit ) {
            valid = false;
            putFieldError("checkNumber", "error.checkexists");
        }
        
        return valid;
    }

}