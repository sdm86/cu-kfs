/**
 * @author cab379
 */
package edu.cornell.kfs.module.receiptProcessing.service;

import org.kuali.kfs.sys.batch.BatchInputFileType;

public interface ReceiptProcessingService {

    /**
     * Validates and parses all files ready to go in the batch staging area.
     * @return
     */
    public boolean loadFiles();

    /**
     * 
     */
    public boolean loadFile(String fileName, BatchInputFileType batchInputFileType);
           
    /**
     * 
     * Provide a file name generation for the CustomerInputFileType(csv) 
     * 
     * @return
     */
    public String getFileName(String principalName, String fileUserIdentifer, String prefix, String delim);
}

