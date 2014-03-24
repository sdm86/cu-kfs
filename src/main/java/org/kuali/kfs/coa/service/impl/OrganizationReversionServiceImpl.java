/*
 * Copyright 2006 The Kuali Foundation
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
package org.kuali.kfs.coa.service.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.kuali.kfs.coa.businessobject.OrganizationReversion;
import org.kuali.kfs.coa.businessobject.ReversionCategory;
import org.kuali.kfs.coa.dataaccess.OrganizationReversionDao;
import org.kuali.kfs.coa.service.OrganizationReversionService;
import org.kuali.kfs.gl.batch.service.ReversionCategoryLogic;
import org.kuali.kfs.gl.batch.service.impl.GenericReversionCategory;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.kfs.sys.service.NonTransactional;
import org.kuali.rice.kns.service.BusinessObjectService;

/**
 * 
 * This service implementation is the default implementation of the OrganizationReversion service that is delivered with Kuali.
 */

@NonTransactional
public class OrganizationReversionServiceImpl implements OrganizationReversionService {
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(OrganizationReversionServiceImpl.class);

    private OrganizationReversionDao organizationReversionDao;
    private BusinessObjectService businessObjectService;

    /**
     * @see org.kuali.kfs.coa.service.OrganizationReversionService#getByPrimaryId(java.lang.Integer, java.lang.String,
     *      java.lang.String)
     */
    public OrganizationReversion getByPrimaryId(Integer fiscalYear, String chartCode, String orgCode) {
        LOG.debug("getByPrimaryId() started");
        return organizationReversionDao.getByPrimaryId(fiscalYear, chartCode, orgCode);
    }

    /**
     * @see org.kuali.kfs.coa.service.OrganizationReversionService#getCategories()
     */
    public Map<String, ReversionCategoryLogic> getCategories() {
        LOG.debug("getCategories() started");

        Map<String, ReversionCategoryLogic> categories = new HashMap<String, ReversionCategoryLogic>();

        Collection cats = organizationReversionDao.getCategories();

        for (Iterator iter = cats.iterator(); iter.hasNext();) {
            ReversionCategory orc = (ReversionCategory) iter.next();

            String categoryCode = orc.getReversionCategoryCode();

            Map<String, ReversionCategoryLogic> beanMap = SpringContext.getBeansOfType(ReversionCategoryLogic.class);
            if (beanMap.containsKey("gl" + categoryCode + "OrganizationReversionCategory")) {
                LOG.info("Found Organization Reversion Category Logic for gl" + categoryCode + "OrganizationReversionCategory");
                categories.put(categoryCode, beanMap.get("gl" + categoryCode + "OrganizationReversionCategory"));
            }
            else {
                LOG.info("No Organization Reversion Category Logic for gl" + categoryCode + "OrganizationReversionCategory; using generic");
                GenericReversionCategory cat = SpringContext.getBean(GenericReversionCategory.class);
                cat.setCategoryCode(categoryCode);
                cat.setCategoryName(orc.getReversionCategoryName());
                categories.put(categoryCode, (ReversionCategoryLogic) cat);
            }
        }
        return categories;
    }

    /**
     * 
     * @see org.kuali.kfs.coa.service.OrganizationReversionService#getCategoryList()
     */
    public List<ReversionCategory> getCategoryList() {
        LOG.debug("getCategoryList() started");

        return organizationReversionDao.getCategories();
    }

    /**
     * @see org.kuali.kfs.coa.service.OrganizationReversionService#isCategoryActive(java.lang.String)
     */
    public boolean isCategoryActive(String categoryCode) {
        Map<String, Object> pkMap = new HashMap<String, Object>();
        pkMap.put("reversionCategoryCode", categoryCode);
        final ReversionCategory category = (ReversionCategory)businessObjectService.findByPrimaryKey(ReversionCategory.class, pkMap);
        if (category == null) return false;
        return category.isActive();
    }

    /**
     * @see org.kuali.kfs.coa.service.OrganizationReversionService#isCategoryActiveByName(java.lang.String)
     */
    public boolean isCategoryActiveByName(String categoryName) {
        Map<String, Object> fieldMap = new HashMap<String, Object>();
        fieldMap.put("reversionCategoryName", categoryName);
        final Collection categories = businessObjectService.findMatching(ReversionCategory.class, fieldMap);
        final Iterator categoriesIterator = categories.iterator();
        ReversionCategory category = null;
        while (categoriesIterator.hasNext()) {
            category = (ReversionCategory)categoriesIterator.next();
        }
        if (category == null) return false;
        return category.isActive();
    }

    /**
     * 
     * This method injects the OrganizationReversionDao
     * @param orgDao
     */
    public void setOrganizationReversionDao(OrganizationReversionDao orgDao) {
        organizationReversionDao = orgDao;
    }
    
    /**
     * Sets an implementation of the business object service
     * @param boService the implementation of the BusinessObjectService to set
     */
    public void setBusinessObjectService(BusinessObjectService boService) {
        this.businessObjectService = boService;
    }
}