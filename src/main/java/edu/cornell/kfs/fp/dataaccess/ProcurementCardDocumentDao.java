package edu.cornell.kfs.fp.dataaccess;

import java.sql.Date;

import org.kuali.kfs.fp.document.ProcurementCardDocument;

public interface ProcurementCardDocumentDao {
    public void save(ProcurementCardDocument document);
    public ProcurementCardDocument getDocument(String fdocNbr);
    public ProcurementCardDocument getDocumentByCarhdHolderAmountDateVendor(String cardHolder, String amount, Date transactionDate, String vendorName);
}
