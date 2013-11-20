/*
 * Copyright 2009 The Kuali Foundation
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
package org.kuali.kfs.module.purap.document.workflow;

import org.kuali.rice.kew.actionitem.ActionItem;
import org.kuali.rice.kew.actionlist.CustomActionListAttribute;
import org.kuali.rice.kew.actionlist.DisplayParameters;
import org.kuali.rice.kew.actions.ActionSet;
import org.kuali.rice.kew.web.session.UserSession;

public class PurchaseOrderActionListAttribute implements CustomActionListAttribute {

    public DisplayParameters getDocHandlerDisplayParameters(UserSession arg0, ActionItem arg1) throws Exception {
        return null;
    }

    // KFSPTS-2107 : make FYI appear
    public ActionSet getLegalActions(UserSession arg0, ActionItem arg1) throws Exception {
    	ActionSet as = new ActionSet();
    	as.addFyi();
    	return as; 
    }
    
    
}