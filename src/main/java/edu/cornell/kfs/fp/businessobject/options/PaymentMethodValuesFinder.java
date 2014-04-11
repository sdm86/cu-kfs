/*
 * Copyright 2007 The Kuali Foundation
 * 
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.opensource.org/licenses/ecl2.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.cornell.kfs.fp.businessobject.options;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.rice.core.api.util.ConcreteKeyValue;
import org.kuali.rice.core.api.util.KeyValue;
import org.kuali.rice.krad.keyvalues.KeyValuesBase;
import org.kuali.rice.krad.service.BusinessObjectService;

import edu.cornell.kfs.fp.CuFPConstants;
import edu.cornell.kfs.fp.businessobject.PaymentMethod;

/**
 * This class returns list of payment method key value pairs.
 * 
 * Customization for UA: addition of "A" type for credit card payments.
 * 
 * @author jonathan
 * @see org.kuali.kfs.fp.businessobject.options.PaymentMethodValuesFinder
 */
public class PaymentMethodValuesFinder extends KeyValuesBase {
    private static BusinessObjectService businessObjectService;
    static protected Map<String,String> filterCriteria = new HashMap<String, String>();
    static {
        filterCriteria.put(CuFPConstants.ACTIVE, CuFPConstants.YES);
    }
    
    /*
     * @see org.kuali.keyvalues.KeyValuesFinder#getKeyValues()
     */
    public List<KeyValue> getKeyValues() {
        Collection<PaymentMethod> paymentMethods = getBusinessObjectService().findMatchingOrderBy(PaymentMethod.class, getFilterCriteria(),  "paymentMethodName",true);
        List<KeyValue> labels = new ArrayList<KeyValue>( paymentMethods.size() );       
        for ( PaymentMethod pm : paymentMethods ) {
            labels.add(new ConcreteKeyValue(pm.getPaymentMethodCode(), pm.getPaymentMethodCode() + " - " + pm.getPaymentMethodName()));
        }
        return labels;
    }

    protected BusinessObjectService getBusinessObjectService() {
        if ( businessObjectService == null ) {
            businessObjectService = SpringContext.getBean(BusinessObjectService.class);
        }
        return businessObjectService;
    }
    
    protected Map<String,String> getFilterCriteria() {
        return filterCriteria;
    }
    
}