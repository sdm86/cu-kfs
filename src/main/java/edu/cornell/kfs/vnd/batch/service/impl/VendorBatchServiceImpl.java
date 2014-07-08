package edu.cornell.kfs.vnd.batch.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.kfs.sys.batch.BatchInputFileType;
import org.kuali.kfs.sys.batch.service.BatchInputFileService;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.kfs.sys.exception.ParseException;
import org.kuali.kfs.vnd.businessobject.VendorAddress;
import org.kuali.kfs.vnd.businessobject.VendorContact;
import org.kuali.kfs.vnd.businessobject.VendorDetail;
import org.kuali.kfs.vnd.businessobject.VendorHeader;
import org.kuali.kfs.vnd.document.VendorMaintainableImpl;
import org.kuali.rice.krad.UserSession;
import org.kuali.rice.krad.exception.ValidationException;
import org.kuali.rice.krad.maintenance.MaintenanceDocument;
import org.kuali.rice.krad.service.DocumentService;
import org.kuali.rice.krad.util.GlobalVariables;
import org.kuali.rice.krad.util.MessageMap;
import org.kuali.rice.krad.util.ObjectUtils;

import edu.cornell.kfs.vnd.batch.service.VendorBatchService;
import edu.cornell.kfs.vnd.businessobject.CuVendorAddressExtension;
import edu.cornell.kfs.vnd.businessobject.VendorBatchAddress;
import edu.cornell.kfs.vnd.businessobject.VendorBatchContact;
import edu.cornell.kfs.vnd.businessobject.VendorBatchDetail;
import edu.cornell.kfs.vnd.businessobject.VendorDetailExtension;
import edu.cornell.kfs.vnd.document.service.CUVendorService;

public class VendorBatchServiceImpl implements VendorBatchService{
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(VendorBatchServiceImpl.class);
    
    private BatchInputFileService batchInputFileService;    
    private List<BatchInputFileType> batchInputFileTypes;
    
    public VendorBatchServiceImpl() {
    }

    /*
     * (non-Javadoc)
     * @see edu.cornell.kfs.vnd.batch.service.VendorBatchService#processVendors()
     */
    public boolean processVendors() {
        
        LOG.info("Beginning processing of all available files for Receipt Batch Upload.");
        boolean result = true;
                     
        //  create a list of the files to process
         Map<String, BatchInputFileType> fileNamesToLoad = getListOfFilesToProcess();
        LOG.info("Found " + fileNamesToLoad.size() + " file(s) to process.");
        
        //  process each file in turn
        List<String> processedFiles = new ArrayList<String>();
        for (String inputFileName : fileNamesToLoad.keySet()) {
            
            LOG.info("Beginning processing of filename: " + inputFileName + ".");
   
            if (updateVendors(inputFileName, fileNamesToLoad.get(inputFileName))) {
                result &= true;
                LOG.info("Successfully loaded csv file");
                processedFiles.add(inputFileName);
            }
            else {
                LOG.error("Failed to load file");
                result &= false;
            }
        }

        //  remove done files
        removeDoneFiles(processedFiles);
       
        return result;
    }
    
    /**
     * Create a collection of the files to process with the mapped value of the BatchInputFileType
     * 
     * @return
     */
    protected Map<String, BatchInputFileType> getListOfFilesToProcess() {

        Map<String, BatchInputFileType> inputFileTypeMap = new LinkedHashMap<String, BatchInputFileType>();

        for (BatchInputFileType batchInputFileType : batchInputFileTypes) {

            List<String> inputFileNames = batchInputFileService.listInputFileNamesWithDoneFile(batchInputFileType);
            if (inputFileNames == null) {
                criticalError("BatchInputFileService.listInputFileNamesWithDoneFile(" + batchInputFileType.getFileTypeIdentifer() + ") returned NULL which should never happen.");
            }
            else {
                // update the file name mapping
                for (String inputFileName : inputFileNames) {

                    // filenames returned should never be blank/empty/null
                    if (StringUtils.isBlank(inputFileName)) {
                        criticalError("One of the file names returned as ready to process [" + inputFileName + "] was blank.  This should not happen, so throwing an error to investigate.");
                    }

                    inputFileTypeMap.put(inputFileName, batchInputFileType);
                }
            }
        }

        return inputFileTypeMap;
    }
    
    /**
     * Clears out associated .done files for the processed data files.
     * 
     * @param dataFileNames
     */
    protected void removeDoneFiles(List<String> dataFileNames) {
        for (String dataFileName : dataFileNames) {
            File doneFile = new File(StringUtils.substringBeforeLast(dataFileName, ".") + ".done");
            if (doneFile.exists()) {
                doneFile.delete();
            }
        }
    }

    /*
     * process each parsed data record and add/update vendor accordingly
     */
	protected boolean updateVendors(String fileName, BatchInputFileType batchInputFileType) {
        boolean result = true;
        
        //  load up the file into a byte array 
        byte[] fileByteContent = safelyLoadFileBytes(fileName);
        
        LOG.info("Attempting to parse the file");
        Object parsedObject = null;
        try {
             parsedObject =  batchInputFileService.parse(batchInputFileType, fileByteContent);
        }
        catch (ParseException e) {
            String errorMessage ="Error parsing batch file: " + e.getMessage();
            LOG.error(errorMessage, e);
            throw new RuntimeException(errorMessage);
        }
        
        //  make sure we got the type we expected, then cast it
        if (!(parsedObject instanceof List)) {
            String errorMessage = "Parsed file was not of the expected type.  Expected [" + List.class + "] but got [" + parsedObject.getClass() + "].";
            criticalError(errorMessage);
        }
        
        
        
       
        List<VendorBatchDetail> vendors =  ((List<VendorBatchDetail>) parsedObject);
        
        for (VendorBatchDetail vendorBatch  : vendors) {                                   
        // process each line to add/update vendor accordingly 
        	String returnVal = "";
        	if (StringUtils.isBlank(vendorBatch.getVendorNumber())) {
                returnVal = addVendor(vendorBatch);
        	} else {
                returnVal = changeVendor(vendorBatch);
        	}
            if (StringUtils.startsWith(returnVal, "Failed request")) {
            	// TODO : there is error.  need to handle here or before exit
            	LOG.error(returnVal);
            	result = false;
            } else {
            	LOG.info("Document " + returnVal + " routed.");
            }
        }        
                
        return result;
	}
	
	
    protected byte[] safelyLoadFileBytes(String fileName) {
    // TODO : several classes have this same method.  Should re-factor to a util class for sharing.    
        InputStream fileContents;
        byte[] fileByteContent;
        try {
            fileContents = new FileInputStream(fileName);
        }
        catch (FileNotFoundException e1) {
            LOG.error("Batch file not found [" + fileName + "]. " + e1.getMessage());
            throw new RuntimeException("Batch File not found [" + fileName + "]. " + e1.getMessage());
        }
        try {
            fileByteContent = IOUtils.toByteArray(fileContents);
        }
        catch (IOException e1) {
            LOG.error("IO Exception loading: [" + fileName + "]. " + e1.getMessage());
            throw new RuntimeException("IO Exception loading: [" + fileName + "]. " + e1.getMessage());
        }
        return fileByteContent;
    }

    private void criticalError(String errorMessage){
        LOG.error(errorMessage);
        throw new RuntimeException(errorMessage);
    }

	public BatchInputFileService getBatchInputFileService() {
		return batchInputFileService;
	}

	public void setBatchInputFileService(BatchInputFileService batchInputFileService) {
		this.batchInputFileService = batchInputFileService;
	}

	public List<BatchInputFileType> getBatchInputFileTypes() {
		return batchInputFileTypes;
	}

	public void setBatchInputFileTypes(List<BatchInputFileType> batchInputFileTypes) {
		this.batchInputFileTypes = batchInputFileTypes;
	}

	/*
	 * create vendor document and route
	 */
	protected String addVendor(VendorBatchDetail vendorBatch) {
//        UserSession actualUserSession = GlobalVariables.getUserSession();
//        MessageMap globalErrorMap = GlobalVariables.getMessageMap();
        GlobalVariables.setMessageMap(new MessageMap());
    	     
        // create and route doc as system user
        GlobalVariables.setUserSession(new UserSession("kfs"));
        LOG.info("addVendor "+vendorBatch.getLogData());       
        try {
        	DocumentService docService = SpringContext.getBean(DocumentService.class);
        	
            MaintenanceDocument vendorDoc = (MaintenanceDocument)docService.getNewDocument("PVEN");
            
            vendorDoc.getDocumentHeader().setDocumentDescription("New vendor from Procurement tool");
                        
        	VendorMaintainableImpl vImpl = (VendorMaintainableImpl)vendorDoc.getNewMaintainableObject();

        	VendorDetail vDetail = (VendorDetail)vImpl.getBusinessObject();
        	
        	vDetail.setVendorName(vendorBatch.getVendorName());
        	vDetail.setActiveIndicator(true);
        	vDetail.setTaxableIndicator(StringUtils.equalsIgnoreCase("Y", vendorBatch.getTaxable()));

        	((VendorDetailExtension)vDetail.getExtension()).setEinvoiceVendorIndicator(StringUtils.equalsIgnoreCase("Y", vendorBatch.geteInvoice()));
        	((VendorDetailExtension)vDetail.getExtension()).setDefaultB2BPaymentMethodCode( vendorBatch.getDefaultB2BPaymentMethodCode());

        	vDetail.setVendorAddresses(getVendorAddresses(vendorBatch.getVendorAddresses(), vDetail));

        	vDetail.setVendorContacts(getVendorContacts(vendorBatch.getVendorContacts()));

        	
//        	vDetail.setVendorPhoneNumbers(getVendorPhoneNumbers(phoneNumbers));

        	
        	VendorHeader vHeader = vDetail.getVendorHeader();
        	
        	vHeader.setVendorTypeCode(vendorBatch.getVendorTypeCode());
        	vHeader.setVendorTaxNumber(vendorBatch.getTaxNumber());
        	vHeader.setVendorTaxTypeCode(vendorBatch.getTaxNumberType());
        	vHeader.setVendorForeignIndicator(StringUtils.equalsIgnoreCase("Y", vendorBatch.getForeignVendor()));
        	vHeader.setVendorOwnershipCode(vendorBatch.getOwnershipTypeCode());

//        	vHeader.setVendorSupplierDiversities(getVendorSupplierDiversitys(supplierDiversitys));
        	vDetail.setVendorHeader(vHeader);
        	vImpl.setBusinessObject(vDetail);
        	vendorDoc.setNewMaintainableObject(vImpl);

        	docService.routeDocument(vendorDoc, "", null);
        	
            return vendorDoc.getDocumentNumber();
        } catch (Exception e) {
        	if (e instanceof ValidationException) {
        		return "Failed request : "+ e.getMessage() + " - " + GlobalVariables.getMessageMap().getErrorMessages();
        	} else {
        	    return "Failed request : "+ e.getMessage();
        	}
        } finally {
//            GlobalVariables.setUserSession(actualUserSession);
//            GlobalVariables.setMessageMap(globalErrorMap);
		}        
	}    

	/*
	 * convert list of vendor address batch data to list of vendor address
	 */
	private List<VendorAddress> getVendorAddresses(List<VendorBatchAddress> addresses, VendorDetail vDetail) {
    	List<VendorAddress> vAddrs = new ArrayList<VendorAddress>();
    	if (CollectionUtils.isNotEmpty(addresses)) {
			for (VendorBatchAddress address : addresses) {
				LOG.info("addVendor address " + address);
				VendorAddress vendorAddr = new VendorAddress();
				setVendorAddress(address, vendorAddr, vDetail);
				vAddrs.add(vendorAddr);
			}
    	}
    	return vAddrs;
	}

	/*
	 * convert one vendor address batch data from vendor address
	 */
	private void setVendorAddress(VendorBatchAddress address,VendorAddress vendorAddr, VendorDetail vDetail) {
		vendorAddr.setVendorAddressTypeCode(address.getVendorAddressTypeCode());
		vendorAddr.setVendorLine1Address(address.getVendorLine1Address());
		vendorAddr.setVendorCityName(address.getVendorCityName());
		vendorAddr.setVendorStateCode(address.getVendorStateCode());
		vendorAddr.setVendorZipCode(address.getVendorZipCode());
		vendorAddr.setVendorCountryCode(address.getVendorCountryCode());
		vendorAddr.setVendorDefaultAddressIndicator(true);
		vendorAddr.setVendorDefaultAddressIndicator(StringUtils.equalsIgnoreCase("Y", address.getVendorDefaultAddressIndicator()));
		if (vendorAddr.isVendorDefaultAddressIndicator()) {
			// TODO : which one should be the vdetail's default address because there are different type address ???
        	vDetail.setDefaultAddressLine1(address.getVendorLine1Address());
        	vDetail.setDefaultAddressCity(address.getVendorCityName());
        	vDetail.setDefaultAddressStateCode(address.getVendorStateCode());
        	vDetail.setDefaultAddressPostalCode(address.getVendorZipCode());
        	vDetail.setDefaultAddressCountryCode(address.getVendorCountryCode());
			
		}
		((CuVendorAddressExtension)vendorAddr.getExtension()).setPurchaseOrderTransmissionMethodCode(address.getPurchaseOrderTransmissionMethodCode());
		vendorAddr.setVendorAddressEmailAddress(address.getVendorAddressEmailAddress());						
		vendorAddr.setVendorFaxNumber(address.getVendorFaxNumber());
		vendorAddr.setActive(StringUtils.equalsIgnoreCase("Y", address.getActive()));
	}

	private String changeVendor(VendorBatchDetail vendorBatch) {
//		UserSession actualUserSession = GlobalVariables.getUserSession();
//		MessageMap globalErrorMap = GlobalVariables.getMessageMap();
        GlobalVariables.setMessageMap(new MessageMap());

		// create and route doc as system user
		GlobalVariables.setUserSession(new UserSession("kfs"));

		try {
			DocumentService docService = SpringContext.getBean(DocumentService.class);

			MaintenanceDocument vendorDoc = (MaintenanceDocument) docService.getNewDocument("PVEN");

			vendorDoc.getDocumentHeader().setDocumentDescription("Update vendor from Procurement tool");

			LOG.info("updateVendor " + vendorBatch.getLogData());
			VendorDetail vendor = retrieveVendor(vendorBatch.getVendorNumber(), "VENDORID");
			if (vendor != null) {
				// Vendor does not eist
				VendorMaintainableImpl oldVendorImpl = (VendorMaintainableImpl) vendorDoc.getOldMaintainableObject();
				oldVendorImpl.setBusinessObject(vendor);

			} else {
				// Vendor does not eist
				return "Vendor " + vendorBatch.getVendorNumber() + " Not Found.";
			}
				
			VendorMaintainableImpl vImpl = (VendorMaintainableImpl) vendorDoc.getNewMaintainableObject();

			vImpl.setMaintenanceAction(KFSConstants.MAINTENANCE_EDIT_ACTION);
//			VendorDetail vendorCopy = (VendorDetail)ObjectUtils.deepCopy(vendor);
			vImpl.setBusinessObject((VendorDetail)ObjectUtils.deepCopy(vendor));
			VendorDetail vDetail = (VendorDetail) vImpl.getBusinessObject();

        	vDetail.setVendorName(vendorBatch.getVendorName());
        	vDetail.setActiveIndicator(true);
        	vDetail.setTaxableIndicator(StringUtils.equalsIgnoreCase("Y", vendorBatch.getTaxable()));

			((VendorDetailExtension) vDetail.getExtension()).setEinvoiceVendorIndicator(StringUtils.equalsIgnoreCase("Y", vendorBatch.geteInvoice()));

			
        	updateVendorAddresses(vendorBatch.getVendorAddresses(), vendor, vDetail);
//			vDetail.setVendorAddresses(vAddrs);

            updateVendorContacts(vendorBatch.getVendorContacts(), vendor, vDetail);
//        	updateVendorPhoneNumbers(phoneNumbers, vendor, vDetail);
//
//        	updateVendorSupplierDiversitys(supplierDiversitys, vendor, vDetail);

			VendorHeader vHeader = vDetail.getVendorHeader();

        	vHeader.setVendorTypeCode(vendorBatch.getVendorTypeCode());
        	vHeader.setVendorForeignIndicator(StringUtils.equalsIgnoreCase("Y", vendorBatch.getForeignVendor()));
        	vHeader.setVendorOwnershipCode(vendorBatch.getOwnershipTypeCode());

			vDetail.setVendorHeader(vHeader);
			vImpl.setBusinessObject(vDetail);
			vendorDoc.setNewMaintainableObject(vImpl);

			docService.routeDocument(vendorDoc, "", null);

			return vendorDoc.getDocumentNumber();
        } catch (Exception e) {
        	LOG.info("updateVendor STE " + e.getStackTrace()+e.toString());
        	if (e instanceof ValidationException) {
        		return "Failed request : "+ e.getMessage() + " - " + GlobalVariables.getMessageMap().getErrorMessages();
        	} else {
        	    return "Failed request : "+ e.getMessage();
        	}
		} finally {
//			GlobalVariables.setUserSession(actualUserSession);
//			GlobalVariables.setMessageMap(globalErrorMap);
		}
	}	

	private void updateVendorAddresses(List<VendorBatchAddress> addresses, VendorDetail vendor, VendorDetail vDetail) {
    	if (CollectionUtils.isNotEmpty(addresses)) {
			for (VendorBatchAddress address : addresses) {
				VendorAddress vendorAddr = new VendorAddress();
				LOG.info("updateVendor ADDRESS " + address +  "~"  + address.getVendorAddressTypeCode() + "~" + address.getVendorAddressGeneratedIdentifier());
				if (StringUtils.isNotBlank(address.getVendorAddressGeneratedIdentifier())) {
					vendorAddr = getVendorAddress(vDetail, Integer.valueOf(address.getVendorAddressGeneratedIdentifier()));
				}
				setVendorAddress(address, vendorAddr, vDetail);
				
				if (vendorAddr.getVendorAddressGeneratedIdentifier() == null) {
					vDetail.getVendorAddresses().add(vendorAddr);
					vendor.getVendorAddresses().add(new VendorAddress()); 
				}
				// TODO : how about those existing addr, but not passed from request, should they be 'inactivated' ?
			}        	
    	}
	}

	private VendorAddress getVendorAddress(VendorDetail vDetail, Integer vendorAddressGeneratedIdentifier) {
		for (VendorAddress vAddress : vDetail.getVendorAddresses()) {
			if (vendorAddressGeneratedIdentifier.equals(vAddress.getVendorAddressGeneratedIdentifier())) {
				return vAddress;
			}
		}
		return new VendorAddress();
	}

	/**
	 * 
	 * @param vendorId
	 * @param vendorIdType
	 * @return
	 * @throws Exception
	 */
	private VendorDetail retrieveVendor(String vendorId, String vendorIdType) throws Exception {
		VendorDetail vendor = null;
		CUVendorService vendorService = SpringContext.getBean(CUVendorService.class);
		if (StringUtils.equalsIgnoreCase(vendorIdType, "DUNS")) {
			vendor = vendorService.getVendorByDunsNumber(vendorId);
		} else if (StringUtils.equalsIgnoreCase(vendorIdType, "VENDORID")) {
			vendor = vendorService.getByVendorNumber(vendorId);
		} else if (StringUtils.equalsIgnoreCase(vendorIdType, "VENDORNAME")) {
			vendor = vendorService.getVendorByVendorName(vendorId);
		}
		return vendor;
	}

	private void updateVendorContacts(List<VendorBatchContact> contacts, VendorDetail vendor, VendorDetail vDetail) {
    	if (CollectionUtils.isNotEmpty(contacts)) {
	    	for (VendorBatchContact contact : contacts) {
				LOG.info("updateVendor contact " + contact +  "~" + contact.getVendorContactGeneratedIdentifier() + "~" + contact.getVendorContactName());
	        	VendorContact vContact = new VendorContact();
	        	if (StringUtils.isNotBlank(contact.getVendorContactGeneratedIdentifier())) {
	        		vContact = getVendorContact(vDetail, Integer.valueOf(contact.getVendorContactGeneratedIdentifier()));
	        	}
				setVendorContact(contact, vContact);
	        	if (vContact.getVendorContactGeneratedIdentifier() == null) {
	            	vDetail.getVendorContacts().add(vContact);
				     vendor.getVendorContacts().add(new VendorContact());
	      		
	        	}
	        	// TODO : what to do with those existing contacts, but not passed from request
	   		
	    	}
    	}
	}

	private VendorContact getVendorContact(VendorDetail vDetail, Integer vendorContactGeneratedIdentifier) {
    	if (CollectionUtils.isNotEmpty(vDetail.getVendorContacts())) {
			for (VendorContact vContact : vDetail.getVendorContacts()) {
				if (vendorContactGeneratedIdentifier.equals(vContact.getVendorContactGeneratedIdentifier())) {
					return vContact;
				}
			}
		}
		return new VendorContact();
	}
	
	private void setVendorContact(VendorBatchContact contact,VendorContact vContact) {
    	vContact.setVendorContactTypeCode(contact.getVendorContactTypeCode());
    	vContact.setVendorContactName(contact.getVendorContactName());
    	vContact.setVendorContactEmailAddress(contact.getVendorContactEmailAddress());
    	vContact.setVendorContactCommentText(contact.getVendorContactCommentText());
    	vContact.setVendorLine1Address(contact.getVendorLine1Address());
    	vContact.setVendorLine2Address(contact.getVendorLine2Address());
    	vContact.setVendorCityName(contact.getVendorCityName());
    	vContact.setVendorCountryCode(contact.getVendorCountryCode());
    	vContact.setVendorStateCode(contact.getVendorStateCode());
    	vContact.setVendorZipCode(contact.getVendorZipCode());
    	vContact.setVendorAttentionName(contact.getVendorAttentionName());
    	vContact.setVendorAddressInternationalProvinceName(contact.getVendorAddressInternationalProvinceName());    	
    	vContact.setActive(StringUtils.equalsIgnoreCase("Y", contact.getActive()));

	}
	
	private List<VendorContact> getVendorContacts(List<VendorBatchContact> contacts) {
    	ArrayList<VendorContact> vendorContacts = new ArrayList<VendorContact>();
    	if (CollectionUtils.isNotEmpty(contacts)) {
			for (VendorBatchContact contact : contacts) {
				LOG.info("addVendor contact " + contact);
				VendorContact vContact = new VendorContact();
				setVendorContact(contact, vContact);
				vendorContacts.add(vContact);
			}
    	}
    	return vendorContacts;
	}
	

}
