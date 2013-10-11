package edu.cornell.kfs.sys;

public class CUKFSConstants {
        
    public static final String COMMODITY_CODE_FILE_TYPE_INDENTIFIER = "commodityCodeInputFileType";
    public static final String ACCOUNT_MAINTENANCE_DOCUMENT_TYPE_DD_KEY = "AccountMaintenanceDocument";
    public static final String EMPLOYEE_RETIRED_STATUS = "R";
    public static final String KUALI_FREQUENCY_LOOKUPABLE_IMPL = "frequencyCodeLookupable";
    //cannot use SOURCE_ACCOUNTING_LINE_ERROR_PATTERN due to doubled error displayed in checking already added source accounting line
    public static final String NEW_SOURCE_LINE_ERRORS = "newSourceLine*";    
    public static final String ACCOUNT_RESTRICTED_STATUS_CODE = "R";
    public static final String CASH_REVERSION_OBJECT_CD = "CASH_REVERSION_OBJECT_CD";
    public static final String RULE_CODE_CA = "CA";
    
    public static class SysKimApiConstants {
        public static final String ESHOP_USER_ROLE_NAME = "eShop User (cu)";
        public static final String ESHOP_SUPER_USER_ROLE_NAME = "eShop Plus User(cu)";
        public static final String CONTRACTS_AND_GRANTS_PROCESSOR = "Contracts & Grants Processor";
    }       
    public static final class TaxRegionConstants {
        public static final String CREATE_TAX_REGION_FROM_LOOKUP_PARM = "createTaxRegionFromLookup";
    }
    public static final class ReportGeneration {
        public final static String SIP_EXPORT_FILE_NAME = "sip_export.txt";
        public final static String SIP_EXPORT_FILE_NAME_EXECUTIVES = "sip_export_executives.txt";
    }        
    public static final class FinancialDocumentTypeCodes {
        public static final String PAYMENT_APPLICATION = "APP";
        
    }        
    public static class ParameterNamespaces {
        public static final String ENDOWMENT = "KFS-ENDOW";
        public static final String PURCHASING = "KFS-PURAP";
    }
    public static class DocumentTypeAttributes {
        public static final String ACCOUNTING_DOCUMENT_TYPE_NAME = "ACCT";
    }
    
    public static class ObjectCodeConstants {
        public static final String PARAMETER_KC_ENABLE_RESEARCH_ADMIN_OBJECT_CODE_ATTRIBUTE_IND = "ENABLE_RESEARCH_ADMIN_OBJECT_CODE_ATTRIBUTE_IND";
    }
      
    // KFSUPGRADE-72 Account Reversion
    public static class Reversion {
        public static final String VALID_PREFIX = "EXTENDED_DEFINITIONS_INCLUDE_";
        public static final String INVALID_PREFIX = "EXTENDED_DEFINITIONS_EXCLUDE_";
        public static final String IS_EXPENSE_PARAM = "EXTENDED_DEFINITIONS_EXPENSE_CATEGORIES";
        public static final String OBJECT_CONSOL_PARAM_SUFFIX = "OBJECT_CONSOLIDATIONS_BY_REVERSION_CATEGORY";
        public static final String OBJECT_LEVEL_PARAM_SUFFIX = "OBJECT_LEVELS_BY_REVERSION_CATEGORY";
        public static final String OBJECT_TYPE_PARAM_SUFFIX = "OBJECT_TYPES_BY_REVERSION_CATEGORY";
        public static final String OBJECT_SUB_TYPE_PARAM_SUFFIX = "OBJECT_SUB_TYPES_BY_REVERSION_CATEGORY";
    }
    
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
    
    // I Want document constants
    public static final String I_WANT_DOC_ITEM_TAB_ERRORS = "document.item*,newIWantItemLine*";
    public static final String I_WANT_DOC_ACCOUNT_TAB_ERRORS = "newSourceLine*,document.account*";
    public static final String I_WANT_DOC_VENDOR_TAB_ERRORS = "document.vendor*";
    public static final String I_WANT_DOC_ORDER_COMPLETED_TAB_ERRORS = "document.completeOption";
    
    //KFSPTS-1460
    public static final String SEMICOLON = ";";
    
        
}
