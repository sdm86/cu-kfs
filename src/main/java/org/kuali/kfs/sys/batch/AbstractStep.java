/*
 * Copyright 2007 The Kuali Foundation
 * 
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.opensource.org/licenses/ecl2.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.kfs.sys.batch;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.kuali.rice.kns.service.DateTimeService;
import org.kuali.rice.kns.service.ParameterService;
import org.springframework.beans.factory.BeanNameAware;

public abstract class AbstractStep implements Step, BeanNameAware {
    private String name;
    private ParameterService parameterService;
    private DateTimeService dateTimeService;
    private boolean interrupted = false;

    /**
     * Sets the bean name
     * 
     * @param name String that contains the bean name
     * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
     */
    public void setBeanName(String name) {
        this.name = name;
    }

    /**
     * Gets the name attribute.
     * 
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    protected ParameterService getParameterService() {
        return parameterService;
    }

    public void setParameterService(ParameterService parameterService) {
        this.parameterService = parameterService;
    }

    /**
     * Gets the dateTimeService attribute.
     * 
     * @return Returns the dateTimeService.
     */
    protected DateTimeService getDateTimeService() {
        return dateTimeService;
    }

    /**
     * Sets the dateTimeService attribute value.
     * 
     * @param dateTimeService The dateTimeService to set.
     */
    public void setDateTimeService(DateTimeService dateTimeService) {
        this.dateTimeService = dateTimeService;
    }

    /**
     * Returns the boolean value of the interrupted flag
     * 
     * @return boolean
     * @see org.kuali.kfs.sys.batch.Step#isInterrupted()
     */
    public boolean isInterrupted() {
        return interrupted;
    }

    /**
     * Sets the interrupted flag
     * 
     * @param interrupted
     * @see org.kuali.kfs.sys.batch.Step#setInterrupted(boolean)
     */
    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }

    /**
     * Initializes the interrupted flag
     * 
     * @see org.kuali.kfs.sys.batch.Step#interrupt()
     */
    public void interrupt() {
        this.interrupted = true;
    }

	/**
	 * This method takes the file provided and modifies the name of that file to include a date and timestamp.
	 * @param origFile File to be renamed
	 * @param fileName Original file's name
	 * @param directory Directory path where the file will reside
	 */
	protected void addTimeStampToFileName(File origFile, String fileName, String directory) {
		String fileNameProper = fileName.substring(0, fileName.lastIndexOf('.'));
		String fileExtension = fileName.substring(fileName.lastIndexOf('.'));

		DateFormat df = new SimpleDateFormat("MMddyyyy_hhmmss");
		
		File newFile = new File(directory+File.separator+fileNameProper+"_"+df.format(new Date())+fileExtension);
		origFile.renameTo(newFile);
		
	}

}