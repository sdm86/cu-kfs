package edu.cornell.kfs.module.ld.businessobject;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.kuali.kfs.sys.batch.BatchFile;
import org.kuali.kfs.sys.batch.BatchFileUtils;
import org.kuali.kfs.sys.businessobject.lookup.BatchFileLookupableHelperServiceImpl;
import org.kuali.rice.kns.bo.BusinessObject;
import org.kuali.rice.kns.lookup.HtmlData;
import org.kuali.rice.kns.lookup.HtmlData.AnchorHtmlData;
import org.kuali.rice.kns.util.KNSConstants;
import org.kuali.rice.kns.util.UrlFactory;

import edu.cornell.kfs.sys.CUKFSConstants;

public class LaborLedgerBatchFileLookupableHelperServiceImpl extends BatchFileLookupableHelperServiceImpl {

    /**
     * Override method so that it only returns the /staging/ld/enterpriseFeed directory as selected.
     * 
     * @see org.kuali.kfs.sys.businessobject.lookup.BatchFileLookupableHelperServiceImpl#getSelectedPaths()
     */
    @Override
    protected String[] getSelectedPaths() {
        String[] selectedPaths = new String[1];
        String path = CUKFSConstants.STAGING_DIR + System.getProperty("file.separator") + CUKFSConstants.LD_DIR + System.getProperty("file.separator") + CUKFSConstants.ENTERPRISE_FEED_DIR;
        selectedPaths[0] = path;
        return selectedPaths;
    }

    /**
     * Override to add "Create Disencumbrance" link to the Actions section
     * 
     * @see org.kuali.kfs.sys.businessobject.lookup.BatchFileLookupableHelperServiceImpl#getCustomActionUrls(org.kuali.rice.kns.bo.BusinessObject,
     *      java.util.List)
     */
    public List<HtmlData> getCustomActionUrls(BusinessObject businessObject, List pkNames) {
        List<HtmlData> links = new ArrayList<HtmlData>();

        BatchFile batchFile = (BatchFile) businessObject;

        if (canCreateDisencumbrance(batchFile)) {
            links.add(getCreateDisencumbranceUrl(batchFile));
        }

        links.addAll(super.getCustomActionUrls(businessObject, pkNames));

        return links;
    }

    /**
     * Override to use laborLedgerBatchFileAdmin action
     * 
     * @see org.kuali.kfs.sys.businessobject.lookup.BatchFileLookupableHelperServiceImpl#getDownloadUrl(org.kuali.kfs.sys.batch.BatchFile)
     */
    protected HtmlData getDownloadUrl(BatchFile batchFile) {
        Properties parameters = new Properties();
        parameters.put("filePath", BatchFileUtils.pathRelativeToRootDirectory(batchFile.retrieveFile().getAbsolutePath()));
        parameters.put(KNSConstants.DISPATCH_REQUEST_PARAMETER, "download");
        String href = UrlFactory.parameterizeUrl("../laborLedgerBatchFileAdmin.do", parameters);
        return new AnchorHtmlData(href, "download", "Download");
    }

    /**
     * Override to use laborLedgerBatchFileAdmin action
     * 
     * @see org.kuali.kfs.sys.businessobject.lookup.BatchFileLookupableHelperServiceImpl#getDeleteUrl(org.kuali.kfs.sys.batch.BatchFile)
     */
    protected HtmlData getDeleteUrl(BatchFile batchFile) {
        Properties parameters = new Properties();
        parameters.put("filePath", BatchFileUtils.pathRelativeToRootDirectory(batchFile.retrieveFile().getAbsolutePath()));
        parameters.put(KNSConstants.DISPATCH_REQUEST_PARAMETER, "delete");
        String href = UrlFactory.parameterizeUrl("../laborLedgerBatchFileAdmin.do", parameters);
        return new AnchorHtmlData(href, "delete", "Delete");
    }

    /**
     * Creates link for "Create Disencumbrance" action.
     * 
     * @param batchFile
     *            the encumbrance file for which the disencumbrance file will be created
     * @return link for "Create Disencumbrance" action.
     */
    protected HtmlData getCreateDisencumbranceUrl(BatchFile batchFile) {
        Properties parameters = new Properties();
        parameters.put("filePath", BatchFileUtils.pathRelativeToRootDirectory(batchFile.retrieveFile().getAbsolutePath()));
        parameters.put(KNSConstants.DISPATCH_REQUEST_PARAMETER, "disencumber");
        String href = UrlFactory.parameterizeUrl("../laborLedgerBatchFileAdmin.do", parameters);
        return new AnchorHtmlData(href, "disencumber", "Create Disencumbrance");
    }

    /**
     * Checks if the create disencumbrance link should be displayed. It should only be displayed if the current file is a
     * .data file and the user has the permission to delete this file.
     * 
     * @param batchFile
     * @return true if it can be displayed, false otherwise
     */
    protected boolean canCreateDisencumbrance(BatchFile batchFile) {
        String fileName = batchFile.getFileName();
        if (fileName.contains(CUKFSConstants.ENCUMBRANCE_FILE_MARKER) && !fileName.contains(CUKFSConstants.DISENCUMBRANCE_FILE_MARKER) && batchFile.getFileName().endsWith(".data") && canDeleteFile(batchFile)) {
            return true;
        } else
            return false;
    }
}
