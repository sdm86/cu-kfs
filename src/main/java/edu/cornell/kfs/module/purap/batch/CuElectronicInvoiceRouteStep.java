package edu.cornell.kfs.module.purap.batch;
import java.util.Date;

import org.kuali.kfs.module.purap.service.ElectronicInvoiceHelperService;
import org.kuali.kfs.sys.batch.AbstractStep;

import edu.cornell.kfs.module.purap.service.CuElectronicInvoiceHelperService;

public class CuElectronicInvoiceRouteStep extends AbstractStep {
    
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(CuElectronicInvoiceRouteStep.class);

    private ElectronicInvoiceHelperService electronicInvoiceHelperService;

    public boolean execute(String jobName, Date jobRunDate) {
    	try {
    		Thread.sleep(60000);
    	} catch (Exception e) {}
        return ((CuElectronicInvoiceHelperService)electronicInvoiceHelperService).routeDocuments();
       
    }

    public void setElectronicInvoiceHelperService(ElectronicInvoiceHelperService electronicInvoiceHelperService) {
        this.electronicInvoiceHelperService = electronicInvoiceHelperService;
    }

}
