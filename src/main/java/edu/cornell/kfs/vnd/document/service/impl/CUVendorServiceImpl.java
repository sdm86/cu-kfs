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
package edu.cornell.kfs.vnd.document.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.kfs.vnd.VendorConstants;
import org.kuali.kfs.vnd.VendorPropertyConstants;
import org.kuali.kfs.vnd.businessobject.VendorAddress;
import org.kuali.kfs.vnd.businessobject.VendorContract;
import org.kuali.kfs.vnd.businessobject.VendorContractOrganization;
import org.kuali.kfs.vnd.businessobject.VendorDefaultAddress;
import org.kuali.kfs.vnd.businessobject.VendorDetail;
import org.kuali.kfs.vnd.businessobject.VendorHeader;
import org.kuali.kfs.vnd.businessobject.VendorRoutingComparable;
import org.kuali.kfs.vnd.dataaccess.VendorDao;
import org.kuali.kfs.vnd.document.service.VendorService;
import org.kuali.kfs.vnd.document.service.impl.VendorServiceImpl;
import org.kuali.rice.kew.exception.WorkflowException;
import org.kuali.rice.kim.bo.Person;
import org.kuali.rice.kim.service.PersonService;
import org.kuali.rice.kns.document.MaintenanceDocument;
import org.kuali.rice.kns.service.BusinessObjectService;
import org.kuali.rice.kns.service.DocumentService;
import org.kuali.rice.kns.service.PersistenceService;
import org.kuali.rice.kns.util.KualiDecimal;
import org.kuali.rice.kns.util.ObjectUtils;
import org.springframework.transaction.annotation.Transactional;

import edu.cornell.kfs.vnd.document.service.CUVendorService;

@Transactional
public class CUVendorServiceImpl extends VendorServiceImpl implements CUVendorService {
    private static Logger LOG = Logger.getLogger(CUVendorServiceImpl.class);

    private BusinessObjectService businessObjectService;
    /**
     *  @see org.kuali.kfs.vnd.document.service.VendorService#getVendorByDunsNumber(String)
     */
    public VendorDetail getVendorByVendorName(String vendorName) {
        LOG.debug("Entering getVendorByVendorName for vendorName:" + vendorName);
        Map criteria = new HashMap();
        criteria.put(VendorPropertyConstants.VENDOR_NAME, vendorName);
        List<VendorDetail> vds = (List) businessObjectService.findMatching(VendorDetail.class, criteria);
        LOG.debug("Exiting getVendorByVendorName.");
        if (vds.size() < 1) {
            return null;
        }
        else {
            return vds.get(0);
        }
    }

    public void setBusinessObjectService(BusinessObjectService boService) {
        this.businessObjectService = boService;
    }

}
