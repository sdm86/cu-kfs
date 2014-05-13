package edu.cornell.kfs.module.purap.document.web.struts;

import org.kuali.kfs.module.purap.document.web.struts.PaymentRequestForm;

public class CuPaymentRequestForm extends PaymentRequestForm {
	
    // KFSPTS-1891
    protected String wireChargeMessage;
    
    public CuPaymentRequestForm() {
		super();
	}
    

	public String getWireChargeMessage() {
		return wireChargeMessage;
	}

	public void setWireChargeMessage(String wireChargeMessage) {
		this.wireChargeMessage = wireChargeMessage;
	}

}
