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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.log4j.Logger;
import org.kuali.kfs.vnd.VendorPropertyConstants;
import org.kuali.kfs.vnd.businessobject.VendorAddress;
import org.kuali.kfs.vnd.businessobject.VendorDetail;
import org.kuali.kfs.vnd.businessobject.VendorHeader;
import org.kuali.kfs.vnd.businessobject.VendorRoutingComparable;
import org.kuali.kfs.vnd.businessobject.VendorSupplierDiversity;
import org.kuali.kfs.vnd.businessobject.lookup.VendorLookupableHelperServiceImpl;
import org.kuali.kfs.vnd.document.service.impl.VendorServiceImpl;
import org.kuali.rice.krad.bo.PersistableBusinessObjectBase;
import org.kuali.rice.krad.service.BusinessObjectService;
import org.kuali.rice.krad.service.DocumentService;
import org.springframework.transaction.annotation.Transactional;

import edu.cornell.kfs.vnd.businessobject.CuVendorAddressExtension;
import edu.cornell.kfs.vnd.businessobject.CuVendorHeaderExtension;
import edu.cornell.kfs.vnd.businessobject.CuVendorSupplierDiversityExtension;
import edu.cornell.kfs.vnd.businessobject.VendorDetailExtension;
import edu.cornell.kfs.vnd.document.service.CUVendorService;

@Transactional
public class CUVendorServiceImpl extends VendorServiceImpl implements CUVendorService {
    private static Logger LOG = Logger.getLogger(CUVendorServiceImpl.class);

    private BusinessObjectService businessObjectService;
    private VendorLookupableHelperServiceImpl vendorLookupableHelperServiceImpl;
    protected DocumentService documentService;

    /**
     *  @see org.kuali.kfs.vnd.document.service.VendorService#getVendorByDunsNumber(String)
     */
    public VendorDetail getVendorByVendorName(String vendorName) {
        LOG.info("Entering getVendorByVendorName for vendorName:" + vendorName);
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

    /**
     * 
     */
    public VendorDetail getVendorByNamePlusLastFourOfTaxID(String vendorName, String lastFour) {
        LOG.debug("Entering getVendorByNamePlusLastFourOfTaxID for vendorName:" + vendorName + ", last four :" + lastFour);

//        Map criteria = new HashMap();
//        criteria.put(VendorPropertyConstants.VENDOR_NAME, vendorName);
//        List<VendorDetail> vds = (List) businessObjectService.findMatching(VendorDetail.class, criteria);
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put(VendorPropertyConstants.VENDOR_NAME, "*"+vendorName+"*");
        vendorLookupableHelperServiceImpl.setBusinessObjectClass(VendorDetail.class);
        List<VendorDetail> vds = (List) vendorLookupableHelperServiceImpl.getSearchResults(attributes);
        
        List<VendorDetail> matches = new ArrayList<VendorDetail>();
        
        for(VendorDetail detail : vds) {
        	String taxId = detail.getVendorHeader().getVendorTaxNumber();
        	if(StringUtils.isNotBlank(taxId)) {
        		String compareTo = taxId.substring(taxId.length()-4);
        		if(StringUtils.equals(lastFour, compareTo)) {
        			matches.add(detail);
        		}
        	}
        }
        
        LOG.debug("Exiting getVendorByNamePlusLastFourOfTaxID.");

        // TODO : not sure about this.  vendor may have division and have the same tax id ?
        // if > 1, then should we also return matches.get(0) ?
        
        if(matches.size() > 1 || matches.isEmpty()) {
        	return null;
        }
        
        return matches.get(0);
    }
    
    public void setBusinessObjectService(BusinessObjectService boService) {
        this.businessObjectService = boService;
        // TODO : FIX need this to set up super, other wise boservice will be null in vendorserviceimpl
        super.setBusinessObjectService(boService);
    }

    public void setVendorLookupableHelperServiceImpl(VendorLookupableHelperServiceImpl vendorLookupableHelperServiceImpl) {
        this.vendorLookupableHelperServiceImpl = vendorLookupableHelperServiceImpl;
    }

    public VendorHeader getVendorByEin(String vendorEin) {
        LOG.info("Entering getVendorByEin f:" );
        Map criteria = new HashMap();
        criteria.put(VendorPropertyConstants.VENDOR_TAX_NUMBER_ONLY, vendorEin);
        criteria.put("VNDR_TAX_TYP_CD", "FEIN");
        List<VendorHeader> vds = (List) businessObjectService.findMatching(VendorHeader.class, criteria);
        LOG.debug("Exiting getVendorByEin.");
        if (vds.size() < 1) {
            return null;
        }
        else {
            return vds.get(0);
        }
    }

    //TODO UPGRADE-911
    public VendorAddress getVendorDefaultAddress(List<VendorAddress> addresses,
        String addressType, String campus) {
      return null;
    }


    private List<VendorRoutingComparable> convertToRountingComparable(List<? extends PersistableBusinessObjectBase> vendorCollection) {
        List<VendorRoutingComparable> retList = new ArrayList<VendorRoutingComparable>();
        if (CollectionUtils.isNotEmpty(vendorCollection)) {
            for (PersistableBusinessObjectBase pbo : vendorCollection) {
                retList.add((VendorRoutingComparable)pbo);                
            }
        }
        return retList;
    }
   
    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
        super.setDocumentService(documentService);
   }
}

