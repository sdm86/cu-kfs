package edu.cornell.kfs.module.purap.document.authorization;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.kuali.kfs.module.purap.PurapAuthorizationConstants.PaymentRequestEditMode;
import org.kuali.kfs.module.purap.PurapConstants;
import org.kuali.kfs.module.purap.PurapConstants.PaymentRequestStatuses;
import org.kuali.kfs.module.purap.document.PaymentRequestDocument;
import org.kuali.kfs.module.purap.document.authorization.PaymentRequestDocumentPresentationController;
import org.kuali.kfs.sys.KfsAuthorizationConstants;
import org.kuali.rice.krad.document.Document;

import edu.cornell.kfs.module.purap.CUPurapAuthorizationConstants.CUPaymentRequestEditMode;
import edu.cornell.kfs.module.purap.CUPurapConstants;
import edu.cornell.kfs.module.purap.CUPurapConstants.CUPaymentRequestStatuses;
import edu.cornell.kfs.module.purap.CUPurapWorkflowConstants.PaymentRequestDocument.NodeDetailEnum;

public class CuPaymentRequestDocumentPresentationController extends PaymentRequestDocumentPresentationController {
	
	@Override
	public Set<String> getEditModes(Document document) {
		Set<String> editModes = super.getEditModes(document);
		
		PaymentRequestDocument paymentRequestDocument = (PaymentRequestDocument)document;
		
	      // KFSPTS-1891
		editModes.add(KfsAuthorizationConstants.DisbursementVoucherEditMode.FRN_ENTRY);
		editModes.add(KfsAuthorizationConstants.DisbursementVoucherEditMode.WIRE_ENTRY);
		
        // KFSPTS-1891
		if (canApprove(paymentRequestDocument) && canEditAmount(paymentRequestDocument)) {
			editModes.add(CUPaymentRequestEditMode.EDIT_AMOUNT);
		}	
		
		if (paymentRequestDocument.isDocumentStoppedInRouteNode(PaymentRequestStatuses.APPDOC_PAYMENT_METHOD_REVIEW)) {
			editModes.add(CUPaymentRequestEditMode.WAIVE_WIRE_FEE_EDITABLE);
		}
		if(editModes.contains(PaymentRequestEditMode.TAX_INFO_VIEWABLE)){
			editModes.remove(PaymentRequestEditMode.TAX_INFO_VIEWABLE);
		}
        // KFSPTS-2712 : allow payment method review to view tax info
		if ((PaymentRequestStatuses.APPDOC_DEPARTMENT_APPROVED.equals(paymentRequestDocument.getStatusCode()) || PaymentRequestStatuses.APPDOC_PAYMENT_METHOD_REVIEW.equals(paymentRequestDocument.getStatusCode()))&&
               // if and only if the preq has gone through tax review would TaxClassificationCode be non-empty
				!StringUtils.isEmpty(paymentRequestDocument.getTaxClassificationCode())) {
			editModes.add(PaymentRequestEditMode.TAX_INFO_VIEWABLE);
		}
		
		return editModes;
	}
	
    // KFSPTS-1891
	private boolean canEditAmount(PaymentRequestDocument paymentRequestDocument) {
		return  PaymentRequestStatuses.APPDOC_PAYMENT_METHOD_REVIEW.contains(paymentRequestDocument.getStatusCode());
	}

}
