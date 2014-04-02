package edu.cornell.kfs.module.sharepoint.batch.vo;

import java.sql.Date;



public class ReceiptProcessing  {

    /**
     * @author cab379
     */
    
    private String cardHolder;
    private String vendor;
    private String amount;
    private String purchasedate;
    private String SharePointPath;
    private String filename;
        

    public String getCardHolder() {
        return cardHolder;
    }

    public void setCardHolder(String cardHolder) {
        this.cardHolder = cardHolder;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getPurchasedate() {
        return purchasedate;
    }

    public void setPurchasedate(String purchasedate) {
        this.purchasedate = purchasedate;
    }

    public String returnProcessFail() {
        String line = this.getCardHolder() + "," + this.getVendor() + "," + this.getAmount() + "," + this.getPurchasedate() + "," + this.getSharePointPath() + "," + this.getFilename() + "," + "0\n";
        return line;        
    }
    
    public String returnProcessSucceed() {
        String line = this.getCardHolder() + "," + this.getVendor() + "," + this.getAmount() + "," + this.getPurchasedate() + "," + this.getSharePointPath() + "," + this.getFilename() + ",";
        return line;        
    }

    public String getSharePointPath() {
        return SharePointPath;
    }

    public void setSharePointPath(String sharePointPath) {
        SharePointPath = sharePointPath;
    }

}
