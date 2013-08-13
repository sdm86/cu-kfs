package edu.cornell.kfs.module.cg.document.validation.impl;

import org.kuali.kfs.module.cg.document.validation.impl.AwardRule;
import org.kuali.kfs.sys.KFSKeyConstants;
import org.kuali.rice.kns.document.MaintenanceDocument;

import edu.cornell.kfs.module.cg.businessobject.AwardExtendedAttribute;
import edu.cornell.kfs.sys.CUKFSKeyConstants;

import org.kuali.kfs.module.cg.businessobject.Award;

@SuppressWarnings("deprecation")
public class AwardExtensionRule extends AwardRule {

    @Override
    protected boolean processCustomRouteDocumentBusinessRules(MaintenanceDocument document) {
    	boolean success = true;
    	success &= super.processCustomRouteDocumentBusinessRules(document);
    	
    	success &= checkFinalFinancialReportRequired();
    	
    	return success;
    }
    
    
	protected boolean checkFinalFinancialReportRequired() {
    	boolean success = true;
    	

    	AwardExtendedAttribute awardExtendedAttributeNew = (AwardExtendedAttribute) ( (Award) super.getNewBo()).getExtension();
    	AwardExtendedAttribute awardExtendedAttributeOld = (AwardExtendedAttribute) ( (Award) super.getOldBo()).getExtension();

    	if (awardExtendedAttributeNew.isFinalFinancialReportRequired() && null==awardExtendedAttributeNew.getFinalFiscalReportDate()) {
    		success = false;
    		putFieldError("extension.finalFiscalReportDate", CUKFSKeyConstants.ERROR_FINAL_FINANCIAL_REPORT_DATE_REQUIRED);
    	}

    	if (awardExtendedAttributeOld.isFinalFinancialReportRequired() && null==awardExtendedAttributeOld.getFinalFiscalReportDate()) {
    		success = false;
    		putFieldError("extension.finalFiscalReportDate", CUKFSKeyConstants.ERROR_FINAL_FINANCIAL_REPORT_DATE_REQUIRED);
    	}

    	
    	return success;
    }

}
