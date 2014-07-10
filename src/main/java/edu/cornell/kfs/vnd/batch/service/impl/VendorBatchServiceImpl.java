package edu.cornell.kfs.vnd.batch.service.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
import org.kuali.kfs.vnd.businessobject.VendorPhoneNumber;
import org.kuali.kfs.vnd.businessobject.VendorSupplierDiversity;
import org.kuali.kfs.vnd.document.VendorMaintainableImpl;
import org.kuali.rice.core.api.config.property.ConfigurationService;
import org.kuali.rice.core.api.datetime.DateTimeService;
import org.kuali.rice.kim.api.identity.Person;
import org.kuali.rice.kim.api.identity.PersonService;
import org.kuali.rice.krad.UserSession;
import org.kuali.rice.krad.bo.Attachment;
import org.kuali.rice.krad.bo.Note;
import org.kuali.rice.krad.document.Document;
import org.kuali.rice.krad.exception.ValidationException;
import org.kuali.rice.krad.maintenance.MaintenanceDocument;
import org.kuali.rice.krad.service.AttachmentService;
import org.kuali.rice.krad.service.DocumentService;
import org.kuali.rice.krad.service.MaintenanceDocumentService;
import org.kuali.rice.krad.util.ErrorMessage;
import org.kuali.rice.krad.util.GlobalVariables;
import org.kuali.rice.krad.util.MessageMap;
import org.kuali.rice.krad.util.ObjectUtils;

import edu.cornell.kfs.vnd.batch.service.VendorBatchService;
import edu.cornell.kfs.vnd.businessobject.CuVendorAddressExtension;
import edu.cornell.kfs.vnd.businessobject.CuVendorSupplierDiversityExtension;
import edu.cornell.kfs.vnd.businessobject.VendorBatchAddress;
import edu.cornell.kfs.vnd.businessobject.VendorBatchContact;
import edu.cornell.kfs.vnd.businessobject.VendorBatchDetail;
import edu.cornell.kfs.vnd.businessobject.VendorBatchPhoneNumber;
import edu.cornell.kfs.vnd.businessobject.VendorBatchSupplierDiversity;
import edu.cornell.kfs.vnd.businessobject.VendorDetailExtension;
import edu.cornell.kfs.vnd.document.service.CUVendorService;

public class VendorBatchServiceImpl implements VendorBatchService{
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(VendorBatchServiceImpl.class);
    
    private BatchInputFileService batchInputFileService;    
    private List<BatchInputFileType> batchInputFileTypes;
    private AttachmentService attachmentService;
    private PersonService personService;
    private DateTimeService dateTimeService; 
    private ConfigurationService configurationService;
    private DocumentService documentService;
    private MaintenanceDocumentService maintenanceDocumentService;
    
    public VendorBatchServiceImpl() {
    }

    /*
     * (non-Javadoc)
     * @see edu.cornell.kfs.vnd.batch.service.VendorBatchService#processVendors()
     */
    public boolean processVendors() {
        
        LOG.info("Beginning processing of all available files for Receipt Batch Upload.");
        boolean result = true;
        StringBuilder processResults = new StringBuilder();
        //  create a list of the files to process
        Map<String, BatchInputFileType> fileNamesToLoad = getListOfFilesToProcess();
        LOG.info("Found " + fileNamesToLoad.size() + " file(s) to process.");
        //  process each file in turn
        List<String> processedFiles = new ArrayList<String>();
        for (String inputFileName : fileNamesToLoad.keySet()) {
            
            LOG.info("Beginning processing of filename: " + inputFileName + ".");
            processResults.append("Beginning processing of filename: " + inputFileName + ". \n");
  
            if (maintainVendors(inputFileName, fileNamesToLoad.get(inputFileName), processResults)) {
                result &= true;
                LOG.info("Successfully loaded csv file");
                processedFiles.add(inputFileName);
            } else {
                LOG.error("Failed to load file");
                result &= false;
            }
        }

        //  remove done files
        removeDoneFiles(processedFiles);
        getReportWriter(processResults.toString());       
   
        return result;
    }

    protected void getReportWriter(String reportLog) {
        
       String reportDropFolder = new File(batchInputFileTypes.get(0).getDirectoryPath()) + "/report/";
        String fileName = "VENDORBATCH_OUT_" +
            new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(dateTimeService.getCurrentDate()) + ".txt";
        
         //  setup the writer
         File reportFile = new File(reportDropFolder + fileName);
         BufferedWriter writer = null;
         try {
             writer = new BufferedWriter(new FileWriter(reportFile));
             writer.write(reportLog);
             writer.close();
         } catch (IOException e1) {
             LOG.error("IOException when trying to write report file");
             e1.printStackTrace();
         }
         
                         
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
	private boolean maintainVendors(String fileName, BatchInputFileType batchInputFileType, StringBuilder processResults) {
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
        		processResults.append("add vendor : " + vendorBatch.getLogData() +"\n");
                returnVal = addVendor(vendorBatch);
        	} else {
        		processResults.append("update vendor : " + vendorBatch.getLogData() +"\n");
                returnVal = updateVendor(vendorBatch);
        	}
            if (StringUtils.startsWith(returnVal, "Failed request")) {
            	// TODO : there is error.  need to handle here or before exit
            	LOG.error(returnVal);
            	result = false;
        		processResults.append(returnVal +"\n");
           } else {
            	LOG.info("Document " + returnVal + " routed.");
        		processResults.append("Document " + returnVal + " routed." +"\n");
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
	private String addVendor(VendorBatchDetail vendorBatch) {
        GlobalVariables.setMessageMap(new MessageMap());
    	     
        // create and route doc as system user
        GlobalVariables.setUserSession(new UserSession("kfs"));
        LOG.info("addVendor "+vendorBatch.getLogData());       
        try {
        	
            MaintenanceDocument vendorDoc = (MaintenanceDocument)documentService.getNewDocument("PVEN");
            
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

        	
        	vDetail.setVendorPhoneNumbers(getVendorPhoneNumbers(vendorBatch.getVendorPhoneNumbers()));

        	
        	VendorHeader vHeader = vDetail.getVendorHeader();
        	
        	vHeader.setVendorTypeCode(vendorBatch.getVendorTypeCode());
        	vHeader.setVendorTaxNumber(vendorBatch.getTaxNumber());
        	vHeader.setVendorTaxTypeCode(vendorBatch.getTaxNumberType());
        	vHeader.setVendorForeignIndicator(StringUtils.equalsIgnoreCase("Y", vendorBatch.getForeignVendor()));
        	vHeader.setVendorOwnershipCode(vendorBatch.getOwnershipTypeCode());

        	vHeader.setVendorSupplierDiversities(getVendorSupplierDiversities(vendorBatch.getVendorSupplierDiversities()));
        	vDetail.setVendorHeader(vHeader);
        	vImpl.setBusinessObject(vDetail);
        	vendorDoc.setNewMaintainableObject(vImpl);
			if (StringUtils.isNotBlank(vendorBatch.getAttachmentFiles())) {
			    loadDocumentAttachments(vendorDoc, Arrays.asList(vendorBatch.getAttachmentFiles().split("\\|")));
			}

			documentService.routeDocument(vendorDoc, "", null);
        	
            return vendorDoc.getDocumentNumber();
        } catch (Exception e) {
        	if (e instanceof ValidationException) {
        		return "Failed request : "+ e.getMessage() + " - " +  getValidationErrorMessage();
        	} else {
        	    return "Failed request : "+ e.getCause() + " - " + e.getMessage();
        	}
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

	private String getValidationErrorMessage() {
		StringBuilder validationError = new StringBuilder();
        for (String errorProperty : GlobalVariables.getMessageMap().getAllPropertiesWithErrors()) {
            for (Object errorMessage : GlobalVariables.getMessageMap().getMessages(errorProperty)) {
                String errorMsg = configurationService.getPropertyValueAsString(((ErrorMessage) errorMessage).getErrorKey());
                if (errorMsg == null) {
                    throw new RuntimeException("Cannot find message for error key: " + ((ErrorMessage) errorMessage).getErrorKey());
                }
                else {
                    Object[] arguments = (Object[]) ((ErrorMessage) errorMessage).getMessageParameters();
                    if (arguments != null && arguments.length != 0) {
                        errorMsg = MessageFormat.format(errorMsg, arguments);
                    }
                }
                validationError.append(errorMsg + "\n");;
            }
        }
        return validationError.toString();

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

	private String updateVendor(VendorBatchDetail vendorBatch) {
        GlobalVariables.setMessageMap(new MessageMap());

		// create and route doc as system user
//		GlobalVariables.setUserSession(new UserSession("kfs"));

		try {

			MaintenanceDocument vendorDoc = (MaintenanceDocument) documentService.getNewDocument("PVEN");

			vendorDoc.getDocumentHeader().setDocumentDescription("Update vendor from Procurement tool");

			LOG.info("updateVendor " + vendorBatch.getLogData());
			VendorDetail vendor = retrieveVendor(vendorBatch.getVendorNumber(), "VENDORID");
			if (vendor != null) {
				// Vendor does not eist
				VendorMaintainableImpl oldVendorImpl = (VendorMaintainableImpl) vendorDoc.getOldMaintainableObject();
				oldVendorImpl.setBusinessObject(vendor);

			} else {
				// Vendor does not eist
				return "Failed request : Vendor " + vendorBatch.getVendorNumber() + " Not Found.";
			}
				
			VendorMaintainableImpl vImpl = (VendorMaintainableImpl) vendorDoc.getNewMaintainableObject();

			vImpl.setMaintenanceAction(KFSConstants.MAINTENANCE_EDIT_ACTION);
			vImpl.setBusinessObject((VendorDetail)ObjectUtils.deepCopy(vendor));
			VendorDetail vDetail = (VendorDetail) vImpl.getBusinessObject();

        	vDetail.setVendorName(vendorBatch.getVendorName());
        	vDetail.setActiveIndicator(true);
        	vDetail.setTaxableIndicator(StringUtils.equalsIgnoreCase("Y", vendorBatch.getTaxable()));

			((VendorDetailExtension) vDetail.getExtension()).setEinvoiceVendorIndicator(StringUtils.equalsIgnoreCase("Y", vendorBatch.geteInvoice()));

			
        	updateVendorAddresses(vendorBatch.getVendorAddresses(), vendor, vDetail);
//			vDetail.setVendorAddresses(vAddrs);

            updateVendorContacts(vendorBatch.getVendorContacts(), vendor, vDetail);
        	updateVendorPhoneNumbers(vendorBatch.getVendorPhoneNumbers(), vendor, vDetail);
//
        	updateVendorSupplierDiversitys(vendorBatch.getVendorSupplierDiversities(), vendor, vDetail);

			VendorHeader vHeader = vDetail.getVendorHeader();

        	vHeader.setVendorTypeCode(vendorBatch.getVendorTypeCode());
        	vHeader.setVendorForeignIndicator(StringUtils.equalsIgnoreCase("Y", vendorBatch.getForeignVendor()));
        	vHeader.setVendorOwnershipCode(vendorBatch.getOwnershipTypeCode());

			vDetail.setVendorHeader(vHeader);
			vImpl.setBusinessObject(vDetail);
			vendorDoc.setNewMaintainableObject(vImpl);

			// attachment
			if (StringUtils.isNotBlank(vendorBatch.getAttachmentFiles())) {
			    loadDocumentAttachments(vendorDoc, Arrays.asList(vendorBatch.getAttachmentFiles().split("\\|")));
			}
			// end attachment
			documentService.routeDocument(vendorDoc, "", null);
			return vendorDoc.getDocumentNumber();
        } catch (Exception e) {
        	LOG.info("updateVendor STE " + e.getStackTrace()+e.toString());
        	if (e instanceof ValidationException) {
        		return "Failed request : "+ e.getMessage() + " - " +  getValidationErrorMessage();
        	} else {
        	    return "Failed request : "+ e.getCause() + " - " + e.getMessage();
        	}
		}
	}	

    private void loadDocumentAttachments(Document document, List<String> attachments) {
    	String attachmentsPath = new File(batchInputFileTypes.get(0).getDirectoryPath()).toString() + "/attachment";
        
        for (String attachment : attachments) {
            Note note = new Note();

            note.setNoteText("Procurement vendor batch process - add attachment");
            note.setRemoteObjectIdentifier(document.getObjectId());
            note.setAuthorUniversalIdentifier(getSystemUser().getPrincipalId());
            note.setNoteTypeCode(KFSConstants.NoteTypeEnum.BUSINESS_OBJECT_NOTE_TYPE.getCode());
            note.setNotePostedTimestampToCurrent();
            
            // attempt to load file
            String fileName = attachmentsPath + "/" + attachment;
            File attachmentFile = new File(fileName);
            if (!attachmentFile.exists()) {
//                errorMap.putError(KFSConstants.GLOBAL_ERRORS, KFSKeyConstants.ERROR_BATCH_FEED_ATTACHMENT, new String[] { attachment, attachmentsPath });
                continue;
            }

            try {
                FileInputStream fileInputStream = new FileInputStream(fileName);
                Integer fileSize = Integer.parseInt(Long.toString(attachmentFile.length()));

                // TODO : Files.probeContentType is supported by java 7.  Not sure if this will be an issue
                String mimeTypeCode = Files.probeContentType(attachmentFile.toPath());
                // TODO : urlconnection is working for java 7 and under, but it return null for 'docx/pptx/xslx' 
                String type = URLConnection.guessContentTypeFromName(attachmentFile.getAbsolutePath());

                String attachType = "";

                Attachment noteAttachment = attachmentService.createAttachment(document.getDocumentHeader(), attachment, mimeTypeCode, fileSize, fileInputStream, attachType);

                note.addAttachment(noteAttachment);
                document.addNote(note);
            }
            catch (FileNotFoundException e) {
//                errorMap.putError(KFSConstants.GLOBAL_ERRORS, KFSKeyConstants.ERROR_BATCH_FEED_ATTACHMENT, new String[] { attachment, attachmentsPath });
                continue;
            }
            catch (IOException e1) {
                throw new RuntimeException("Unable to create attachment for File: " + fileName, e1);
            }
        }
    }

    private Person getSystemUser() {
        return personService.getPersonByPrincipalName(KFSConstants.SYSTEM_USER);
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

	private void updateVendorContacts(List<VendorBatchContact> contacts, VendorDetail oldVendorDetail, VendorDetail vDetail) {
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
				    oldVendorDetail.getVendorContacts().add(new VendorContact());
	      		
	        	}
	   		
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
	
	private void updateVendorPhoneNumbers(List<VendorBatchPhoneNumber> phoneNumbers, VendorDetail vendor, VendorDetail vDetail) {
    	if (CollectionUtils.isNotEmpty(phoneNumbers)) {
	    	for (VendorBatchPhoneNumber phoneNumber : phoneNumbers) {
				LOG.info("updateVendor phoneNumber " + phoneNumber + "~" + phoneNumber.getVendorPhoneGeneratedIdentifier() + "~" 
						+ phoneNumber.getVendorPhoneTypeCode());
	    		VendorPhoneNumber vPhoneNumber = new VendorPhoneNumber();
	        	if (StringUtils.isNotBlank(phoneNumber.getVendorPhoneGeneratedIdentifier())) {
	        		vPhoneNumber = getVendorPhoneNumber(vDetail, Integer.valueOf(phoneNumber.getVendorPhoneGeneratedIdentifier()));
	        	}
	        	setVendorPhoneNumber(phoneNumber, vPhoneNumber);
	        	if (vPhoneNumber.getVendorPhoneGeneratedIdentifier() == null) {
	            	vDetail.getVendorPhoneNumbers().add(vPhoneNumber);
	            	vendor.getVendorPhoneNumbers().add(new VendorPhoneNumber()); 
	      		
	        	}
        	// TODO : what to do with those existing contacts, but not passed from request
   		
	    	}
    	}
	}

	private void updateVendorSupplierDiversitys(List<VendorBatchSupplierDiversity> supplierDiversitys, VendorDetail vendor, VendorDetail vDetail) {
    	ArrayList<VendorSupplierDiversity> vendorSupplierDiversitys = new ArrayList<VendorSupplierDiversity>();
    	if (CollectionUtils.isNotEmpty(supplierDiversitys)) {
	    	for (VendorBatchSupplierDiversity diversity : supplierDiversitys) {
				LOG.info("updateVendor diversity " + diversity);
	    		VendorSupplierDiversity vDiversity = getVendorSupplierDiversity(vDetail.getVendorHeader(), diversity.getVendorSupplierDiversityCode());
	            boolean isExist = StringUtils.isNotBlank(vDiversity.getVendorSupplierDiversityCode());
	            vDiversity.setVendorSupplierDiversityCode(diversity.getVendorSupplierDiversityCode());

	            ((CuVendorSupplierDiversityExtension)vDiversity.getExtension()).setVendorSupplierDiversityExpirationDate(new java.sql.Date(getFormatDate(diversity.getVendorSupplierDiversityExpirationDate()).getTime()));
	            vDiversity.setActive(StringUtils.equalsIgnoreCase("Y", diversity.getActive()));
	            if (!isExist) {
	            	vDetail.getVendorHeader().getVendorSupplierDiversities().add(vDiversity);
	            	vendor.getVendorHeader().getVendorSupplierDiversities().add(new VendorSupplierDiversity());
	            }
	   		
	    	}
    	}
	}

	private VendorPhoneNumber getVendorPhoneNumber(VendorDetail vDetail, Integer vendorPhoneNumberGeneratedIdentifier) {
    	if (CollectionUtils.isNotEmpty(vDetail.getVendorPhoneNumbers())) {
		for (VendorPhoneNumber vPhoneNumber : vDetail.getVendorPhoneNumbers()) {
			if (vendorPhoneNumberGeneratedIdentifier.equals(vPhoneNumber.getVendorPhoneGeneratedIdentifier())) {
				return vPhoneNumber;
			}
		}
    	}
		return new VendorPhoneNumber();
	}
	
	private VendorSupplierDiversity getVendorSupplierDiversity(VendorHeader vHeader, String supplierDiversityCode) {
    	if (CollectionUtils.isNotEmpty(vHeader.getVendorSupplierDiversities())) {
			for (VendorSupplierDiversity vSupplierDiversity : vHeader.getVendorSupplierDiversities()) {
				if (StringUtils.equals(vSupplierDiversity.getVendorSupplierDiversityCode(), supplierDiversityCode)) {
					return vSupplierDiversity;
				}
			}
    	}
		return new VendorSupplierDiversity();
	}

	private void setVendorPhoneNumber(VendorBatchPhoneNumber phoneNumber,VendorPhoneNumber vPhoneNumber) {
    	vPhoneNumber.setVendorPhoneTypeCode(phoneNumber.getVendorPhoneTypeCode());
    	vPhoneNumber.setVendorPhoneNumber(phoneNumber.getVendorPhoneNumber());
    	vPhoneNumber.setVendorPhoneExtensionNumber(phoneNumber.getVendorPhoneExtensionNumber());
    	vPhoneNumber.setActive(StringUtils.equalsIgnoreCase("Y", phoneNumber.getActive()));
	}
	
	private Date getFormatDate(String stringDate) {
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
        Date date = null;
        try {
            date = format.parse(stringDate);
        } catch (Exception e) {
        	LOG.error("parse date error : " + e.getMessage());
        }
        return date;
	}

	private List<VendorPhoneNumber> getVendorPhoneNumbers(List<VendorBatchPhoneNumber> phoneNumbers) {
    	List<VendorPhoneNumber> vendorPhoneNumbers = new ArrayList<VendorPhoneNumber>();
    	if (CollectionUtils.isNotEmpty(phoneNumbers)) {
	    	for (VendorBatchPhoneNumber phoneNumber : phoneNumbers) {
		        LOG.info("addVendor phoneNumber " + phoneNumber);       
				VendorPhoneNumber vPhoneNumber = new VendorPhoneNumber();
				setVendorPhoneNumber(phoneNumber, vPhoneNumber);
		    	vendorPhoneNumbers.add(vPhoneNumber);
	   		
	    	}
    	}
    	return vendorPhoneNumbers;
	}
	
	private List<VendorSupplierDiversity> getVendorSupplierDiversities(List<VendorBatchSupplierDiversity> supplierDiversitys) {
    	List<VendorSupplierDiversity> vendorSupplierDiversitys = new ArrayList<VendorSupplierDiversity>();
    	if (CollectionUtils.isNotEmpty(supplierDiversitys)) {
	    	for (VendorBatchSupplierDiversity diversity : supplierDiversitys) {
		        LOG.info("addVendor diversity " + diversity);       
	    		VendorSupplierDiversity vDiversity = new VendorSupplierDiversity();
	
	            vDiversity.setVendorSupplierDiversityCode(diversity.getVendorSupplierDiversityCode());
	            ((CuVendorSupplierDiversityExtension)vDiversity.getExtension()).setVendorSupplierDiversityExpirationDate(new java.sql.Date(getFormatDate(diversity.getVendorSupplierDiversityExpirationDate()).getTime()));
	            vDiversity.setActive(StringUtils.equalsIgnoreCase("Y", diversity.getActive()));
	            vendorSupplierDiversitys.add(vDiversity);
	   		
	    	}
    	}
    	return vendorSupplierDiversitys;
	}

	public void setAttachmentService(AttachmentService attachmentService) {
		this.attachmentService = attachmentService;
	}

	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

	public void setDateTimeService(DateTimeService dateTimeService) {
		this.dateTimeService = dateTimeService;
	}

	public void setConfigurationService(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

	public void setDocumentService(DocumentService documentService) {
		this.documentService = documentService;
	}

	public void setMaintenanceDocumentService(
			MaintenanceDocumentService maintenanceDocumentService) {
		this.maintenanceDocumentService = maintenanceDocumentService;
	}

}
