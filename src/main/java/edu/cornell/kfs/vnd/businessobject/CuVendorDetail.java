package edu.cornell.kfs.vnd.businessobject;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.log4j.Logger;
import org.kuali.kfs.vnd.businessobject.VendorDetail;
import org.kuali.rice.krad.util.ObjectUtils;

public class CuVendorDetail extends VendorDetail {
	private static Logger LOG = Logger.getLogger(CuVendorDetail.class);
	
    /**
     * @see org.kuali.kfs.vnd.document.routing.VendorRoutingComparable#isEqualForRouting(java.lang.Object)
     */
    @Override
    public boolean isEqualForRouting(Object toCompare) {
        LOG.debug("Entering isEqualForRouting.");
        if ((ObjectUtils.isNull(toCompare)) || !(toCompare instanceof VendorDetail)) {
            return false;
        }
        else {
            VendorDetail detail = (VendorDetail) toCompare;
            return new EqualsBuilder().append(
                    this.getVendorHeaderGeneratedIdentifier(), detail.getVendorHeaderGeneratedIdentifier()).append(
                    this.getVendorDetailAssignedIdentifier(), detail.getVendorDetailAssignedIdentifier()).append(
                    this.isVendorParentIndicator(), detail.isVendorParentIndicator()).append(
                    this.getVendorName(), detail.getVendorName()).append(
                    this.getVendorLastName(), detail.getVendorLastName()).append(
                    this.getVendorFirstName(), detail.getVendorFirstName()).append(
                    this.isActiveIndicator(), detail.isActiveIndicator()).append(
                    this.getVendorInactiveReasonCode(), detail.getVendorInactiveReasonCode()).append(
                    this.getVendorDunsNumber(), detail.getVendorDunsNumber()).append(
                    this.getVendorPaymentTermsCode(), detail.getVendorPaymentTermsCode()).append(
                    this.getVendorShippingTitleCode(), detail.getVendorShippingTitleCode()).append(
                    this.getVendorShippingPaymentTermsCode(), detail.getVendorShippingPaymentTermsCode()).append(
                    this.getVendorConfirmationIndicator(), detail.getVendorConfirmationIndicator()).append(
                    this.getVendorPrepaymentIndicator(), detail.getVendorPrepaymentIndicator()).append(
                    this.getVendorCreditCardIndicator(), detail.getVendorCreditCardIndicator()).append(
                    this.getVendorMinimumOrderAmount(), detail.getVendorMinimumOrderAmount()).append(
                    this.getVendorUrlAddress(), detail.getVendorUrlAddress()).append(
                    this.getVendorRemitName(), detail.getVendorRemitName()).append(
                    this.getVendorRestrictedIndicator(), detail.getVendorRestrictedIndicator()).append(
                    this.getVendorRestrictedReasonText(), detail.getVendorRestrictedReasonText()).append(
                    this.getVendorRestrictedDate(), detail.getVendorRestrictedDate()).append(
                    this.getVendorRestrictedPersonIdentifier(), detail.getVendorRestrictedPersonIdentifier()).append(
                    this.getVendorSoldToGeneratedIdentifier(), detail.getVendorSoldToGeneratedIdentifier()).append(
                    this.getVendorSoldToAssignedIdentifier(), detail.getVendorSoldToAssignedIdentifier()).append(
                    this.getVendorSoldToName(), detail.getVendorSoldToName()).append( // KFSUPGRADE-779
                    this.isTaxableIndicator(), detail.isTaxableIndicator()).append( 
                    ((VendorDetailExtension)this.getExtension()).isInsuranceRequiredIndicator(),((VendorDetailExtension)detail.getExtension()).isInsuranceRequiredIndicator()).append(
                    ((VendorDetailExtension)this.getExtension()).getInsuranceRequirementsCompleteIndicator(),((VendorDetailExtension)detail.getExtension()).getInsuranceRequirementsCompleteIndicator()).append(
                    ((VendorDetailExtension)this.getExtension()).getCornellAdditionalInsuredIndicator(),((VendorDetailExtension)detail.getExtension()).getCornellAdditionalInsuredIndicator()).append(
                    ((VendorDetailExtension)this.getExtension()).getGeneralLiabilityCoverageAmount(),((VendorDetailExtension)detail.getExtension()).getGeneralLiabilityCoverageAmount()).append(
                    ((VendorDetailExtension)this.getExtension()).getGeneralLiabilityExpiration(),((VendorDetailExtension)detail.getExtension()).getGeneralLiabilityExpiration()).append(
                    ((VendorDetailExtension)this.getExtension()).getAutomobileLiabilityCoverageAmount(),((VendorDetailExtension)detail.getExtension()).getAutomobileLiabilityCoverageAmount()).append(
                    ((VendorDetailExtension)this.getExtension()).getAutomobileLiabilityExpiration(),((VendorDetailExtension)detail.getExtension()).getAutomobileLiabilityExpiration()).append(
                    ((VendorDetailExtension)this.getExtension()).getWorkmansCompCoverageAmount(),((VendorDetailExtension)detail.getExtension()).getWorkmansCompCoverageAmount()).append(
                    ((VendorDetailExtension)this.getExtension()).getWorkmansCompExpiration(),((VendorDetailExtension)detail.getExtension()).getWorkmansCompExpiration()).append(
                    ((VendorDetailExtension)this.getExtension()).getExcessLiabilityUmbExpiration(),((VendorDetailExtension)detail.getExtension()).getExcessLiabilityUmbExpiration()).append(
                    ((VendorDetailExtension)this.getExtension()).getExcessLiabilityUmbrellaAmount(),((VendorDetailExtension)detail.getExtension()).getExcessLiabilityUmbrellaAmount()).append(
                    ((VendorDetailExtension)this.getExtension()).getHealthOffSiteCateringLicenseReq(),((VendorDetailExtension)detail.getExtension()).getHealthOffSiteCateringLicenseReq()).append(
                    ((VendorDetailExtension)this.getExtension()).getHealthOffSiteLicenseExpirationDate(),((VendorDetailExtension)detail.getExtension()).getHealthOffSiteLicenseExpirationDate()).append(
                    ((VendorDetailExtension)this.getExtension()).getInsuranceNotes(),((VendorDetailExtension)detail.getExtension()).getInsuranceNotes()).append(
                    ((VendorDetailExtension)this.getExtension()).getMerchantNotes(),((VendorDetailExtension)detail.getExtension()).getMerchantNotes()).append( 
                    ((VendorDetailExtension)this.getExtension()).getDefaultB2BPaymentMethodCode(),((VendorDetailExtension)detail.getExtension()).getDefaultB2BPaymentMethodCode()).append( // end KFSUPGRADE-779
                    this.isVendorFirstLastNameIndicator(), detail.isVendorFirstLastNameIndicator()).isEquals();
        }
    }

}
