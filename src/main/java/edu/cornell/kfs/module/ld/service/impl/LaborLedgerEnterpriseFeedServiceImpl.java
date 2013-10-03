package edu.cornell.kfs.module.ld.service.impl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.lang.StringUtils;
import org.kuali.kfs.sys.KFSConstants;

import edu.cornell.kfs.module.ld.service.LaborLedgerEnterpriseFeedService;

public class LaborLedgerEnterpriseFeedServiceImpl implements LaborLedgerEnterpriseFeedService {

    private static final String TRANSACTION_SIGN = "+";
    public static final int FDOC_NBR_START_INDEX = 37;
    public static final int FDOC_NBR_END_INDEX = 51;
    private static final int TRANSACTION_SIGN_START_INDEX = 114;
    private static final int TRANSACTION_SIGN_END_INDEX = 115;
    private static final int CREDIT_DEBIT_CODE_START_INDEX = 135;
    private static final int CREDIT_DEBIT_CODE_END_INDEX = 136;
    public static final String FDOC_REF_TYP_CD = "PAYE";
    public static final int FDOC_REF_TYP_CD_START_INDEX = 164;
    public static final int FDOC_REF_TYP_CD_END_INDEX = 168;
    public static final String FS_REF_ORIGIN_CD = "P4";
    public static final int FS_REF_ORIGIN_CD_START_INDEX = 168;
    public static final int FS_REF_ORIGIN_CD_END_INDEX = 170;
    public static final int FDOC_REF_NBR_START_INDEX = 170;
    public static final int FDOC_REF_NBR_END_INDEX = 184;
    public static final String TRN_ENCUM_UPDT_CD = "R";
    public static final int TRN_ENCUM_UPDT_CD_START_INDEX = 194;
    public static final int TRN_ENCUM_UPDT_CD_END_INDEX = 195;
    public static final int LD_ENTERPRISE_FEED_LINE_LENGTH = 296;

    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(LaborLedgerEnterpriseFeedServiceImpl.class);

    /**
     * 
     * @see edu.cornell.kfs.module.ld.service.LaborLedgerEnterpriseFeedService#createDisencumbrance(java.io.InputStream)
     */
    public InputStream createDisencumbrance(InputStream encumbranceFile) {

        InputStream disencumbranceStream = null;
        if (encumbranceFile == null) {

            return null;
        }

        ByteArrayOutputStream disencEntriesPrintStream = new ByteArrayOutputStream();
        try {

            InputStream fStream = encumbranceFile;
            BufferedReader in = new BufferedReader(new InputStreamReader(fStream));

            while (in.ready()) {
                String currentLine = in.readLine();

                if (currentLine != null && currentLine.length() == LD_ENTERPRISE_FEED_LINE_LENGTH) {
                    String resultLine = currentLine;

                    resultLine = StringUtils.overlay(resultLine, TRANSACTION_SIGN, TRANSACTION_SIGN_START_INDEX, TRANSACTION_SIGN_END_INDEX);

                    if (KFSConstants.GL_CREDIT_CODE.equalsIgnoreCase(StringUtils.substring(currentLine, CREDIT_DEBIT_CODE_START_INDEX, CREDIT_DEBIT_CODE_END_INDEX))) {
                        resultLine = StringUtils.overlay(resultLine, KFSConstants.GL_DEBIT_CODE, CREDIT_DEBIT_CODE_START_INDEX, CREDIT_DEBIT_CODE_END_INDEX);
                    } else if (KFSConstants.GL_DEBIT_CODE.equalsIgnoreCase(StringUtils.substring(currentLine, CREDIT_DEBIT_CODE_START_INDEX, CREDIT_DEBIT_CODE_END_INDEX))) {
                        resultLine = StringUtils.overlay(resultLine, KFSConstants.GL_CREDIT_CODE, CREDIT_DEBIT_CODE_START_INDEX, CREDIT_DEBIT_CODE_END_INDEX);
                    }

                    resultLine = StringUtils.overlay(resultLine, FDOC_REF_TYP_CD, FDOC_REF_TYP_CD_START_INDEX, FDOC_REF_TYP_CD_END_INDEX);
                    resultLine = StringUtils.overlay(resultLine, FS_REF_ORIGIN_CD, FS_REF_ORIGIN_CD_START_INDEX, FS_REF_ORIGIN_CD_END_INDEX);
                    resultLine = StringUtils.overlay(resultLine, TRN_ENCUM_UPDT_CD, TRN_ENCUM_UPDT_CD_START_INDEX, TRN_ENCUM_UPDT_CD_END_INDEX);

                    String fDocNbr = resultLine.substring(FDOC_NBR_START_INDEX, FDOC_NBR_END_INDEX);
                    resultLine = StringUtils.overlay(resultLine, fDocNbr, FDOC_REF_NBR_START_INDEX, FDOC_REF_NBR_END_INDEX);
                    resultLine = resultLine.concat(KFSConstants.NEWLINE);

                    disencEntriesPrintStream.write((resultLine.getBytes()));

                } else {
                    LOG.info("Input file has an invalid format");
                    in.close();
                    disencEntriesPrintStream.close();
                    return null;
                }
            }
            
            in.close();
            disencEntriesPrintStream.flush();
            disencumbranceStream = new ByteArrayInputStream(disencEntriesPrintStream.toByteArray());
            disencEntriesPrintStream.close();
            return disencumbranceStream;
            
        } catch (FileNotFoundException e) {
            LOG.info("Error creating PrintStream to write invalid entries");
        } catch (IOException e) {
            LOG.info("Error creating the disencumbrance file");
        }

        return disencumbranceStream;
    }

}
