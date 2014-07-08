package edu.cornell.kfs.vnd.businessobject;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import sun.misc.Regexp;

/**
 * 
 * Non-persistable BO to hold data loaded from vendor batch csv file.
 *
 */
public class VendorBatchDetail {
	private String vendorNumber;
	private String vendorName;
	private String vendorTypeCode;
	private String foreignVendor;
	private String taxNumber;
	private String taxNumberType;
	private String ownershipTypeCode;
	private String defaultB2BPaymentMethodCode;
	private String taxable;
	private String eInvoice;
	private String addresses;
	private String contacts;
	
	private static final String DATA_DELIMITER = ";";
	public String getVendorName() {
		return vendorName;
	}
	public void setVendorName(String vendorName) {
		this.vendorName = vendorName;
	}
	public String getVendorTypeCode() {
		return vendorTypeCode;
	}
	public void setVendorTypeCode(String vendorTypeCode) {
		this.vendorTypeCode = vendorTypeCode;
	}
	public String getForeignVendor() {
		return foreignVendor;
	}
	public void setForeignVendor(String foreignVendor) {
		this.foreignVendor = foreignVendor;
	}
	public String getTaxNumber() {
		return taxNumber;
	}
	public void setTaxNumber(String taxNumber) {
		this.taxNumber = taxNumber;
	}
	public String getTaxNumberType() {
		return taxNumberType;
	}
	public void setTaxNumberType(String taxNumberType) {
		this.taxNumberType = taxNumberType;
	}
	public String getOwnershipTypeCode() {
		return ownershipTypeCode;
	}
	public void setOwnershipTypeCode(String ownershipTypeCode) {
		this.ownershipTypeCode = ownershipTypeCode;
	}
	public String getTaxable() {
		return taxable;
	}
	public void setTaxable(String taxable) {
		this.taxable = taxable;
	}
	public String geteInvoice() {
		return eInvoice;
	}
	public void seteInvoice(String eInvoice) {
		this.eInvoice = eInvoice;
	}

	public String getAddresses() {
		return addresses;
	}
	public void setAddresses(String addresses) {
		this.addresses = addresses;
	}
	
	public List<VendorBatchAddress> getVendorAddresses() {
		List<VendorBatchAddress> vendorAddresses = new ArrayList<VendorBatchAddress>();
		String[] addressLines = getAddresses().split("::");
		for (String addressLine : addressLines) {
			vendorAddresses.add(new VendorBatchAddress(addressLine.split("\\|")));
		}
		return vendorAddresses;
	}

	public String getDefaultB2BPaymentMethodCode() {
		return defaultB2BPaymentMethodCode;
	}
	public void setDefaultB2BPaymentMethodCode(String defaultB2BPaymentMethodCode) {
		this.defaultB2BPaymentMethodCode = defaultB2BPaymentMethodCode;
	}
	public String getVendorNumber() {
		return vendorNumber;
	}
	public void setVendorNumber(String vendorNumber) {
		this.vendorNumber = vendorNumber;
	}

	public String getLogData() {
	    StringBuilder sb = new StringBuilder();
	    if (StringUtils.isNotBlank(getVendorNumber())) {
	        sb.append(getVendorNumber()).append(DATA_DELIMITER);
	    }
	    sb.append(getVendorName()).append(DATA_DELIMITER)
        .append(getVendorTypeCode()).append(DATA_DELIMITER)
        .append(getOwnershipTypeCode()).append(DATA_DELIMITER)
        .append(getTaxNumberType());
	    return sb.toString();
	}
	public String getContacts() {
		return contacts;
	}
	public void setContacts(String contacts) {
		this.contacts = contacts;
	}
	
	public List<VendorBatchContact> getVendorContacts() {
		List<VendorBatchContact> vendorContacts = new ArrayList<VendorBatchContact>();
		String[] contactLines = getContacts().split("::");
		for (String contactLine : contactLines) {
			vendorContacts.add(new VendorBatchContact(contactLine.split("\\|")));
		}
		return vendorContacts;
	}

}
