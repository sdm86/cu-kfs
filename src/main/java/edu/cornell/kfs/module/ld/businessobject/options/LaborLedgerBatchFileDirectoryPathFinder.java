package edu.cornell.kfs.module.ld.businessobject.options;

import org.kuali.rice.kns.lookup.valueFinder.ValueFinder;

import edu.cornell.kfs.sys.CUKFSConstants;

public class LaborLedgerBatchFileDirectoryPathFinder implements ValueFinder {

    /**
     * @see org.kuali.rice.kns.lookup.valueFinder.ValueFinder#getValue()
     */
    public String getValue() {
        String path = CUKFSConstants.STAGING_DIR + System.getProperty("file.separator") + CUKFSConstants.LD_DIR + System.getProperty("file.separator") + CUKFSConstants.ENTERPRISE_FEED_DIR;

        return path;
    }
}