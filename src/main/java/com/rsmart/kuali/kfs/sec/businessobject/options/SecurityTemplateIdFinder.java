/*
 * Copyright 2007 The Kuali Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rsmart.kuali.kfs.sec.businessobject.options;

import java.util.ArrayList;
import java.util.List;

import org.kuali.rice.core.util.KeyLabelPair;
import org.kuali.rice.kns.lookup.keyvalues.KeyValuesBase;

import com.rsmart.kuali.kfs.sec.SecConstants;

/**
 * Returns list of security template Ids
 */
public class SecurityTemplateIdFinder extends KeyValuesBase {

    /**
     * @see org.kuali.keyvalues.KeyValuesFinder#getKeyValues()
     */
    public List getKeyValues() {
        List activeLabels = new ArrayList();

        activeLabels.add(new KeyLabelPair(SecConstants.SecurityTemplateIds.INQUIRY_FIELD_VALUE, "Balance Inquiry"));
        activeLabels.add(new KeyLabelPair(SecConstants.SecurityTemplateIds.EDIT_ACCOUNTING_LINE_FIELD_VALUE, "Edit Accounting Line"));
        activeLabels.add(new KeyLabelPair(SecConstants.SecurityTemplateIds.EDIT_DOCUMENT_FIELD_VALUE, "Edit Document"));
        activeLabels.add(new KeyLabelPair(SecConstants.SecurityTemplateIds.LOOKUP_FIELD_VALUE, "Lookup Records"));
        activeLabels.add(new KeyLabelPair(SecConstants.SecurityTemplateIds.VIEW_ACCOUNTING_LINE_FIELD_VALUE, "View Accounting Line"));
        activeLabels.add(new KeyLabelPair(SecConstants.SecurityTemplateIds.VIEW_DOCUMENT_FIELD_VALUE, "View Document"));
        activeLabels.add(new KeyLabelPair(SecConstants.SecurityTemplateIds.VIEW_NOTES_ATTACHMENTS_FIELD_VALUE, "View Notes/Attachments"));

        return activeLabels;
    }
}