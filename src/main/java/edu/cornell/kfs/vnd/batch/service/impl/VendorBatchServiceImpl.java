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
import org.kuali.kfs.sys.batch.BatchInputFileType;
import org.kuali.kfs.sys.batch.service.BatchInputFileService;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.kfs.sys.exception.ParseException;
import org.kuali.kfs.vnd.businessobject.VendorAddress;
import org.kuali.kfs.vnd.businessobject.VendorDetail;
import org.kuali.kfs.vnd.businessobject.VendorHeader;
import org.kuali.kfs.vnd.document.VendorMaintainableImpl;
import org.kuali.rice.krad.UserSession;
import org.kuali.rice.krad.maintenance.MaintenanceDocument;
import org.kuali.rice.krad.service.DocumentService;
import org.kuali.rice.krad.util.GlobalVariables;
import org.kuali.rice.krad.util.MessageMap;

import edu.cornell.kfs.vnd.batch.service.VendorBatchService;
import edu.cornell.kfs.vnd.businessobject.CuVendorAddressExtension;
import edu.cornell.kfs.vnd.businessobject.VendorAddressBatch;
import edu.cornell.kfs.vnd.businessobject.VendorBatch;
import edu.cornell.kfs.vnd.businessobject.VendorDetailExtension;

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
        
        
        
       
        List<VendorBatch> vendors =  ((List<VendorBatch>) parsedObject);
        
        for (VendorBatch vendorBatch  : vendors) {                                   
        // process each line to add/update vendor accordingly            
            String returnVal = addVendor(vendorBatch);
            if (StringUtils.startsWith(returnVal, "Failed request")) {
            	// TODO : there is error.  need to handle here or before exit
            	result = false;
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
	protected String addVendor(VendorBatch vendorBatch) {
//        UserSession actualUserSession = GlobalVariables.getUserSession();
//        MessageMap globalErrorMap = GlobalVariables.getMessageMap();
        GlobalVariables.setMessageMap(new MessageMap());
    	     
        // create and route doc as system user
        GlobalVariables.setUserSession(new UserSession("kfs"));
        LOG.info("addVendor "+vendorBatch.getVendorName());       
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

//        	vDetail.setVendorContacts(getVendorContacts(contacts));

        	
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
        	return "Failed request : "+ e.getMessage();
        } finally {
//            GlobalVariables.setUserSession(actualUserSession);
//            GlobalVariables.setMessageMap(globalErrorMap);
		}        
	}    

	/*
	 * convert list of vendor address batch data to list of vendor address
	 */
	private List<VendorAddress> getVendorAddresses(List<VendorAddressBatch> addresses, VendorDetail vDetail) {
    	List<VendorAddress> vAddrs = new ArrayList<VendorAddress>();
    	if (CollectionUtils.isNotEmpty(addresses)) {
			for (VendorAddressBatch address : addresses) {
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
	private void setVendorAddress(VendorAddressBatch address,VendorAddress vendorAddr, VendorDetail vDetail) {
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

}
