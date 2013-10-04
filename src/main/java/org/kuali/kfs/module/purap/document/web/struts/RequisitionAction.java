/*
 * Copyright 2006 The Kuali Foundation
 * 
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.opensource.org/licenses/ecl2.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.kfs.module.purap.document.web.struts;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.kuali.kfs.module.purap.PurapKeyConstants;
import org.kuali.kfs.module.purap.businessobject.DefaultPrincipalAddress;
import org.kuali.kfs.module.purap.businessobject.RequisitionItem;
import org.kuali.kfs.module.purap.document.RequisitionDocument;
import org.kuali.kfs.module.purap.document.service.PurapService;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.rice.core.util.RiceConstants;
import org.kuali.rice.kew.exception.WorkflowException;
import org.kuali.rice.kns.service.BusinessObjectService;
import org.kuali.rice.kns.service.PersistenceService;
import org.kuali.rice.kns.util.GlobalVariables;
import org.kuali.rice.kns.util.ObjectUtils;
import org.kuali.rice.kns.web.struts.form.KualiDocumentFormBase;
import org.kuali.rice.kns.workflow.service.KualiWorkflowDocument;

import edu.cornell.kfs.module.purap.document.IWantDocument;
import edu.cornell.kfs.module.purap.document.service.IWantDocumentService;

/**
 * Struts Action for Requisition document.
 */
public class RequisitionAction extends PurchasingActionBase {
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(RequisitionAction.class);

    /**
     * Does initialization for a new requisition.
     * 
     * @see org.kuali.rice.kns.web.struts.action.KualiDocumentActionBase#createDocument(org.kuali.rice.kns.web.struts.form.KualiDocumentFormBase)
     */
    @Override
    protected void createDocument(KualiDocumentFormBase kualiDocumentFormBase) throws WorkflowException {
        super.createDocument(kualiDocumentFormBase);
        ((RequisitionDocument) kualiDocumentFormBase.getDocument()).initiateDocument();
    }

    public ActionForward setAsDefaultBuilding(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        RequisitionDocument req = (RequisitionDocument) ((RequisitionForm) form).getDocument();
        
        if (ObjectUtils.isNotNull(req.getDeliveryCampusCode()) && ObjectUtils.isNotNull(req.getDeliveryBuildingCode())) {
            DefaultPrincipalAddress defaultPrincipalAddress = new DefaultPrincipalAddress(GlobalVariables.getUserSession().getPerson().getPrincipalId());
            Map addressKeys = SpringContext.getBean(PersistenceService.class).getPrimaryKeyFieldValues(defaultPrincipalAddress);
            defaultPrincipalAddress = (DefaultPrincipalAddress) SpringContext.getBean(BusinessObjectService.class).findByPrimaryKey(DefaultPrincipalAddress.class, addressKeys);

            if (ObjectUtils.isNull(defaultPrincipalAddress)) {
                defaultPrincipalAddress = new DefaultPrincipalAddress(GlobalVariables.getUserSession().getPerson().getPrincipalId());
            }
            
            defaultPrincipalAddress.setDefaultBuilding(req.getDeliveryCampusCode(), req.getDeliveryBuildingCode(), req.getDeliveryBuildingRoomNumber());
            SpringContext.getBean(BusinessObjectService.class).save(defaultPrincipalAddress);
            GlobalVariables.getMessageList().add(PurapKeyConstants.DEFAULT_BUILDING_SAVED);
        }
        
        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    /**
     * @see org.kuali.rice.kns.web.struts.action.KualiAction#refresh(org.apache.struts.action.ActionMapping,
     *      org.apache.struts.action.ActionForm, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public ActionForward refresh(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ActionForward forward = super.refresh(mapping, form, request, response);
        RequisitionForm rqForm = (RequisitionForm) form;
        RequisitionDocument document = (RequisitionDocument) rqForm.getDocument();

        // super.refresh() must occur before this line to get the correct APO limit
        document.setOrganizationAutomaticPurchaseOrderLimit(SpringContext.getBean(PurapService.class).getApoLimit(document.getVendorContractGeneratedIdentifier(), document.getChartOfAccountsCode(), document.getOrganizationCode()));

        return forward;
    }
    
    /**
     * Adds a PurchasingItemCapitalAsset (a container for the Capital Asset Number) to the selected 
     * item's list.
     * 
     * @param mapping       An ActionMapping
     * @param form          The Form
     * @param request       An HttpServletRequest
     * @param response      The HttpServletResponse
     * @return      An ActionForward
     * @throws Exception
     */
    public ActionForward addAsset(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        RequisitionForm rqForm = (RequisitionForm) form;
        RequisitionDocument document = (RequisitionDocument) rqForm.getDocument();
        RequisitionItem item = (RequisitionItem)document.getItemByLineNumber(getSelectedLine(request) + 1);
        //TODO: Add a new way to add assets to the system.
        //item.addAsset();
        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    public ActionForward displayB2BRequisition(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        RequisitionForm reqForm = (RequisitionForm) form;
        reqForm.setDocId((String) request.getSession().getAttribute("docId"));
        loadDocument(reqForm);
        String multipleB2BReqs = (String) request.getSession().getAttribute("multipleB2BRequisitions");
        if (StringUtils.isNotEmpty(multipleB2BReqs)) {
            GlobalVariables.getMessageList().add(PurapKeyConstants.B2B_MULTIPLE_REQUISITIONS);
        }
        request.getSession().removeAttribute("docId");
        request.getSession().removeAttribute("multipleB2BRequisitions");
        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    /**
     * Clears the vendor selection from the Requisition.  NOTE, this functionality is only available on Requisition and not PO.
     * 
     * @param mapping An ActionMapping
     * @param form An ActionForm
     * @param request A HttpServletRequest
     * @param response A HttpServletResponse
     * @return An ActionForward
     * @throws Exception
     */
    public ActionForward clearVendor(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        PurchasingFormBase baseForm = (PurchasingFormBase) form;
        RequisitionDocument document = (RequisitionDocument) baseForm.getDocument();

        document.setVendorHeaderGeneratedIdentifier(null);
        document.setVendorDetailAssignedIdentifier(null);
        document.setVendorDetail(null);
        document.setVendorName("");
        document.setVendorLine1Address("");
        document.setVendorLine2Address("");
        document.setVendorAddressInternationalProvinceName("");
        document.setVendorCityName("");
        document.setVendorStateCode("");
        document.setVendorPostalCode("");
        document.setVendorCountryCode("");
        document.setVendorContractGeneratedIdentifier(null);
        document.setVendorContract(null);
        document.setVendorFaxNumber("");
        document.setVendorCustomerNumber("");
        document.setVendorAttentionName("");
        document.setVendorAddressGeneratedIdentifier(null);  //clearing value that was set in PurchasingDocumentBase.templateVendorAction
        return mapping.findForward(KFSConstants.MAPPING_BASIC);
    }

    /**
     * Set up blanket approve indicator which will be used to decide if need to run accounting line validation at the time of
     * blanket approve.
     * 
     * @see org.kuali.kfs.module.purap.document.web.struts.PurchasingActionBase#blanketApprove(org.apache.struts.action.ActionMapping,
     *      org.apache.struts.action.ActionForm, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public ActionForward blanketApprove(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        RequisitionDocument document = (RequisitionDocument) ((PurchasingFormBase) form).getDocument();
        document.setBlanketApproveRequest(true);
        return super.blanketApprove(mapping, form, request, response);
    }
 
    
    /**
     * Overridden to guarantee that form of copied document is set to whatever the entry mode of the document is
     * @see org.kuali.rice.kns.web.struts.action.KualiTransactionalDocumentActionBase#copy(org.apache.struts.action.ActionMapping, org.apache.struts.action.ActionForm, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public ActionForward copy(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
    	ActionForward forward = null;
    	
    	if(request.getParameter("docId") == null){
    	    forward = super.copy(mapping, form, request, response);
    	}
    	else{
			// this is copy document from Procurement Gateway:
		    // use this url to call: http://localhost:8080/kfs-dev/purapRequisition.do?methodToCall=copy&docId=xxxx
	       String docId = request.getParameter("docId");
	       KualiDocumentFormBase kualiDocumentFormBase = (KualiDocumentFormBase) form;
    	       
	       RequisitionDocument document = null;
	       document = (RequisitionDocument)getDocumentService().getByDocumentHeaderId(docId);
	       document.toCopyFromGateway();
   	       
	       kualiDocumentFormBase.setDocument(document);
	       KualiWorkflowDocument workflowDocument = document.getDocumentHeader().getWorkflowDocument();
	       kualiDocumentFormBase.setDocTypeName(workflowDocument.getDocumentType());
	       GlobalVariables.getUserSession().setWorkflowDocument(workflowDocument);
    	        
	       forward = mapping.findForward(RiceConstants.MAPPING_BASIC);	
    	    
    	}
        return forward;
    }
    
    /**
     * Creates a requisition document based on information on an I Want document.
     * 
     * @param mapping
     * @param form
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    public ActionForward createReqFromIWantDoc(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String iWantDocumentNumber = request.getParameter("docId");
        RequisitionForm requisitionForm = (RequisitionForm) form;

        IWantDocument iWantDocument = (IWantDocument) getDocumentService().getByDocumentHeaderId(iWantDocumentNumber);
        
        // Do not allow the req to be created if the IWNT doc is already associated with another req.
        if (iWantDocument != null && StringUtils.isNotBlank(iWantDocument.getReqsDocId())) {
        	throw new WorkflowException("Cannot create requisition from IWantDocument '" + iWantDocumentNumber +
        			"' because a requisition has already been created from that document");
        }
        
        IWantDocumentService iWantDocumentService = SpringContext.getBean(IWantDocumentService.class);

        createDocument(requisitionForm);
        
        RequisitionDocument requisitionDocument = requisitionForm.getRequisitionDocument();

        iWantDocumentService.setUpRequisitionDetailsFromIWantDoc(iWantDocument, requisitionDocument, requisitionForm);
        
        // Set the requisition doc ID reference on the IWantDocument.
        iWantDocument.setReqsDocId(requisitionDocument.getDocumentNumber());
        SpringContext.getBean(PurapService.class).saveDocumentNoValidation(iWantDocument);
        
      //  doDistribution(mapping, requisitionForm, request, response);
        
        //save(mapping, requisitionForm, request, response);

        return mapping.findForward(RiceConstants.MAPPING_BASIC);
    }

}