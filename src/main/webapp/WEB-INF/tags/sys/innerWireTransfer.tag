<%--
Copyright 2007-2009 The Kuali Foundation

Licensed under the Educational Community License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.opensource.org/licenses/ecl2.php

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
--%>
<%@ include file="/jsp/sys/kfsTldHeader.jsp"%>

<c:set var="wireTransAttributes" value="${DataDictionary.PaymentSourceWireTransfer.attributes}" />
<c:set var="wireTransAttributes2" value="${DataDictionary.DisbursementVoucherWireTransferExtendedAttribute.attributes}" />

<table cellpadding=0 class="datatable" summary="Wire Transfer Section">
	<tbody>
		<tr>
			<td colspan="4" align="left" valign="middle" class="datacell"><bean:write name="KualiForm" property="wireChargeMessage" /></td>
		</tr>
		<tr>
			<th scope=row class="bord-l-b"><div align="right"><kul:htmlAttributeLabel attributeEntry="${wireTransAttributes.automatedClearingHouseProfileNumber}"/></div></th>
			<td class="datacell">
				<kul:htmlControlAttribute attributeEntry="${wireTransAttributes.automatedClearingHouseProfileNumber}" property="document.wireTransfer.automatedClearingHouseProfileNumber" readOnly="${!fullEntryMode&&!wireEntryMode&&!frnEntryMode}"/>
			</td>
			<th scope=row class="bord-l-b"><div align="right"><kul:htmlAttributeLabel attributeEntry="${wireTransAttributes.wireTransferFeeWaiverIndicator}"/> </div></th>
			<td class="datacell">
				<kul:htmlControlAttribute attributeEntry="${wireTransAttributes.wireTransferFeeWaiverIndicator}" property="document.wireTransfer.wireTransferFeeWaiverIndicator" readOnly="${!wireEntryMode&&!frnEntryMode}"/>
			</td>
		</tr>
		<tr>
			<th scope=row class="bord-l-b"><div align="right"><kul:htmlAttributeLabel attributeEntry="${wireTransAttributes.bankName}"/></div></th>
			<td class="datacell">
				<kul:htmlControlAttribute attributeEntry="${wireTransAttributes.bankName}" property="document.wireTransfer.bankName" readOnly="${!fullEntryMode&&!wireEntryMode&&!frnEntryMode}"/>
			</td>
			<th scope=row class="bord-l-b"><div align="right"><kul:htmlAttributeLabel attributeEntry="${wireTransAttributes.additionalWireText}"/></div></th>
			<td class="datacell">
				<kul:htmlControlAttribute attributeEntry="${wireTransAttributes.additionalWireText}" property="document.wireTransfer.additionalWireText" readOnly="${!fullEntryMode&&!wireEntryMode&&!frnEntryMode}"/>
			</td>
		</tr>
		<tr>
			<th scope=row class="bord-l-b"><div align="right"><kul:htmlAttributeLabel attributeEntry="${wireTransAttributes.bankRoutingNumber}"/>
				<c:if test="${fullEntryMode||wireEntryMode||frnEntryMode}"><br/> *required for US bank</c:if>
			</th>
			<td class="datacell">
				<kul:htmlControlAttribute attributeEntry="${wireTransAttributes.bankRoutingNumber}" property="document.wireTransfer.bankRoutingNumber" readOnly="${!fullEntryMode&&!wireEntryMode&&!frnEntryMode}"/>
			</td>
			<th scope=row class="bord-l-b"><div align="right"><kul:htmlAttributeLabel attributeEntry="${wireTransAttributes.attentionLineText}"/></div></th>
			<td class="datacell">
				<kul:htmlControlAttribute attributeEntry="${wireTransAttributes.attentionLineText}" property="document.wireTransfer.attentionLineText" readOnly="${!fullEntryMode&&!wireEntryMode&&!frnEntryMode}"/>
			</td>
		</tr>
		
		<tr>
                <th scope=row class="bord-l-b" ><div align="right"><kul:htmlAttributeLabel attributeEntry="${wireTransAttributes2.disbVchrBankStreetAddress}"/>
                </div></th>
                <td class="datacell" >
                  <kul:htmlControlAttribute attributeEntry="${wireTransAttributes2.disbVchrBankStreetAddress}" property="document.wireTransfer.extension.disbVchrBankStreetAddress" readOnly="${!fullEntryMode&&!wireEntryMode&&!frnEntryMode}"/>
                </td>
                
       </tr>
		
		<tr>
			<th scope=row class="bord-l-b"><div align="right"><kul:htmlAttributeLabel attributeEntry="${wireTransAttributes.bankCityName}"/></div></th>
			<td class="datacell">
				<kul:htmlControlAttribute attributeEntry="${wireTransAttributes.bankCityName}" property="document.wireTransfer.bankCityName" readOnly="${!fullEntryMode&&!wireEntryMode&&!frnEntryMode}"/>
			</td>
			<th scope=row class="bord-l-b"><div align="right"><kul:htmlAttributeLabel attributeEntry="${wireTransAttributes.currencyTypeCode}"/></div></th>
			<c:set var="amountHint">
        			<bean:message key="label.wireTransfer.amount.hint"/>
                  </c:set>
			<td class="datacell">
				<kul:htmlControlAttribute accessibilityHint="${amountHint}" attributeEntry="${wireTransAttributes.currencyTypeCode}" property="document.wireTransfer.currencyTypeCode" readOnly="${!fullEntryMode&&!wireEntryMode&&!frnEntryMode}"/>
			</td>
		</tr>
		<tr>
			<th scope=row class="bord-l-b"><div align="right"><kul:htmlAttributeLabel attributeEntry="${wireTransAttributes.bankStateCode}"/>
				<c:if test="${fullEntryMode||wireEntryMode||frnEntryMode}"><br/> *required for US bank</c:if>
			</th>
			<td class="datacell">
				<kul:htmlControlAttribute attributeEntry="${wireTransAttributes.bankStateCode}" property="document.wireTransfer.bankStateCode" readOnly="${!fullEntryMode&&!wireEntryMode&&!frnEntryMode}"/>
			</td>
			<th scope=row class="bord-l-b" rowspan=2><div align="right"><kul:htmlAttributeLabel attributeEntry="${wireTransAttributes.currencyTypeName}"/></div></th>
			<td class="datacell" colspan="3" rowspan=2>
				<kul:htmlControlAttribute attributeEntry="${wireTransAttributes.currencyTypeName}" property="document.wireTransfer.currencyTypeName" readOnly="${!fullEntryMode&&!wireEntryMode&&!frnEntryMode}"/>
			</td>
		</tr>
		<tr>
			<th scope=row class="bord-l-b"><div align="right"><kul:htmlAttributeLabel attributeEntry="${wireTransAttributes.bankCountryCode}"/></div></th>
			<td class="datacell" colspan="3">
				<kul:htmlControlAttribute attributeEntry="${wireTransAttributes.bankCountryCode}" property="document.wireTransfer.bankCountryCode" readOnly="${!fullEntryMode&&!wireEntryMode&&!frnEntryMode}"/>
			</td>
		</tr>
		<tr>
			<th scope=row class="bord-l-b"><div align="right"><kul:htmlAttributeLabel attributeEntry="${wireTransAttributes.payeeAccountNumber}"/></div></th>
			<td class="datacell" colspan="3">
				<kul:htmlControlAttribute attributeEntry="${wireTransAttributes.payeeAccountNumber}" property="document.wireTransfer.payeeAccountNumber" readOnly="${!fullEntryMode&&!wireEntryMode&&!frnEntryMode}" />
			</td>
		</tr>
		<tr>
			<th scope=row class="bord-l-b"><div align="right"><kul:htmlAttributeLabel attributeEntry="${wireTransAttributes.payeeAccountName}"/></div></th>
			<td class="datacell">
				<kul:htmlControlAttribute attributeEntry="${wireTransAttributes.payeeAccountName}" property="document.wireTransfer.payeeAccountName" readOnly="${!fullEntryMode&&!wireEntryMode&&!frnEntryMode}"/>
			</td>
			<td class="datacell" colspan="2"><bean:message key="message.wiretransfer.fee"/></td>
		</tr>
		 <tr>
                <td colspan=4 valign=middle class="tab-subhead"><b><bean:message key="message.wiretransfer.foreign.wires.additional.data"/></b></td>
              </tr>
              
              <tr>
                <th scope=row class="bord-l-b" colspan="2"><div align="right">&nbsp;</div></th>
                <td class="datacell" colspan="2">
                  <b><bean:message key="message.wiretransfer.correspondent.bank"/></b>
                </td>
              </tr>
              
              <tr>
                <th scope=row class="bord-l-b"><div align="right"><kul:htmlAttributeLabel attributeEntry="${wireTransAttributes2.disbVchrBankIBAN}"/>
                </div></th>
                   <c:set var="bankIBANHint">
        			<bean:message key="label.wireTransfer.bankIBAN.hint"/>
                  </c:set>
                <td class="datacell">
                  <kul:htmlControlAttribute accessibilityHint="${bankIBANHint}" attributeEntry="${wireTransAttributes2.disbVchrBankIBAN}" property="document.wireTransfer.extension.disbVchrBankIBAN" readOnly="${!fullEntryMode&&!wireEntryMode&&!frnEntryMode}"/>
                </td>
                <th scope=row class="bord-l-b" ><div align="right"><kul:htmlAttributeLabel attributeEntry="${wireTransAttributes2.disbVchrCorrespondentBankName}"/></div></th>
                <td class="datacell">
                  <kul:htmlControlAttribute attributeEntry="${wireTransAttributes2.disbVchrCorrespondentBankName}" property="document.wireTransfer.extension.disbVchrCorrespondentBankName" readOnly="${!fullEntryMode&&!wireEntryMode&&!frnEntryMode}"/>
                </td>
              </tr>
              
              <tr>
                <th scope=row class="bord-l-b"><div align="right"><kul:htmlAttributeLabel attributeEntry="${wireTransAttributes2.disbVchrBankSWIFTCode}"/>
                </div></th>
                  <c:set var="bankSwiftCode">
        			<bean:message key="label.wireTransfer.bankSwiftCode.hint"/>
                  </c:set>
                <td class="datacell">
                  <kul:htmlControlAttribute accessibilityHint="${bankSwiftCode}" attributeEntry="${wireTransAttributes2.disbVchrBankSWIFTCode}" property="document.wireTransfer.extension.disbVchrBankSWIFTCode" readOnly="${!fullEntryMode&&!wireEntryMode&&!frnEntryMode}"/>
                </td>
                <th scope=row class="bord-l-b" ><div align="right"><kul:htmlAttributeLabel attributeEntry="${wireTransAttributes2.disbVchrCorrespondentBankAddress}"/></div></th>
                <td class="datacell">
                  <kul:htmlControlAttribute attributeEntry="${wireTransAttributes2.disbVchrCorrespondentBankAddress}" property="document.wireTransfer.extension.disbVchrCorrespondentBankAddress" readOnly="${!fullEntryMode&&!wireEntryMode&&!frnEntryMode}"/>
                </td>
              </tr>
              
              <tr>
                <th scope=row class="bord-l-b"><div align="right"><kul:htmlAttributeLabel attributeEntry="${wireTransAttributes2.disbVchrSortOrTransitCode}"/>
                </div></th>
                   <c:set var="sortOrTransitCodeHint">
        			<bean:message key="label.wireTransfer.sortOrTransitCode.hint"/>
                  </c:set>
                <td class="datacell">
                  <kul:htmlControlAttribute accessibilityHint="${sortOrTransitCodeHint}" attributeEntry="${wireTransAttributes2.disbVchrSortOrTransitCode}" property="document.wireTransfer.extension.disbVchrSortOrTransitCode" readOnly="${!fullEntryMode&&!wireEntryMode&&!frnEntryMode}"/>
                </td>


                <th scope=row class="bord-l-b" ><div align="right"><kul:htmlAttributeLabel attributeEntry="${wireTransAttributes2.disbVchrCorrespondentBankSwiftCode}"/></div></th>
    
                <td class="datacell">
                  <kul:htmlControlAttribute attributeEntry="${wireTransAttributes2.disbVchrCorrespondentBankSwiftCode}" property="document.wireTransfer.extension.disbVchrCorrespondentBankSwiftCode" readOnly="${!fullEntryMode&&!wireEntryMode&&!frnEntryMode}"/>
                </td>
              </tr>
              
              <tr>
                <th scope=row class="bord-l-b" colspan="2"><div align="right">&nbsp;
                </div></th>
                <th scope=row class="bord-l-b" ><div align="right"><kul:htmlAttributeLabel attributeEntry="${wireTransAttributes2.disbVchrCorrespondentBankRoutingNumber}"/></div></th>
                <td class="datacell">
                  <kul:htmlControlAttribute attributeEntry="${wireTransAttributes2.disbVchrCorrespondentBankRoutingNumber}" property="document.wireTransfer.extension.disbVchrCorrespondentBankRoutingNumber" readOnly="${!fullEntryMode&&!wireEntryMode&&!frnEntryMode}"/>
                </td>
              </tr>
              
              <tr>
                <td class="datacell" colspan="2">
                  <b><bean:message key="message.wiretransfer.use.IBAN"/></b>
                </td>
                <th scope=row class="bord-l-b" ><div align="right"><kul:htmlAttributeLabel attributeEntry="${wireTransAttributes2.disbVchrCorrespondentBankAccountNumber}"/></div></th>
                <td class="datacell">
                  <kul:htmlControlAttribute attributeEntry="${wireTransAttributes2.disbVchrCorrespondentBankAccountNumber}" property="document.wireTransfer.extension.disbVchrCorrespondentBankAccountNumber" readOnly="${!fullEntryMode&&!wireEntryMode&&!frnEntryMode}"/>
                </td>
              </tr>
	</tbody>
</table>