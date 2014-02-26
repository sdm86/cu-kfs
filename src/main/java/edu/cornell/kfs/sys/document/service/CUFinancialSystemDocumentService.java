package edu.cornell.kfs.sys.document.service;

import org.kuali.kfs.sys.document.AccountingDocument;

public interface CUFinancialSystemDocumentService extends org.kuali.kfs.sys.document.service.FinancialSystemDocumentService{

    public void checkAccountingLinesForChanges(AccountingDocument accountingDocument);
    
    
    // KFSPTS-3057 add a new method that allows us to provide as input the from and to documents for which to compare accounting lines
    /**
     * Compares and logs changes in accounting lines for given from and to documents.
     * 
     * @param fromAccountingDocument
     * @param toAccountingDocument
     */
    public void checkAccountingLinesForChanges(AccountingDocument fromAccountingDocument, AccountingDocument toAccountingDocument);


    // KFSPTS-3083
    /**
     * Compares and logs changes in given accounting lines.
     * 
     * @param savedDoc
     * @param savedSourceAcctLines
     * @param savedTargetAcctLines
     * @param accountingDocument
     * @param sourceAccountingLines
     * @param targetAccountingLines
     */
    public void checkAccountingLinesForChanges(AccountingDocument savedDoc, List<AccountingLine> savedSourceAcctLines, List<AccountingLine> savedTargetAcctLines, AccountingDocument accountingDocument, List<AccountingLine> sourceAccountingLines, List<AccountingLine> targetAccountingLines);
	
}
