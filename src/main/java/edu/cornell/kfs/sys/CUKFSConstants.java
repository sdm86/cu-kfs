package edu.cornell.kfs.sys;

import org.kuali.kfs.sys.ParameterKeyConstants;
import org.kuali.rice.core.util.JSTLConstants;

public class CUKFSConstants extends JSTLConstants implements ParameterKeyConstants {
        
        public static final String COMMODITY_CODE_FILE_TYPE_INDENTIFIER = "commodityCodeInputFileType";
        
        public static class PreEncumbranceDocumentConstants {
                public static final String BIWEEKLY = "biWeekly";
                public static final String CUSTOM = "custom";
                public static final String SEMIMONTHLY = "semiMonthly";
                public static final String MONTHLY = "monthly";
        }
        
        public static class PreEncumbranceSourceAccountingLineConstants {
                public static final String END_DATE = "endDate";
                public static final String START_DATE = "startDate";
                public static final String AUTO_DISENCUMBER_TYPE = "autoDisEncumberType";
                public static final String PARTIAL_TRANSACTION_COUNT = "partialTransactionCount";
                public static final String PARTIAL_AMOUNT = "partialAmount";
        }
        // KFSPTS-1960
        public static final String CURLY_BRACKET_LEFT = "{";
        public static final String CURLY_BRACKET_RIGHT = "}";
   
        //KFSPTS-2400
    	public static final String STAGING_DIR = "staging";
    	public static final String LD_DIR = "ld";
    	public static final String ENTERPRISE_FEED_DIR = "enterpriseFeed";
        public static final String ENCUMBRANCE_FILE_MARKER = "ENCUM";
        public static final String DISENCUMBRANCE_FILE_MARKER = "DISENCUM";
        
}
