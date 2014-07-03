package edu.cornell.kfs.vnd.businessobject;

/**
 *  non-persistable to hold vendor address converted from input data file
 **/
public class VendorAddressBatch {

    private String vendorAddressTypeCode;
    private String vendorLine1Address;
    private String vendorLine2Address;
    private String vendorCityName;
    private String vendorStateCode;
    private String vendorZipCode;
    private String vendorCountryCode;
    private String vendorAttentionName;
    private String vendorAddressInternationalProvinceName;
    private String vendorAddressEmailAddress;
    private String vendorBusinessToBusinessUrlAddress;
    private String vendorFaxNumber;
    private String vendorDefaultAddressIndicator;
    private String purchaseOrderTransmissionMethodCode;
    private String active;
    
    public VendorAddressBatch(String[] address) {
    	vendorAddressTypeCode = address[0];
        vendorLine1Address = address[1];
        vendorLine2Address = address[2];
        vendorCityName = address[3];
        vendorStateCode = address[4];
        vendorZipCode = address[5];
        vendorCountryCode = address[6];
        vendorAttentionName = address[7];
        vendorAddressInternationalProvinceName = address[8];
        vendorAddressEmailAddress = address[9];
        vendorBusinessToBusinessUrlAddress = address[10];
        vendorFaxNumber = address[11];
        vendorDefaultAddressIndicator = address[12];
        purchaseOrderTransmissionMethodCode = address[13];
        active = address[14];
    }
    
	public String getVendorAddressTypeCode() {
		return vendorAddressTypeCode;
	}
	public void setVendorAddressTypeCode(String vendorAddressTypeCode) {
		this.vendorAddressTypeCode = vendorAddressTypeCode;
	}
	public String getVendorLine1Address() {
		return vendorLine1Address;
	}
	public void setVendorLine1Address(String vendorLine1Address) {
		this.vendorLine1Address = vendorLine1Address;
	}
	public String getVendorLine2Address() {
		return vendorLine2Address;
	}
	public void setVendorLine2Address(String vendorLine2Address) {
		this.vendorLine2Address = vendorLine2Address;
	}
	public String getVendorCityName() {
		return vendorCityName;
	}
	public void setVendorCityName(String vendorCityName) {
		this.vendorCityName = vendorCityName;
	}
	public String getVendorStateCode() {
		return vendorStateCode;
	}
	public void setVendorStateCode(String vendorStateCode) {
		this.vendorStateCode = vendorStateCode;
	}
	public String getVendorZipCode() {
		return vendorZipCode;
	}
	public void setVendorZipCode(String vendorZipCode) {
		this.vendorZipCode = vendorZipCode;
	}
	public String getVendorCountryCode() {
		return vendorCountryCode;
	}
	public void setVendorCountryCode(String vendorCountryCode) {
		this.vendorCountryCode = vendorCountryCode;
	}
	public String getVendorAttentionName() {
		return vendorAttentionName;
	}
	public void setVendorAttentionName(String vendorAttentionName) {
		this.vendorAttentionName = vendorAttentionName;
	}
	public String getVendorAddressInternationalProvinceName() {
		return vendorAddressInternationalProvinceName;
	}
	public void setVendorAddressInternationalProvinceName(
			String vendorAddressInternationalProvinceName) {
		this.vendorAddressInternationalProvinceName = vendorAddressInternationalProvinceName;
	}
	public String getVendorAddressEmailAddress() {
		return vendorAddressEmailAddress;
	}
	public void setVendorAddressEmailAddress(String vendorAddressEmailAddress) {
		this.vendorAddressEmailAddress = vendorAddressEmailAddress;
	}
	public String getVendorBusinessToBusinessUrlAddress() {
		return vendorBusinessToBusinessUrlAddress;
	}
	public void setVendorBusinessToBusinessUrlAddress(
			String vendorBusinessToBusinessUrlAddress) {
		this.vendorBusinessToBusinessUrlAddress = vendorBusinessToBusinessUrlAddress;
	}
	public String getVendorFaxNumber() {
		return vendorFaxNumber;
	}
	public void setVendorFaxNumber(String vendorFaxNumber) {
		this.vendorFaxNumber = vendorFaxNumber;
	}
	public String getVendorDefaultAddressIndicator() {
		return vendorDefaultAddressIndicator;
	}
	public void setVendorDefaultAddressIndicator(
			String vendorDefaultAddressIndicator) {
		this.vendorDefaultAddressIndicator = vendorDefaultAddressIndicator;
	}
	public String getPurchaseOrderTransmissionMethodCode() {
		return purchaseOrderTransmissionMethodCode;
	}
	public void setPurchaseOrderTransmissionMethodCode(
			String purchaseOrderTransmissionMethodCode) {
		this.purchaseOrderTransmissionMethodCode = purchaseOrderTransmissionMethodCode;
	}
	public String getActive() {
		return active;
	}
	public void setActive(String active) {
		this.active = active;
	}
    
    

}
