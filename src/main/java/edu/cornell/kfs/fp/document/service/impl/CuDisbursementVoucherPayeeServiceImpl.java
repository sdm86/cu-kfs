package edu.cornell.kfs.fp.document.service.impl;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.kuali.kfs.fp.businessobject.DisbursementPayee;
import org.kuali.kfs.fp.document.DisbursementVoucherConstants;
import org.kuali.kfs.fp.document.service.impl.DisbursementVoucherPayeeServiceImpl;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.kfs.sys.KFSPropertyConstants;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.kfs.vnd.VendorConstants;
import org.kuali.kfs.vnd.businessobject.VendorDetail;
import org.kuali.rice.coreservice.framework.parameter.ParameterService;
import org.kuali.rice.kim.api.identity.Person;
import org.kuali.rice.kim.impl.KIMPropertyConstants;

import edu.cornell.kfs.fp.businessobject.CuDisbursementPayee;
import edu.cornell.kfs.fp.businessobject.CuDisbursementVoucherPayeeDetail;
import edu.cornell.kfs.fp.document.CuDisbursementVoucherConstants;
import edu.cornell.kfs.fp.document.CuDisbursementVoucherDocument;
import edu.cornell.kfs.fp.document.service.CuDisbursementVoucherPayeeService;



public class CuDisbursementVoucherPayeeServiceImpl extends DisbursementVoucherPayeeServiceImpl implements CuDisbursementVoucherPayeeService {

    /**
     * @see org.kuali.kfs.fp.document.service.DisbursementVoucherPayeeService#getPayeeFromVendor(org.kuali.kfs.vnd.businessobject.VendorDetail)
     */
    @Override
    public CuDisbursementPayee getPayeeFromVendor(VendorDetail vendorDetail) {
        CuDisbursementPayee disbursementPayee = new CuDisbursementPayee();

        disbursementPayee.setActive(vendorDetail.isActiveIndicator());

        disbursementPayee.setPayeeIdNumber(vendorDetail.getVendorNumber());
        disbursementPayee.setPayeeName(vendorDetail.getAltVendorName());
        disbursementPayee.setTaxNumber(vendorDetail.getVendorHeader().getVendorTaxNumber());

        String vendorTypeCode = vendorDetail.getVendorHeader().getVendorTypeCode();
        String payeeTypeCode = getVendorPayeeTypeCodeMapping().get(vendorTypeCode);
        disbursementPayee.setPayeeTypeCode(payeeTypeCode);

        String vendorAddress = MessageFormat.format(addressPattern, vendorDetail.getDefaultAddressLine1(), vendorDetail.getDefaultAddressCity(), vendorDetail.getDefaultAddressStateCode(), vendorDetail.getDefaultAddressCountryCode());
        disbursementPayee.setAddress(vendorAddress);

        return disbursementPayee;
    }
    
    
    @Override
    public String getPayeeTypeDescription(String payeeTypeCode) {
        String payeeTypeDescription = StringUtils.EMPTY;

        if (CuDisbursementVoucherConstants.DV_PAYEE_TYPE_EMPLOYEE.equals(payeeTypeCode) || 
        CuDisbursementVoucherConstants.DV_PAYEE_TYPE_ALUMNI.equals(payeeTypeCode) ||
        CuDisbursementVoucherConstants.DV_PAYEE_TYPE_STUDENT.equals(payeeTypeCode)) {
            payeeTypeDescription = parameterService.getParameterValueAsString(CuDisbursementVoucherDocument.class, CuDisbursementVoucherConstants.NON_VENDOR_EMPLOYEE_PAYEE_TYPE_LABEL_PARM_NM);
        }
        else if (CuDisbursementVoucherConstants.DV_PAYEE_TYPE_VENDOR.equals(payeeTypeCode)) {
            payeeTypeDescription = parameterService.getParameterValueAsString(CuDisbursementVoucherDocument.class, CuDisbursementVoucherConstants.PO_AND_DV_PAYEE_TYPE_LABEL_PARM_NM);
        }
        else if (CuDisbursementVoucherConstants.DV_PAYEE_TYPE_REVOLVING_FUND_VENDOR.equals(payeeTypeCode)) {
            payeeTypeDescription = this.getVendorTypeDescription(VendorConstants.VendorTypes.REVOLVING_FUND);
        }
        else if (CuDisbursementVoucherConstants.DV_PAYEE_TYPE_SUBJECT_PAYMENT_VENDOR.equals(payeeTypeCode)) {
            payeeTypeDescription = this.getVendorTypeDescription(VendorConstants.VendorTypes.SUBJECT_PAYMENT);
        }

        return payeeTypeDescription;
    }
    
    
    public DisbursementPayee getPayeeFromPerson(Person person, String payeeTypeCode) {
        CuDisbursementPayee disbursementPayee = new CuDisbursementPayee();

        disbursementPayee.setActive(person.isActive());
        
        Collection<String> payableEmplStatusCodes = SpringContext.getBean(ParameterService.class).getParameterValuesAsString(CuDisbursementVoucherDocument.class, CuDisbursementVoucherConstants.ALLOWED_EMPLOYEE_STATUSES_FOR_PAYMENT);

        if (StringUtils.equalsIgnoreCase(payeeTypeCode, DisbursementVoucherConstants.DV_PAYEE_TYPE_EMPLOYEE) && StringUtils.isNotBlank(person.getEmployeeId()) && payableEmplStatusCodes.contains(person.getEmployeeStatusCode())) {
            disbursementPayee.setPayeeIdNumber(person.getEmployeeId());
        } else {
            disbursementPayee.setPayeeIdNumber(person.getPrincipalId());
        }

        disbursementPayee.setPrincipalId(person.getPrincipalId());
        disbursementPayee.setPrincipalName(person.getPrincipalName()); 
        
        disbursementPayee.setPayeeName(person.getNameUnmasked());
        disbursementPayee.setTaxNumber(KFSConstants.BLANK_SPACE);

        disbursementPayee.setPayeeTypeCode(DisbursementVoucherConstants.DV_PAYEE_TYPE_EMPLOYEE);

        disbursementPayee.setPayeeTypeCode(payeeTypeCode);
        
        String personAddress = MessageFormat.format(addressPattern, person.getAddressLine1Unmasked(), person.getAddressCityUnmasked(), person.getAddressStateProvinceCodeUnmasked(), person.getAddressCountryCode() == null ? "" : person.getAddressCountryCode());
        disbursementPayee.setAddress(personAddress);

        return (DisbursementPayee) disbursementPayee;
    }
    
    /**
     * @see org.kuali.kfs.fp.document.service.DisbursementVoucherPayeeService#getFieldConversionBetweenPayeeAndPerson()
     */
    @Override
    public Map<String, String> getFieldConversionBetweenPayeeAndPerson() {
        Map<String, String> fieldConversionMap = super.getFieldConversionBetweenPayeeAndPerson();
        fieldConversionMap.put(KFSPropertyConstants.PERSON_USER_IDENTIFIER, KIMPropertyConstants.Person.PRINCIPAL_NAME);
        return fieldConversionMap;
    }
    
    public boolean isStudent(CuDisbursementVoucherPayeeDetail dvPayeeDetail) {
        String payeeTypeCode = dvPayeeDetail.getDisbursementVoucherPayeeTypeCode();
        return CuDisbursementVoucherConstants.DV_PAYEE_TYPE_STUDENT.equals(payeeTypeCode);
    }

    /**
     * @see org.kuali.kfs.fp.document.service.DisbursementVoucherPayeeService#isVendor(org.kuali.kfs.fp.businessobject.DisbursementPayee)
     */
    public boolean isStudent(CuDisbursementPayee payee) {
        String payeeTypeCode = payee.getPayeeTypeCode();
        return CuDisbursementVoucherConstants.DV_PAYEE_TYPE_STUDENT.equals(payeeTypeCode);
    }

    /**
     * @see org.kuali.kfs.fp.document.service.DisbursementVoucherPayeeService#isVendor(org.kuali.kfs.fp.businessobject.DisbursementVoucherPayeeDetail)
     */
    public boolean isAlumni(CuDisbursementVoucherPayeeDetail dvPayeeDetail) {
        String payeeTypeCode = dvPayeeDetail.getDisbursementVoucherPayeeTypeCode();
        return CuDisbursementVoucherConstants.DV_PAYEE_TYPE_ALUMNI.equals(payeeTypeCode);
    }

    /**
     * @see org.kuali.kfs.fp.document.service.DisbursementVoucherPayeeService#isVendor(org.kuali.kfs.fp.businessobject.DisbursementPayee)
     */
    public boolean isAlumni(CuDisbursementPayee payee) {
        String payeeTypeCode = payee.getPayeeTypeCode();
        return CuDisbursementVoucherConstants.DV_PAYEE_TYPE_ALUMNI.equals(payeeTypeCode);
    }

    /**
     * @see org.kuali.kfs.fp.document.service.DisbursementVoucherPayeeService#isVendor(org.kuali.kfs.fp.businessobject.DisbursementVoucherPayeeDetail)
     */
    

}