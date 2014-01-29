package edu.cornell.kfs.fp.document.authorization;

import org.kuali.kfs.sys.businessobject.FinancialSystemDocumentHeader;
import org.kuali.kfs.sys.document.authorization.AccountingDocumentPresentationControllerBase;
import org.kuali.rice.kns.document.Document;
import org.kuali.rice.kns.workflow.service.KualiWorkflowDocument;

public class AdvanceDepositDocumentPresentationController extends AccountingDocumentPresentationControllerBase {
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(AdvanceDepositDocumentPresentationController.class);
    
    /**
     * @see org.kuali.rice.kns.document.authorization.DocumentPresentationControllerBase#canEdit(org.kuali.rice.kns.document.Document)
     */
    @Override
    protected boolean canEdit(Document document) {
        KualiWorkflowDocument workflowDocument = document.getDocumentHeader().getWorkflowDocument();
        FinancialSystemDocumentHeader documentheader = (FinancialSystemDocumentHeader) (document.getDocumentHeader());

        if (workflowDocument.stateIsCanceled() || documentheader.getFinancialDocumentInErrorNumber() != null) {
            return false;
        }
//        else if (workflowDocument.stateIsEnroute()) {
//            List<String> currentRouteLevels = getCurrentRouteLevels(workflowDocument);
//
//            if (currentRouteLevels.contains(RouteLevelNames.ACCOUNTING_ORGANIZATION_HIERARCHY)) {
//                return false;
//            }
//        }

        return super.canEdit(document);
    }

}
