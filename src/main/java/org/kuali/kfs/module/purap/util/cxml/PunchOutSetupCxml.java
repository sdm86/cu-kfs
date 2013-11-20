/*
 * Copyright 2005-2009 The Kuali Foundation
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
/*
 * Created on Mar 10, 2005
 *
 */
package org.kuali.kfs.module.purap.util.cxml;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.kuali.kfs.module.purap.PurapConstants;
import org.kuali.kfs.module.purap.businessobject.B2BInformation;
import org.kuali.kfs.module.purap.document.service.impl.B2BPurchaseOrderSciquestServiceImpl;
import org.kuali.kfs.module.purap.util.PurApDateFormatUtils;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.rice.kim.bo.Person;
import org.kuali.rice.kim.service.RoleManagementService;
import org.kuali.rice.kns.service.DateTimeService;
import org.kuali.rice.kns.util.GlobalVariables;

public class PunchOutSetupCxml {
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(PunchOutSetupCxml.class);
  private Person user;
  private B2BInformation b2bInformation;

  public PunchOutSetupCxml(Person u,B2BInformation b) {
    user = u;
    b2bInformation = b;
  }

  /**
   * Get cxml punch out request xml
   * @return xml for punch out request
   */
  public String getPunchOutSetupRequestMessage() {
    StringBuffer cxml = new StringBuffer();
    Date d = SpringContext.getBean(DateTimeService.class).getCurrentDate();
    SimpleDateFormat date = PurApDateFormatUtils.getSimpleDateFormat(PurapConstants.NamedDateFormats.CXML_SIMPLE_DATE_FORMAT);
    SimpleDateFormat time = PurApDateFormatUtils.getSimpleDateFormat(PurapConstants.NamedDateFormats.CXML_SIMPLE_TIME_FORMAT);

    // doing as two parts b/c they want a T instead of space
    // between them, and SimpleDateFormat doesn't allow putting the
    // constant "T" in the string

    cxml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    cxml.append("<!DOCTYPE cXML SYSTEM \"cXML.dtd\">\n");
    cxml.append("<cXML payloadID=\"irrelevant\" xml:lang=\"en-US\" timestamp=\"").append(date.format(d)).append("T")
        .append(time.format(d)).append("-05:00").append("\">\n");

    // note that timezone is hard coded b/c this is the format
    // they wanted, but SimpleDateFormat returns -0500, so rather than
    // parse it just hard-coded

    cxml.append("  <Header>\n");
    cxml.append("    <From>\n");
    cxml.append("      <Credential domain=\"NetworkId\">\n");
    cxml.append("        <Identity>KualiDemo</Identity>\n");
    cxml.append("      </Credential>\n");
    cxml.append("    </From>\n");
    cxml.append("    <To>\n");
    cxml.append("      <Credential domain=\"DUNS\">\n");
    cxml.append("        <Identity>KualiDemo</Identity>\n");
    cxml.append("      </Credential>\n");
    cxml.append("      <Credential domain=\"internalsupplierid\">\n");
    cxml.append("        <Identity>1016</Identity>\n");
    cxml.append("      </Credential>\n");
    cxml.append("    </To>\n");
    cxml.append("    <Sender>\n");
    cxml.append("      <Credential domain=\"TOPSNetworkUserId\">\n");
    cxml.append("        <Identity>").append(user.getPrincipalName().toUpperCase()).append("</Identity>\n");
    cxml.append("        <SharedSecret>").append(b2bInformation.getPassword()).append("</SharedSecret>\n");
    cxml.append("      </Credential>\n");
    cxml.append("      <UserAgent>").append(b2bInformation.getUserAgent()).append("</UserAgent>\n");
    cxml.append("    </Sender>\n");
    cxml.append("  </Header>\n");
    cxml.append("  <Request deploymentMode=\"").append(b2bInformation.getEnvironment()).append("\">\n");
    cxml.append("    <PunchOutSetupRequest operation=\"create\">\n");
    cxml.append("      <BuyerCookie>").append(user.getPrincipalName().toUpperCase()).append("</BuyerCookie>\n");
    cxml.append("      <Extrinsic name=\"UserEmail\">").append(user.getEmailAddressUnmasked()).append("</Extrinsic>\n"); 
    cxml.append("      <Extrinsic name=\"UniqueName\">").append(user.getPrincipalName().toUpperCase()).append("</Extrinsic>\n");
    cxml.append("      <Extrinsic name=\"PhoneNumber\">").append(user.getPhoneNumberUnmasked()).append("</Extrinsic>\n");
    cxml.append("      <Extrinsic name=\"Department\">").append(user.getCampusCode()).append(user.getPrimaryDepartmentCode()).append("</Extrinsic>\n");
    cxml.append("      <Extrinsic name=\"Campus\">").append(user.getCampusCode()).append("</Extrinsic>\n");
    // KFSPTS-1720
    cxml.append("      <Extrinsic name=\"FirstName\">").append(user.getFirstName()).append("</Extrinsic>\n");
    cxml.append("      <Extrinsic name=\"LastName\">").append(user.getLastName()).append("</Extrinsic>\n");
    cxml.append("      <Extrinsic name=\"Role\">").append(getPreAuthValue(user.getPrincipalId())).append("</Extrinsic>\n");
    cxml.append("      <BrowserFormPost>\n");
    cxml.append("        <URL>").append(b2bInformation.getPunchbackURL()).append("</URL>\n");
    cxml.append("      </BrowserFormPost>\n");
//    cxml.append("      <Contact role=\"endUser\">\n");
//    cxml.append("        <Name xml:lang=\"en\">").append(user.getName()).append("</Name>\n");
//    cxml.append("        <Email>").append(user.getEmailAddressUnmasked()).append("</Email>\n"); 
//    cxml.append("        <PhoneNumber>").append(user.getPhoneNumberUnmasked()).append("</PhoneNumber>\n"); 
//    cxml.append("      </Contact>\n");
    cxml.append("      <SupplierSetup>\n");
    cxml.append("        <URL>").append(b2bInformation.getPunchoutURL()).append("</URL>\n");
    cxml.append("      </SupplierSetup>\n");
    cxml.append("    </PunchOutSetupRequest>\n");
    cxml.append("  </Request>\n");
    cxml.append("</cXML>\n");

    LOG.info("Punch out cxml "+ cxml.toString());
    
    return cxml.toString();
  }

  // KFSPTS-1720
  private String getPreAuthValue(String principalId) {
	  try {
		List<String> roleIds = new ArrayList<String>();
		roleIds.add(getRoleManagementService().getRoleIdByName(KFSConstants.ParameterNamespaces.PURCHASING,KFSConstants.SysKimConstants.ESHOP_USER_ROLE_NAME));
		roleIds.add(getRoleManagementService().getRoleIdByName(KFSConstants.ParameterNamespaces.PURCHASING,KFSConstants.SysKimConstants.ESHOP_SUPER_USER_ROLE_NAME));
		if (getRoleManagementService().principalHasRole(principalId,roleIds, null)) {
			return "Preauthorized";
		}
		return "NonPreauthorized";
	  } catch (Exception e) {
		  // incase something goes wrong.  continue to process
		  LOG.info("error from role check " + e.getMessage());
		  return "Preauthorized";
	  }
  }
  
  private RoleManagementService getRoleManagementService() {
	  return SpringContext.getBean(RoleManagementService.class);
  }

}
