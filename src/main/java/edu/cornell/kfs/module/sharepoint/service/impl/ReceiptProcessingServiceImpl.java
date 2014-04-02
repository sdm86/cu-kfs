/**
 * @author cab379
 */
package edu.cornell.kfs.module.sharepoint.service.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.kuali.kfs.fp.document.ProcurementCardDocument;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.kfs.sys.batch.BatchInputFileType;
import org.kuali.kfs.sys.batch.service.BatchInputFileService;
import org.kuali.kfs.sys.exception.ParseException;
import org.kuali.rice.kim.bo.Person;
import org.kuali.rice.kim.service.PersonService;
import org.kuali.rice.kns.bo.Attachment;
import org.kuali.rice.kns.bo.Note;
import org.kuali.rice.kns.service.AttachmentService;
import org.kuali.rice.kns.service.DateTimeService;
import org.kuali.rice.kns.service.NoteService;
import org.kuali.rice.kns.util.KNSConstants;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;

import edu.cornell.kfs.fp.dataaccess.ProcurementCardDocumentDao;
import edu.cornell.kfs.module.sharepoint.batch.vo.ReceiptProcessing;
import edu.cornell.kfs.module.sharepoint.service.ReceiptProcessingService;

public class ReceiptProcessingServiceImpl implements ReceiptProcessingService {
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(ReceiptProcessingServiceImpl.class);
    

    private static final String WORKFLOW_DOC_ID_PREFIX = " - WITH WORKFLOW DOCID: ";
    
    private BatchInputFileService batchInputFileService;    
    private ProcurementCardDocumentDao procurementCardDocumentDao;
    private DateTimeService dateTimeService; 
    private List<BatchInputFileType> batchInputFileTypes;
    private String outputDirectory;
    private String pdfDirectory;
    private AttachmentService attachmentService;
    private NoteService noteService;
    private PersonService personService;
    
    public ReceiptProcessingServiceImpl() {
    }
    
    /**
     * @see org.kuali.kfs.module.ar.batch.service.ReceiptLoadService#loadFiles()
     */
    public boolean loadFiles() {
        
        LOG.info("Beginning processing of all available files for Receipt Batch Upload.");
        
        boolean result = true;
        
                       
        //  create a list of the files to process
         Map<String, BatchInputFileType> fileNamesToLoad = getListOfFilesToProcess();
        LOG.info("Found " + fileNamesToLoad.size() + " file(s) to process.");
        
        //  process each file in turn
        List<String> processedFiles = new ArrayList<String>();
        for (String inputFileName : fileNamesToLoad.keySet()) {
            
            LOG.info("Beginning processing of filename: " + inputFileName + ".");
            
            
            
            if (loadFile(inputFileName, fileNamesToLoad.get(inputFileName))) {
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

    /**
    */
    public boolean loadFile(String fileName, BatchInputFileType batchInputFileType) {
        
        boolean result = true;
        
        //  load up the file into a byte array 
        byte[] fileByteContent = safelyLoadFileBytes(fileName);

        //  parse the file against the XSD schema and load it into an object
        LOG.info("Attempting to parse the file using Apache Digester.");
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
        
        
        
        StringBuilder processResults = new StringBuilder();
        processResults.append("\"cardHolder\",\"vendor\",\"amount\",\"purchasedate\",\"SharePointPath\",\"filename\",\"Success\"\n");        
       
        List<ReceiptProcessing> receipts =  ((List<ReceiptProcessing>) parsedObject);
        final String attachmentsPath = pdfDirectory;
        String mimeTypeCode = "pdf";
        
        for (ReceiptProcessing receipt  : receipts) {                                   
            Note note = new Note();

            java.util.Date pdate = null;
            DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
            try
            {
                pdate = (java.util.Date) df.parse(receipt.getPurchasedate());
            }
            catch(ParseException e)
            {
                processResults.append(receipt.returnProcessFail()); 
                e.printStackTrace();
                continue;
            } catch (java.text.ParseException e) {
                processResults.append(receipt.returnProcessFail()); 
                e.printStackTrace();
                continue;
            }
            Date pdateSQL = null;
            if (pdate != null) {             
                pdateSQL = new Date(pdate.getTime());
            }
            ProcurementCardDocument pcdo = procurementCardDocumentDao.getDocumentByCarhdHolderAmountDateVendor(receipt.getCardHolder(), receipt.getAmount(), pdateSQL, receipt.getVendor());
            //ProcurementCardDocument pcdo = procurementCardDocumentDao.getDocument("3397880");
            
            if (pcdo == null) {
                processResults.append(receipt.returnProcessFail());
                continue;
            } 
            String pdfFileName = attachmentsPath + "/" + receipt.getFilename();
            
            File f = null;
            FileInputStream fileInputStream = null;
            try {
                f = new File(pdfFileName);
                fileInputStream = new FileInputStream(pdfFileName);                                
            }
            catch (FileNotFoundException e) {
                LOG.error("file not found for Document " + pcdo.getDocumentNumber());
                processResults.append(receipt.returnProcessFail());
                e.printStackTrace();
                continue;
            }
            catch (IOException e1) {
                LOG.error("generic Io exception for Document " + pcdo.getDocumentNumber());
                processResults.append(receipt.returnProcessFail());
                continue;
            }
            
            long fileSizeLong = f.length();          
            Integer fileSize = Integer.parseInt(Long.toString(fileSizeLong));
    
            String attachType = "";
            Attachment noteAttachment = null;
            try {
                noteAttachment = attachmentService.createAttachment(pcdo.getDocumentHeader(), pdfFileName, mimeTypeCode , fileSize, fileInputStream, attachType);
            } catch (IOException e) {
                LOG.error("Failed to attache file for Document " + pcdo.getDocumentNumber());
                processResults.append(receipt.returnProcessFail());               
                e.printStackTrace();
                continue;
            }
            
            if (noteAttachment != null) {
                note.setNoteText("Sharepoint receipt2");
                note.addAttachment(noteAttachment);
                note.setRemoteObjectIdentifier(pcdo.getDocumentHeader().getObjectId());
                note.setAuthorUniversalIdentifier(getSystemUser().getPrincipalId());
                note.setNoteTypeCode(KNSConstants.NoteTypeEnum.BUSINESS_OBJECT_NOTE_TYPE.getCode());               
                note.setNotePostedTimestamp(dateTimeService.getCurrentTimestamp());
                                
                try {
                    noteService.save(note);
                } catch (Exception e) {
                    LOG.error("Failed to save note for Document " + pcdo.getDocumentNumber());
                    processResults.append(receipt.returnProcessFail());               
                    e.printStackTrace();
                    continue;
                }
                
                LOG.info("Attached pdf for document " + pcdo.getDocumentNumber());
                processResults.append(receipt.returnProcessSucceed() + pcdo.getDocumentNumber() +"\n");                
            }
            
            
        }
        
        String outputCsv = processResults.toString();
        getcsvWriter(outputCsv);       
                
        return result;
    }  
    
    
    protected void getcsvWriter(String csvDoc) {
        
        String reportDropFolder = outputDirectory + "/";
        String fileName = "CIT_OUT_" +
            new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(dateTimeService.getCurrentDate()) + ".csv";
        
         //  setup the writer
         File reportFile = new File(reportDropFolder + fileName);
         BufferedWriter writer = null;
         try {
             writer = new BufferedWriter(new FileWriter(reportFile));
             writer.write(csvDoc);
             writer.close();
         } catch (IOException e1) {
             LOG.error("IOException when trying to write report file");
             e1.printStackTrace();
         }
         
                         
     }
    
 
    protected byte[] safelyLoadFileBytes(String fileName) {
        
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
    

    
   
    
    protected void writeMessageEntryLines(Document pdfDoc, List<String[]> messageLines) {
        Font font = FontFactory.getFont(FontFactory.COURIER, 8, Font.NORMAL);
        
        Paragraph paragraph;
        String messageEntry;
        for (String[] messageLine : messageLines) {
            paragraph = new Paragraph();
            paragraph.setAlignment(Element.ALIGN_LEFT);
            messageEntry = StringUtils.rightPad(messageLine[0], (12 - messageLine[0].length()), " ") + " - " + messageLine[1].toUpperCase();
            paragraph.add(new Chunk(messageEntry, font));

            //  blank line
            paragraph.add(new Chunk("", font));
            
            try {
                pdfDoc.add(paragraph);
            }
            catch (DocumentException e) {
                LOG.error("iText DocumentException thrown when trying to write content.", e);
                throw new RuntimeException("iText DocumentException thrown when trying to write content.", e);
            }
        }
    }        
    
    public void setBatchInputFileTypes(List<BatchInputFileType> batchInputFileType) {
        this.batchInputFileTypes = batchInputFileType;
    }

    public void setBatchInputFileService(BatchInputFileService batchInputFileService) {
        this.batchInputFileService = batchInputFileService;
    }
    
    public void setDateTimeService(DateTimeService dateTimeService) {
        this.dateTimeService = dateTimeService;
    }

    public void setprocurementCardDocumentDao(ProcurementCardDocumentDao procurementCardDocumentDao) {
        this.procurementCardDocumentDao = procurementCardDocumentDao;
    }
    
    public void setAttachmentService(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }
    
   

    /**    
     */
    
    public String getFileName(String principalName, String fileUserIdentifer, String prefix, String delim) {

        //  start with the batch-job-prefix
        StringBuilder fileName = new StringBuilder(delim);
        
        //  add the logged-in user name if there is one, otherwise use a sensible default
        fileName.append(delim + principalName);
        
        //  if the user specified an identifying lable, then use it
        if (StringUtils.isNotBlank(fileUserIdentifer)) {
            fileName.append(delim + fileUserIdentifer);
        }
        
        //  stick a timestamp on the end
        fileName.append(delim + dateTimeService.toString(dateTimeService.getCurrentTimestamp(), "yyyyMMdd_HHmmss"));

        //  stupid spaces, begone!
        return StringUtils.remove(fileName.toString(), " ");
    }

    /**
     * LOG error and throw RunTimeException
     * 
     * @param errorMessage
     */
    private void criticalError(String errorMessage){
        LOG.error(errorMessage);
        throw new RuntimeException(errorMessage);
    }
    
    /**
     * @see org.kuali.kfs.sys.batch.InitiateDirectoryBase#getRequiredDirectoryNames()
     */
    public List<String> getRequiredDirectoryNames() {
        List<String> directoryNames = new ArrayList<String>();
        for (BatchInputFileType batchInputFileType : batchInputFileTypes){
            directoryNames.add(batchInputFileType.getDirectoryPath());
        }
        return directoryNames;
    }       

    public NoteService getNoteService() {
        return noteService;
    }

    public void setNoteService(NoteService noteService) {
        this.noteService = noteService;
    }

    public Person getSystemUser() {
        return personService.getPersonByPrincipalName(KFSConstants.SYSTEM_USER);
    }
    
    /**
     * @return Returns the personService.
     */
    protected PersonService getPersonService() {
        return personService;
    }

    /**
     * @param personService The personService to set.
     */
    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }

    public String getPdfDirectory() {
        return pdfDirectory;
    }

    public void setPdfDirectory(String pdfDirectory) {
        this.pdfDirectory = pdfDirectory;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
              
    
}

