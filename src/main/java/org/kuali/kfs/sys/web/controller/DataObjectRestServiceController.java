/*
 * Copyright 2014 The Kuali Foundation.
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
package org.kuali.kfs.sys.web.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.kuali.kfs.sys.businessobject.datadictionary.FinancialSystemBusinessObjectEntry;
import org.kuali.kfs.sys.businessobject.lookup.LookupableSpringContext;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.kfs.sys.service.impl.KfsParameterConstants;
import org.kuali.kfs.sys.util.RestXmlUtil;
import org.kuali.rice.core.api.config.property.ConfigContext;
import org.kuali.rice.core.api.util.type.TypeUtils;
import org.kuali.rice.coreservice.framework.parameter.ParameterService;
import org.kuali.rice.kim.api.KimConstants;
import org.kuali.rice.kim.api.permission.PermissionService;
import org.kuali.rice.kim.api.services.KimApiServiceLocator;
import org.kuali.rice.kns.datadictionary.InquirySectionDefinition;
import org.kuali.rice.kns.datadictionary.LookupDefinition;
import org.kuali.rice.kns.lookup.LookupableHelperService;
import org.kuali.rice.krad.UserSession;
import org.kuali.rice.krad.bo.BusinessObject;
import org.kuali.rice.krad.service.BusinessObjectService;
import org.kuali.rice.krad.service.DataDictionaryService;
import org.kuali.rice.krad.service.KRADServiceLocator;
import org.kuali.rice.krad.service.KRADServiceLocatorWeb;
import org.kuali.rice.krad.service.PersistenceStructureService;
import org.kuali.rice.krad.service.XmlObjectSerializerService;
import org.kuali.rice.krad.util.GlobalVariables;
import org.kuali.rice.krad.util.KRADConstants;
import org.kuali.rice.krad.util.KRADUtils;
import org.kuali.rice.krad.util.ObjectUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class DataObjectRestServiceController {

    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(DataObjectRestServiceController.class);

    private DataDictionaryService dataDictionaryService;
    private PersistenceStructureService persistenceStructureService;
    private ParameterService parameterService;
    private PermissionService permissionService;
    private BusinessObjectService businessObjectService;

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "Not authorized.")
    public void handleAccessDeniedException(AccessDeniedException ex, HttpServletResponse response) {
    }

    @ExceptionHandler(NoSuchBeanDefinitionException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Data object not found.")
    public void handleNoSuchBeanDefinitionException(NoSuchBeanDefinitionException ex, HttpServletResponse response) {
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Unexpected exception has occured.")
    public String handleRuntimeException(RuntimeException ex, HttpServletResponse response) {
        return ExceptionUtils.getStackTrace(ex);
    }

    @RequestMapping(value = "/{namespace}/{dataobject}.json", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> getDataObjectsAsJSON(@PathVariable("namespace") String namespace, @PathVariable("dataobject") String dataobject, HttpServletRequest request) throws Exception {
        FinancialSystemBusinessObjectEntry boe = getBusinessObject(dataobject);
        validateRequest(boe, namespace, dataobject, request);

        try {
            List<Map<String, String>> resultMap = generateResultMap(request, boe);

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(SerializationConfig.Feature.WRAP_ROOT_VALUE, true);

            String jsonData = null;
            if (resultMap.size() == 1) {
                jsonData = mapper.defaultPrettyPrintingWriter()
                        .writeValueAsString(resultMap.get(0))
                        .replaceFirst(HashMap.class.getSimpleName(), boe.getBusinessObjectClass().getName());
            } else {
                jsonData = mapper.defaultPrettyPrintingWriter()
                        .writeValueAsString(resultMap)
                        .replaceFirst(ArrayList.class.getSimpleName(), ArrayList.class.getSimpleName()+"<"+boe.getBusinessObjectClass().getName()+">");
            }

            return new ResponseEntity<String>(jsonData, HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("Unexpected exception has occured.", e);
            throw new RuntimeException("Unexpected exception has occured.");
        }
    }

    @RequestMapping(value = "/{namespace}/{dataobject}.xml", method = RequestMethod.GET, produces = "application/xml")
    @ResponseBody
    public ResponseEntity<String> getDataObjectsAsXML(@PathVariable("namespace") String namespace, @PathVariable("dataobject") String dataobject, HttpServletRequest request) throws Exception {
        FinancialSystemBusinessObjectEntry boe = getBusinessObject(dataobject);
        validateRequest(boe, namespace, dataobject, request);

        try {
            List<Map<String, String>> resultMap = generateResultMap(request, boe);

            String xml = null;
            if (resultMap.size() == 1) {
                xml = RestXmlUtil.toXML(boe, resultMap.get(0));
            } else {
                xml = RestXmlUtil.toXML(boe, resultMap);
            }

            return new ResponseEntity<String>(xml, HttpStatus.OK);
        } catch (Exception e) {
            LOG.error("Unexpected exception has occured.", e);
            throw new RuntimeException("Unexpected exception has occured.");
        }
    }

    protected List<Map<String, String>> generateResultMap(HttpServletRequest request, FinancialSystemBusinessObjectEntry boe) {
        List<? extends BusinessObject> results = getSearchResults(request, boe);
        List<String> inquiryFields = getInquiryFields(boe);

        List<Map<String, String>> resultMap = new ArrayList<Map<String, String>>();
        for (BusinessObject bo : results) {
            Map<String, String> objectMap = new HashMap<String, String>();
            Object object = ObjectUtils.createNewObjectFromClass(boe.getBusinessObjectClass());
            for (String propertyName : inquiryFields) {
                Object propertyValue = ObjectUtils.getPropertyValue(bo, propertyName);
                Class<?> propertyType = ObjectUtils.getPropertyType(bo, propertyName, getPersistenceStructureService());
                if (isPropertyTypeValid(propertyType)) {
                    objectMap.put(propertyName, propertyValue + "");
                }
            }

            resultMap.add(objectMap);
        }

        return resultMap;
    }

    protected boolean isAuthorized(FinancialSystemBusinessObjectEntry boe) throws Exception {
        if (boe != null) {
	        return getPermissionService().isAuthorizedByTemplate( GlobalVariables.getUserSession().getPrincipalId(), KRADConstants.KNS_NAMESPACE, KimConstants.PermissionTemplateNames.LOOK_UP_RECORDS, KRADUtils.getNamespaceAndComponentSimpleName(boe.getBusinessObjectClass()), Collections.<String, String> emptyMap());
        }

        return false;
    }

    protected boolean isPropertyTypeValid(Class<?> propertyType) {
        if (propertyType != null && (TypeUtils.isStringClass(propertyType)
                || TypeUtils.isIntegralClass(propertyType)
                || TypeUtils.isDecimalClass(propertyType)
                || TypeUtils.isTemporalClass(propertyType)
                || TypeUtils.isBooleanClass(propertyType))) {
            return true;
        }

        return false;
    }

    protected List<String> getInquiryFields(FinancialSystemBusinessObjectEntry boe) {
        List<String> inquiryFields = new ArrayList<String>();
        for (InquirySectionDefinition section : boe.getInquiryDefinition().getInquirySections()) {
            inquiryFields.addAll(section.getInquiryFieldNames());
        }

        return inquiryFields;
    }

    protected List<? extends BusinessObject> getSearchResults(HttpServletRequest request, FinancialSystemBusinessObjectEntry boe) {
        Map<String, String> fieldValues = new HashMap<String, String>();
        for (Object o : request.getParameterMap().keySet()) {
            String[] value = (String[]) request.getParameterMap().get(o);
            fieldValues.put(o.toString(), value[0]);
        }

        LookupDefinition lookupDefinition = boe.getLookupDefinition();
        if (lookupDefinition.getLookupableID() != null) {
            LookupableHelperService lookupableHelperService = getLookupableHelperService(lookupDefinition.getLookupableID());
            lookupableHelperService.setBusinessObjectClass(boe.getBusinessObjectClass());
            return lookupableHelperService.getSearchResults(fieldValues);
        } else { // if lookupable definiton exists but lookupable id is not set
            return new ArrayList(getBusinessObjectService().findMatching(boe.getBusinessObjectClass(), fieldValues));
        }
    }

    protected void validateRequest(FinancialSystemBusinessObjectEntry boe, String namespace, String dataobject, HttpServletRequest request) throws Exception {
        // check for https (will be ignored in dev mode), authorization
        if ((!ConfigContext.getCurrentContextConfig().getDevMode() && !request.isSecure())) {
            LOG.debug("HTTPS check failed.");
            throw new AccessDeniedException("Not authorized.");
        }

        if (boe == null) {
            LOG.debug("BusinessObjectEntry is null.");
            throw new NoSuchBeanDefinitionException("Data object not found.");
        }

        Boolean isModuleLocked = getParameterService().getParameterValueAsBoolean(namespace, KfsParameterConstants.PARAMETER_ALL_DETAIL_TYPE, KRADConstants.SystemGroupParameterNames.OLTP_LOCKOUT_ACTIVE_IND);
        boolean notAuthorized = !isAuthorized(boe);
        boolean moduleIsLocked = isModuleLocked != null && isModuleLocked;
        boolean noInquiryDefinition = !boe.hasInquiryDefinition();

        if (notAuthorized || moduleIsLocked || noInquiryDefinition) {
            LOG.debug("notAuthorized: " + notAuthorized);
            LOG.debug("moduleIsLocked: " + moduleIsLocked);
            LOG.debug("noInquiryDefinition: " + noInquiryDefinition);

            throw new AccessDeniedException("Not authorized.");
        }
    }

    protected FinancialSystemBusinessObjectEntry getBusinessObject(String dataobject) {
        try {
            return (FinancialSystemBusinessObjectEntry) getDataDictionaryService().getDictionaryObject(dataobject);
        } catch (NoSuchBeanDefinitionException e) {
            LOG.debug("Failed to retrieve data dictionary object.", e);
        }

        return null;
    }

    protected LookupableHelperService getLookupableHelperService(String lookupableID) {
        LookupableHelperService lookupableHelperService = LookupableSpringContext.getLookupable(lookupableID).getLookupableHelperService();
        return lookupableHelperService;
    }

    public DataDictionaryService getDataDictionaryService() {
        if (this.dataDictionaryService == null) {
            this.dataDictionaryService = KRADServiceLocatorWeb.getDataDictionaryService();
        }
        return this.dataDictionaryService;
    }

    public void setDataDictionaryService(DataDictionaryService dataDictionaryService) {
        this.dataDictionaryService = dataDictionaryService;
    }

    public PersistenceStructureService getPersistenceStructureService() {
        if (persistenceStructureService == null) {
            persistenceStructureService = KRADServiceLocator.getPersistenceStructureService();
        }
        return persistenceStructureService;
    }

    public void setPersistenceStructureService(PersistenceStructureService persistenceStructureService) {
        this.persistenceStructureService = persistenceStructureService;
    }

    public ParameterService getParameterService() {
        if (parameterService == null) {
            parameterService = SpringContext.getBean(ParameterService.class);
        }
        return parameterService;
    }

    public void setParameterService(ParameterService parameterService) {
        this.parameterService = parameterService;
    }

    public XmlObjectSerializerService getXmlObjectSerializerService() {
        return KRADServiceLocator.getXmlObjectSerializerService();
    }

    public PermissionService getPermissionService() {
        if (permissionService == null) {
            permissionService = KimApiServiceLocator.getPermissionService();
        }
        return permissionService;
    }

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public BusinessObjectService getBusinessObjectService() {
        if (businessObjectService == null) {
            businessObjectService = KRADServiceLocator.getBusinessObjectService();
        }
        return businessObjectService;
    }

    public void setBusinessObjectService(BusinessObjectService businessObjectService) {
        this.businessObjectService = businessObjectService;
    }

}
