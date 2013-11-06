package edu.cornell.kfs.pdp.batch.service.impl;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.kuali.kfs.fp.batch.service.DisbursementVoucherExtractService;
import org.kuali.kfs.fp.document.DisbursementVoucherConstants;
import org.kuali.kfs.fp.document.DisbursementVoucherDocument;
import org.kuali.kfs.integration.purap.PurchasingAccountsPayableModuleService;
import org.kuali.kfs.pdp.PdpConstants;
import org.kuali.kfs.pdp.batch.service.impl.ProcessPdpCancelPaidServiceImpl;
import org.kuali.kfs.pdp.businessobject.PaymentDetail;
import org.kuali.kfs.pdp.service.PaymentDetailService;
import org.kuali.kfs.pdp.service.PaymentGroupService;
import org.kuali.kfs.sys.KFSParameterKeyConstants;
import org.kuali.kfs.sys.service.impl.KfsParameterConstants;
import org.kuali.rice.kns.service.DateTimeService;
import org.kuali.rice.kns.service.ParameterService;

import edu.cornell.kfs.integration.purap.CuPurchasingAccountsPayableModuleService;
import edu.cornell.kfs.pdp.businessobject.PaymentDetailExtendedAttribute;

public class CuProcessPdpCancelPaidServiceImpl extends ProcessPdpCancelPaidServiceImpl {
    
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(CuProcessPdpCancelPaidServiceImpl.class);
    
    protected PaymentGroupService paymentGroupService;
    protected PaymentDetailService paymentDetailService;
    protected ParameterService parameterService;
    protected DateTimeService dateTimeService;
    protected CuPurchasingAccountsPayableModuleService purchasingAccountsPayableModuleService;
    protected DisbursementVoucherExtractService dvExtractService;
    
    
    /**
     * @see org.kuali.kfs.module.purap.service.ProcessPdpCancelPaidService#processPdpCancels()
     */
    public void processPdpCancels() {
        LOG.debug("processPdpCancels() started");

        Date processDate = dateTimeService.getCurrentSqlDate();

        String organization = parameterService.getParameterValue(KfsParameterConstants.PURCHASING_BATCH.class, KFSParameterKeyConstants.PurapPdpParameterConstants.PURAP_PDP_ORG_CODE);
        String purapSubUnit = parameterService.getParameterValue(KfsParameterConstants.PURCHASING_BATCH.class, KFSParameterKeyConstants.PurapPdpParameterConstants.PURAP_PDP_SUB_UNIT_CODE);
        String dvSubUnit = parameterService.getParameterValue(DisbursementVoucherDocument.class, DisbursementVoucherConstants.DvPdpExtractGroup.DV_PDP_SBUNT_CODE);

        List<String> subUnits = new ArrayList<String>();
        subUnits.add(purapSubUnit);
        subUnits.add(dvSubUnit);

        Iterator<PaymentDetail> details = paymentDetailService.getUnprocessedCancelledDetails(organization, subUnits);
        while (details.hasNext()) {
            PaymentDetail paymentDetail = details.next();

            String documentTypeCode = paymentDetail.getFinancialDocumentTypeCode();
            String documentNumber = paymentDetail.getCustPaymentDocNbr();

            boolean primaryCancel = paymentDetail.getPrimaryCancelledPayment();
            boolean disbursedPayment = PdpConstants.PaymentStatusCodes.CANCEL_PAYMENT.equals(paymentDetail.getPaymentGroup().getPaymentStatusCode());
            //KFSPTS-2719
            boolean crCancel = ((PaymentDetailExtendedAttribute)paymentDetail.getExtension()).getCrCancelledPayment();

            if(purchasingAccountsPayableModuleService.isPurchasingBatchDocument(documentTypeCode)) {
                purchasingAccountsPayableModuleService.handlePurchasingBatchCancels(documentNumber, documentTypeCode, primaryCancel, disbursedPayment, crCancel);
            }
            else if (DisbursementVoucherConstants.DOCUMENT_TYPE_CHECKACH.equals(documentTypeCode)) {
                DisbursementVoucherDocument dv = dvExtractService.getDocumentById(documentNumber);
                if (dv != null) {
                    if (disbursedPayment || primaryCancel || crCancel) {
                        if (!crCancel) {
                            dvExtractService.cancelExtractedDisbursementVoucher(dv, processDate);
                        }
                    } else {
                        dvExtractService.resetExtractedDisbursementVoucher(dv, processDate);
                    }
                }
            }
            else {
                LOG.warn("processPdpCancels() Unknown document type (" + documentTypeCode + ") for document ID: " + documentNumber);
                continue;
            }

            paymentGroupService.processCancelledGroup(paymentDetail.getPaymentGroup(), processDate);
        }
    }


    /**
     * Gets the paymentGroupService.
     * 
     * @return paymentGroupService
     */
    public PaymentGroupService getPaymentGroupService() {
        return paymentGroupService;
    }


    /**
     * Sets the paymentGroupService
     * 
     * @see org.kuali.kfs.pdp.batch.service.impl.ProcessPdpCancelPaidServiceImpl#setPaymentGroupService(org.kuali.kfs.pdp.service.PaymentGroupService)
     */
    public void setPaymentGroupService(PaymentGroupService paymentGroupService) {
        this.paymentGroupService = paymentGroupService;
    }


    /**
     * Gets the paymentDetailService.
     * 
     * @return paymentDetailService
     */
    public PaymentDetailService getPaymentDetailService() {
        return paymentDetailService;
    }


    /**
     * Sets the paymentDetailService.
     * 
     * @see org.kuali.kfs.pdp.batch.service.impl.ProcessPdpCancelPaidServiceImpl#setPaymentDetailService(org.kuali.kfs.pdp.service.PaymentDetailService)
     */
    public void setPaymentDetailService(PaymentDetailService paymentDetailService) {
        this.paymentDetailService = paymentDetailService;
    }


    /**
     * Gets the parameterService.
     * 
     * @return parameterService
     */
    public ParameterService getParameterService() {
        return parameterService;
    }


    /**
     * Sets the parameterService.
     * 
     * @see org.kuali.kfs.pdp.batch.service.impl.ProcessPdpCancelPaidServiceImpl#setParameterService(org.kuali.rice.kns.service.ParameterService)
     */
    public void setParameterService(ParameterService parameterService) {
        this.parameterService = parameterService;
    }


    /**
     * Gets the dateTimeService.
     * 
     * @return dateTimeService
     */
    public DateTimeService getDateTimeService() {
        return dateTimeService;
    }


    /**
     * Sets the dateTimeService.
     * 
     * @see org.kuali.kfs.pdp.batch.service.impl.ProcessPdpCancelPaidServiceImpl#setDateTimeService(org.kuali.rice.kns.service.DateTimeService)
     */
    public void setDateTimeService(DateTimeService dateTimeService) {
        this.dateTimeService = dateTimeService;
    }


    /**
     * Gets the purchasingAccountsPayableModuleService.
     * 
     * @return purchasingAccountsPayableModuleService
     */
    public CuPurchasingAccountsPayableModuleService getPurchasingAccountsPayableModuleService() {
        return purchasingAccountsPayableModuleService;
    }


    /**
     * Sets the purchasingAccountsPayableModuleService.
     * 
     * @see org.kuali.kfs.pdp.batch.service.impl.ProcessPdpCancelPaidServiceImpl#setPurchasingAccountsPayableModuleService(org.kuali.kfs.integration.purap.PurchasingAccountsPayableModuleService)
     */
    public void setPurchasingAccountsPayableModuleService(CuPurchasingAccountsPayableModuleService purchasingAccountsPayableModuleService) {
        this.purchasingAccountsPayableModuleService = purchasingAccountsPayableModuleService;
    }


    /**
     * Gets the dvExtractService.
     * 
     * @return dvExtractService
     */
    public DisbursementVoucherExtractService getDvExtractService() {
        return dvExtractService;
    }


    /**
     * Sets the dvExtractService.
     * 
     * @see org.kuali.kfs.pdp.batch.service.impl.ProcessPdpCancelPaidServiceImpl#setDvExtractService(org.kuali.kfs.fp.batch.service.DisbursementVoucherExtractService)
     */
    public void setDvExtractService(DisbursementVoucherExtractService dvExtractService) {
        this.dvExtractService = dvExtractService;
    }

}
