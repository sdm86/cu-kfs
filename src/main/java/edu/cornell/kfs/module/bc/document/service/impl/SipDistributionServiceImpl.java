package edu.cornell.kfs.module.bc.document.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kuali.kfs.integration.ld.LaborLedgerPositionObjectBenefit;
import org.kuali.kfs.module.bc.businessobject.BudgetConstructionMonthly;
import org.kuali.kfs.module.bc.businessobject.PendingBudgetConstructionAppointmentFunding;
import org.kuali.kfs.module.bc.businessobject.PendingBudgetConstructionGeneralLedger;
import org.kuali.kfs.module.bc.businessobject.RequestBenefits;
import org.kuali.kfs.module.bc.util.BudgetParameterFinder;
import org.kuali.kfs.module.ld.businessobject.BenefitsCalculation;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.kfs.sys.service.OptionsService;
import org.kuali.rice.core.api.util.type.KualiDecimal;
import org.kuali.rice.core.api.util.type.KualiInteger;
import org.kuali.rice.core.api.util.type.KualiPercent;
import org.kuali.rice.krad.service.BusinessObjectService;
import org.kuali.rice.krad.util.ObjectUtils;
import org.springframework.transaction.annotation.Transactional;

import edu.cornell.kfs.coa.businessobject.AccountExtendedAttribute;
import edu.cornell.kfs.module.bc.CUBCConstants;
import edu.cornell.kfs.module.bc.CUBCPropertyConstants;
import edu.cornell.kfs.module.bc.businessobject.SipImportData;
import edu.cornell.kfs.module.bc.document.dataaccess.SipDistributionDao;
import edu.cornell.kfs.module.bc.document.service.SipDistributionService;

public class SipDistributionServiceImpl implements SipDistributionService {

    private static final String SEPARATOR = "\t";

    protected BusinessObjectService businessObjectService;
    protected OptionsService optionsService;
    protected SipDistributionDao sipDistributionDao;

    /**
     * @see edu.cornell.kfs.module.bc.document.service.SipDistributionService#distributeSip(boolean,
     * java.util.List)
     */
    @Transactional
    public StringBuilder distributeSip(boolean updateMode, List<SipImportData> sipImportDataCollection) {

        StringBuilder reportEntries = new StringBuilder();

        Map<String, AffectedEdocInfo> affectedEdocsFor2PLGCleanup = new HashMap<String, AffectedEdocInfo>();

        //1. SIP totals

        // calculates totals

        calculateTotals(sipImportDataCollection);

        List<SipImportData> invalidEntries = new ArrayList<SipImportData>();
        List<SipImportData> sipToHrValidEntries = new ArrayList<SipImportData>();
        List<SipImportData> bcValidEntries = new ArrayList<SipImportData>();

        separateEntriesBasedOnValidationCode(sipImportDataCollection, invalidEntries, sipToHrValidEntries,
                bcValidEntries);

        // generate report for data with errors
        buildReportEntriesForInvalidEntries(reportEntries, invalidEntries);

        // generate report for SIP to HR entries
        buildSipTotalsForHRValidEntriesReportEntries(reportEntries, sipToHrValidEntries);

        // generate report entries for the SIP totals
        buildSipTotalsReportEntries(reportEntries, bcValidEntries);

        // persist the import data in the database no matter if running in report or update mode

        persistSipTotals(sipToHrValidEntries);

        //2. Sip distribution

        // distribute sip
        List<PendingBudgetConstructionAppointmentFunding> newDistributions = new ArrayList<PendingBudgetConstructionAppointmentFunding>();

        Map<String, KualiInteger> oldDistributionAmounts = new HashMap<String, KualiInteger>();
        Map<String, KualiInteger> oldLeaveDistributionAmounts = new HashMap<String, KualiInteger>();

        StringBuilder header = new StringBuilder();
        header.append("\nSip Distribution \n\n");
        reportEntries.append(header);
        reportEntries.append(buildReportTitlesForNewDistribution());

        for (SipImportData sipImportData : bcValidEntries) {
            if ("Y".equalsIgnoreCase(sipImportData.getPassedValidation())) {

                List<PendingBudgetConstructionAppointmentFunding> calculatedDistributions = calculateSipDistributions(
                        sipImportData, oldDistributionAmounts, oldLeaveDistributionAmounts);

                newDistributions.addAll(calculatedDistributions);

                // generate report entries for sip distribution

                buildSipDistributionsReportEntries(reportEntries, sipImportData, calculatedDistributions,
                        oldDistributionAmounts, oldLeaveDistributionAmounts);
            }
        }

        // generate report entries for sip distribution

        //        buildSipDistributionsReportEntries(reportEntries, newDistributions, oldDistributionAmounts);

        //3. update bcEdocs pending entries after the new distribution
        Map<String, KualiInteger> oldAmounts = new HashMap<String, KualiInteger>();

        // update BC pending entries
        Map<String, PendingBudgetConstructionGeneralLedger> newPBGLEntries = calculateNewPBGL(newDistributions,
                affectedEdocsFor2PLGCleanup, oldAmounts, oldDistributionAmounts, oldLeaveDistributionAmounts);

        // generate report entries for the updated BC pending entries

        buildSipNewPBGLReportEntries(reportEntries, newPBGLEntries, oldAmounts);

        //4. Calculate Benefits

        // calculate annual benefits
        // calculate  monthly benefits

        Map<String, PendingBudgetConstructionGeneralLedger> benefitsMap = new HashMap<String, PendingBudgetConstructionGeneralLedger>();
        Map<String, BudgetConstructionMonthly> monthlyBenefitsMap = new HashMap<String, BudgetConstructionMonthly>();
        Map<String, KualiInteger> oldMonthlyBenefitsAmount = new HashMap<String, KualiInteger>();
        Map<String, KualiInteger> oldAnnualBenefitsAmount = new HashMap<String, KualiInteger>();
        Map<String, List<RequestBenefits>> requestBenefitsMap = new HashMap<String, List<RequestBenefits>>();

        calculatBenefits(newPBGLEntries, oldAmounts, benefitsMap, monthlyBenefitsMap, requestBenefitsMap,
                oldAnnualBenefitsAmount,
                oldMonthlyBenefitsAmount,
                affectedEdocsFor2PLGCleanup);

        // Get SIP pools to delete

        List<PendingBudgetConstructionGeneralLedger> sipPoolEntriesToDelete = getSIPPoolEntriesToDelete(affectedEdocsFor2PLGCleanup);

        //update benefits by removing the SIP pools part
        updateFringeBenefitsByRemovingSipPoolBenefits(sipPoolEntriesToDelete, benefitsMap);

        // generate benefits details entries
        buildSipNewPBGLReportEntriesWithBenefitsDetails(reportEntries, newPBGLEntries, oldAmounts, requestBenefitsMap);

        // generate report entries for the calculated benefits
        buildSipNewAnnualBenefitsReportEntries(reportEntries, benefitsMap, oldAnnualBenefitsAmount);
        buildSipNewMonthlyBenefitsReportEntries(reportEntries, monthlyBenefitsMap, oldMonthlyBenefitsAmount);

        //5. Calculate 2PLG entries

        // calculate the 2PLG entries
        List<PendingBudgetConstructionGeneralLedger> newPlugEntries = calculateNewPlugEntries(affectedEdocsFor2PLGCleanup);

        // build new plug entries report
        buildSipNewPlugReportEntries(reportEntries, newPlugEntries);

        //remove 2PLG entries

        List<PendingBudgetConstructionGeneralLedger> _2PlugEntriesToDelete = get2PLGEntriesToDelete(affectedEdocsFor2PLGCleanup);

        // generate report entries for the calculated 2PLG entries

        buildSip2PlugReportEntries(reportEntries, _2PlugEntriesToDelete);

        // build report for SIP pool entries
        buildSipPoolReportEntries(reportEntries, sipPoolEntriesToDelete);

        /////----persistence step----/////

        if (updateMode) {

            // persist sip distribution
            if (ObjectUtils.isNotNull(newDistributions)) {
                persistNewDistributions(newDistributions);
            }

            // persist updated BC pending entries
            if (ObjectUtils.isNotNull(newPBGLEntries)) {
                persistNewPBGL(newPBGLEntries);
            }

            // persist the calculated benefits

            // persist new annual benefits
            if (ObjectUtils.isNotNull(benefitsMap)) {
                persistNewAnnualBenefits(benefitsMap);
            }

            // persist new monthly benefits
            if (ObjectUtils.isNotNull(monthlyBenefitsMap)) {
                persistNewMonthlyBenefits(monthlyBenefitsMap);
            }

            //persist new plug entries
            if (ObjectUtils.isNotNull(newPlugEntries)) {
                persistEntriesToPBGL(newPlugEntries);
            }

            // delete 2PLG entries
            if (ObjectUtils.isNotNull(_2PlugEntriesToDelete)) {
                deletePBGLEntries(_2PlugEntriesToDelete);
            }

            //delete SIP pool entries
            if (ObjectUtils.isNotNull(sipPoolEntriesToDelete)) {
                deletePBGLEntries(sipPoolEntriesToDelete);
            }
        }

        return reportEntries;

    }

    /**
     * Updates the fringe benefits amounts by subtracting the amount related to the sip
     * pool entry used to build the total fringe amount
     * 
     * @param sipPoolEntriesToDelete
     * @param benefitsMap
     */
    private void updateFringeBenefitsByRemovingSipPoolBenefits(
            List<PendingBudgetConstructionGeneralLedger> sipPoolEntriesToDelete,
            Map<String, PendingBudgetConstructionGeneralLedger> benefitsMap) {
        if (ObjectUtils.isNotNull(sipPoolEntriesToDelete)) {

            for (PendingBudgetConstructionGeneralLedger sipPoolEntry : sipPoolEntriesToDelete) {

                Integer fiscalYear = sipPoolEntry.getUniversityFiscalYear();
                String finObjTypeExpenditureexpCd = optionsService.getOptions(fiscalYear)
                        .getFinObjTypeExpenditureexpCd();

                String laborBenefitsRateCategoryCode = sipPoolEntry.getAccount()
                        .getLaborBenefitRateCategoryCode();
                List<LaborLedgerPositionObjectBenefit> positionObjectBenefits = sipPoolEntry.getPositionObjectBenefit();

                for (LaborLedgerPositionObjectBenefit benefit : positionObjectBenefits) {

                    Map<String, String> keyFields = new HashMap<String, String>();

                    keyFields.put("universityFiscalYear", String.valueOf(fiscalYear));
                    keyFields.put("chartOfAccountsCode", sipPoolEntry.getChartOfAccountsCode());
                    keyFields.put("positionBenefitTypeCode", benefit.getFinancialObjectBenefitsTypeCode());
                    keyFields.put("laborBenefitRateCategoryCode", laborBenefitsRateCategoryCode);

                    BenefitsCalculation benefitsCalculation = (BenefitsCalculation) businessObjectService
                            .findByPrimaryKey(BenefitsCalculation.class, keyFields);

                    if (ObjectUtils.isNotNull(benefitsCalculation)
                            && laborBenefitsRateCategoryCode.equalsIgnoreCase(benefitsCalculation
                                    .getLaborBenefitRateCategoryCode())) {

                        String benefitsObjectCode = benefitsCalculation.getPositionFringeBenefitObjectCode();
                        KualiPercent benefitsPercent = benefitsCalculation.getPositionFringeBenefitPercent();

                        if (benefitsPercent.isNonZero()) {
                            // compute benefits amount
                            RequestBenefits requestBenefit = createRequestBenefit(sipPoolEntry, benefit,
                                    benefitsCalculation, benefitsPercent);

                            //update the corresponding benefits entry by subtracting the sipPoolBenefitAmount
                            String fringeBenefitKey = buildBenefitKey(sipPoolEntry, benefitsObjectCode,
                                    finObjTypeExpenditureexpCd);
                            PendingBudgetConstructionGeneralLedger fringeBenefit = benefitsMap.get(fringeBenefitKey);
                            
                            if(ObjectUtils.isNotNull(fringeBenefit)){
                            	
                            	fringeBenefit.setAccountLineAnnualBalanceAmount(new KualiInteger((fringeBenefit
                                    .getAccountLineAnnualBalanceAmount().subtract(
                                            requestBenefit.getFringeDetailAmount())).bigIntegerValue()));
                            }

                        }
                    }
                }

            }
        }
    }

    /**
     * Totals SIP
     * 
     * 1. New Comp Rate 1. If the Comp_Rate is an hourly amount (Comp_Frequency = H) then
     * the new Comp Rate is stored as the hourly rate 1. Comp_Rate + Increase_to_Minimum +
     * Equity + Merit 2. If the Comp_Rate is an annual amount (Comp_Frequency = A) then
     * the new Comp Rate is stored as an annual amount (and is the same as New Annual
     * Rate) 1. Annual_Rate + Increase_to_Minimum + Equity + Merit 2. New Annual Rate 1.
     * For all records, the New Annual Rate is the annual amount 1. Annual_Rate +
     * Increase_to_Minimum + Equity + Merit 2. Note that in the SIP Export file the
     * Comp_Rate Job_Standard_Hours 52 = Annual_Rate 3. Look out for weird data, like
     * positions 139348 and 147052 (Comp_Frequency = H and Annual_Rate = Comp_Rate) 4. If
     * the Annual_Rate has cents then the new, post-SIP annual rate probably does too
     */
    public void calculateTotals(List<SipImportData> sipImportDataCollection) {

        for (SipImportData sipImportData : sipImportDataCollection) {

            if ("Y".equalsIgnoreCase(sipImportData.getPassedValidation())
                    || "S".equalsIgnoreCase(sipImportData.getPassedValidation())) {

                // calculate totals
                /*
                 * Totals SIP

                1. New Comp Rate
                 1. If the Comp_Rate is an hourly amount (Comp_Frequency = H) then the new Comp Rate is stored as the hourly rate (This is for the file we'll give to Pam to load into HR. Dennis Werner is working on the HR interface. See KITI-3000)
                       1. Comp_Rate + Increase_to_Minimum + Equity + Merit
                 2. If the Comp_Rate is an annual amount (Comp_Frequency = A) then the new Comp Rate is stored as an annual amount (and is the same as New Annual Rate)
                       1. Annual_Rate + Increase_to_Minimum + Equity + Merit
                       */
                KualiDecimal newCompRate = KualiDecimal.ZERO;
                KualiDecimal newAnnualRate = KualiDecimal.ZERO;
                KualiDecimal oldCompRate = sipImportData.getCompRt() == null ? KualiDecimal.ZERO : sipImportData
                        .getCompRt();
                KualiDecimal increaseToMin = sipImportData.getIncToMin() == null ? KualiDecimal.ZERO : sipImportData
                        .getIncToMin();
                KualiDecimal equity = sipImportData.getEquity() == null ? KualiDecimal.ZERO : sipImportData.getEquity();
                KualiDecimal merit = sipImportData.getMerit() == null ? KualiDecimal.ZERO : sipImportData.getMerit();
                KualiDecimal annualRate = sipImportData.getAnnlRt() == null ? KualiDecimal.ZERO : sipImportData
                        .getAnnlRt();

                // compute new comp rate, it is the same for both annual and hourly employees
                newCompRate = newCompRate.add(oldCompRate);
                newCompRate = newCompRate.add(increaseToMin);
                newCompRate = newCompRate.add(equity);
                newCompRate = newCompRate.add(merit);

                /*
                2. New Annual Rate
                1. For all records, the New Annual Rate is the annual amount
                1. Annual_Rate + Increase_to_Minimum + Equity + Merit
                2. Note that in the SIP Export file the Comp_Rate Job_Standard_Hours 52 = Annual_Rate
                3. Look out for weird data, like positions 139348 and 147052 (Comp_Frequency = H and Annual_Rate = Comp_Rate)
                4. If the Annual_Rate has cents then the new, post-SIP annual rate probably does too

                */
                if (CUBCConstants.CompFreq.HOURLY.equalsIgnoreCase(sipImportData.getCompFreq())) {

                    // compute new annual rate

                    KualiDecimal sipAmount = KualiDecimal.ZERO;
                    sipAmount = sipAmount.add(increaseToMin);
                    sipAmount = sipAmount.add(equity);
                    sipAmount = sipAmount.add(merit);
                    sipAmount = sipAmount.multiply(sipImportData.getJobStdHrs());
                    sipAmount = sipAmount.multiply(new KualiDecimal(52));

                    newAnnualRate = annualRate.add(sipAmount);

                } else if (CUBCConstants.CompFreq.ANNUALLY.equalsIgnoreCase(sipImportData.getCompFreq())) {

                    // compute new annual rate

                    newAnnualRate = newAnnualRate.add(oldCompRate);
                    newAnnualRate = newAnnualRate.add(increaseToMin);
                    newAnnualRate = newAnnualRate.add(equity);
                    newAnnualRate = newAnnualRate.add(merit);
                }

                //set the annual and comp rate on the sip totals
                sipImportData.setNewAnnualRate(newAnnualRate);
                sipImportData.setNewCompRate(newCompRate);
            }
        }

    }

    /**
     * Separates the Sip import data based on the validation code: entries that are
     * invalid and are not saved in SIP table and not processed for BC, entries that have
     * errors and are not valid for BC but are saved in SIP table and entries that are
     * only valid for BC
     * 
     * @param sipImportDataCollection
     * @param invalidEntries
     * @param sipToHrValidEntries
     * @param bcValidEntries
     */
    private void separateEntriesBasedOnValidationCode(List<SipImportData> sipImportDataCollection,
            List<SipImportData> invalidEntries, List<SipImportData> sipToHrValidEntries,
            List<SipImportData> bcValidEntries) {

        for (SipImportData sipImportData : sipImportDataCollection) {
            if ("E".equalsIgnoreCase(sipImportData.getPassedValidation())) {
                invalidEntries.add(sipImportData);
            } else if ("Y".equalsIgnoreCase(sipImportData.getPassedValidation())) {
                sipToHrValidEntries.add(sipImportData);
                bcValidEntries.add(sipImportData);
            } else if ("S".equalsIgnoreCase(sipImportData.getPassedValidation())) {
                sipToHrValidEntries.add(sipImportData);
            }
        }
    }

    /**
     * Distributes amount
     * 
     * 1. The New Annual Amount is distributed according to the “% Distribution” for the
     * incumbents’ job(s) and funding 1. chart > account > sub-account > object >
     * sub-object > emplid > position > requested % distribution
     * 
     * @param sipImportData
     * @return a map holding the new distributions per position and emplid
     */
    public List<PendingBudgetConstructionAppointmentFunding> calculateSipDistributions(
            SipImportData sipImportData, Map<String, KualiInteger> oldDistributionAmounts,
            Map<String, KualiInteger> oldLeaveDistributionAmounts) {

        List<PendingBudgetConstructionAppointmentFunding> newDistributions = new ArrayList<PendingBudgetConstructionAppointmentFunding>();

        // check if there was SIP
        if (sipImportData.getAnnlRt().compareTo(sipImportData.getNewAnnualRate()) != 0
                && (sipImportData.getIncToMin().isNonZero() || sipImportData.getMerit().isNonZero() || sipImportData
                        .getEquity().isNonZero())) {

            Map<String, String> keyValues = new HashMap<String, String>();
            keyValues.put(CUBCPropertyConstants.PendingBudgetConstructionAppointmentFundingProperties.POSITION_NBR,
                    "00" + sipImportData.getPositionNbr());
            keyValues.put(CUBCPropertyConstants.PendingBudgetConstructionAppointmentFundingProperties.EMPLID,
                    sipImportData.getEmplId());

            List<PendingBudgetConstructionAppointmentFunding> appointmentFundings = (List<PendingBudgetConstructionAppointmentFunding>) businessObjectService
                    .findMatching(PendingBudgetConstructionAppointmentFunding.class, keyValues);

            for (PendingBudgetConstructionAppointmentFunding appointmentFunding : appointmentFundings) {

                PendingBudgetConstructionAppointmentFunding newDistribution = new PendingBudgetConstructionAppointmentFunding();
                newDistribution = appointmentFunding;
                boolean isChanged = false;

                if (!appointmentFunding.isAppointmentFundingDeleteIndicator()
                        && appointmentFunding.getAppointmentRequestedAmount() != null
                        && appointmentFunding.getAppointmentRequestedAmount().isNonZero()
                        && appointmentFunding.getAppointmentRequestedTimePercent() != null
                        && (appointmentFunding.getAppointmentRequestedTimePercent().compareTo(BigDecimal.ZERO) != 0)
                        && appointmentFunding.getAppointmentRequestedFteQuantity() != null
                        && (appointmentFunding.getAppointmentRequestedFteQuantity().compareTo(BigDecimal.ZERO) != 0)) {

                    oldDistributionAmounts.put(buildAppointmentFundingKey(appointmentFunding), new KualiInteger(
                            appointmentFunding.getAppointmentRequestedAmount().bigIntegerValue()));

                    BigDecimal newDistributionAmount = BigDecimal.ZERO;
                    newDistributionAmount = sipImportData.getNewAnnualRate().bigDecimalValue()
                            .multiply(newDistribution.getAppointmentRequestedFteQuantity());
                    newDistribution.setAppointmentRequestedAmount(new KualiInteger(newDistributionAmount));

                    isChanged = true;

                }

                if (!appointmentFunding.isAppointmentFundingDeleteIndicator()
                        && appointmentFunding.getAppointmentRequestedCsfAmount() != null
                        && appointmentFunding.getAppointmentRequestedCsfAmount().isNonZero()
                        && appointmentFunding.getAppointmentRequestedCsfTimePercent() != null
                        && (appointmentFunding.getAppointmentRequestedCsfTimePercent().compareTo(BigDecimal.ZERO) != 0)
                        && appointmentFunding.getAppointmentRequestedCsfFteQuantity() != null
                        && (appointmentFunding.getAppointmentRequestedCsfFteQuantity().compareTo(BigDecimal.ZERO) != 0)) {

                    oldLeaveDistributionAmounts.put(buildAppointmentFundingKey(appointmentFunding), new KualiInteger(
                            appointmentFunding.getAppointmentRequestedCsfAmount().bigIntegerValue()));

                    KualiDecimal newLeaveDistributionAmount = KualiDecimal.ZERO;
                    newLeaveDistributionAmount = new KualiDecimal(sipImportData.getNewAnnualRate().bigDecimalValue()
                            .multiply(newDistribution.getAppointmentRequestedCsfFteQuantity()));

                    newDistribution.setAppointmentRequestedCsfAmount(new KualiInteger(newLeaveDistributionAmount
                            .bigDecimalValue()));

                    isChanged = true;

                }

                if (isChanged) {
                    newDistributions.add(newDistribution);
                }

            }
        }

        return newDistributions;
    }

    /**
     * @see edu.cornell.kfs.module.bc.document.service.SipDistributionService#persistSipTotals(java.util.List)
     */
    public void persistSipTotals(List<SipImportData> sipImportDataCollection) {
        // wipe out everything first
        businessObjectService.deleteMatching(
                SipImportData.class,
                new HashMap<String, String>());
        businessObjectService.save(sipImportDataCollection);

    }

    /**
     * @see edu.cornell.kfs.module.bc.document.service.SipDistributionService#persistNewDistributions(java.util.List)
     */
    public void persistNewDistributions(List<PendingBudgetConstructionAppointmentFunding> newDistributions) {

        for (PendingBudgetConstructionAppointmentFunding newDistribution : newDistributions) {

            // newDistribution.setVersionNumber(newDistribution.getVersionNumber() + 1);
            businessObjectService.save(newDistribution);
        }

    }

    public void persistNewLeaveDistributions(List<PendingBudgetConstructionAppointmentFunding> newDistributions) {

        for (PendingBudgetConstructionAppointmentFunding newDistribution : newDistributions) {

            //            newDistribution.setVersionNumber(newDistribution.getVersionNumber() + 2);
            businessObjectService.save(newDistribution);
        }

    }

    /**
     * Calculates the new amounts for the PBGL entries affected by SIP: retrieves all PBGL
     * entries for the given appointment funding lines and adds the difference between the
     * old appointment funding amount and the new appointment funding amount after SIP has
     * been added.
     * 
     * @param newDistributions
     * @param affectedEdocs
     * @param oldAmounts
     * @param oldDistributionAmounts
     * @param oldLeaveDistributionAmounts
     * @return a map with the updated PBGL lines
     */
    public Map<String, PendingBudgetConstructionGeneralLedger> calculateNewPBGL(
            List<PendingBudgetConstructionAppointmentFunding> newDistributions,
            Map<String, AffectedEdocInfo> affectedEdocs,
            Map<String, KualiInteger> oldAmounts, Map<String, KualiInteger> oldDistributionAmounts,
            Map<String, KualiInteger> oldLeaveDistributionAmounts) {

        Map<String, PendingBudgetConstructionGeneralLedger> newPBGLLinesMap = new HashMap<String, PendingBudgetConstructionGeneralLedger>();

        for (PendingBudgetConstructionAppointmentFunding newDistribution : newDistributions) {

            // for each appointment funding calculate the change in the appointment funding amount in order to add that to the corresponding PBGL line
            KualiInteger amountChange = KualiInteger.ZERO;

            // if the appointment funding line is not deleted, it has a requested amount that is not empty or zero and it has a requested time percent that is not empty and is not zero the calculate the change in the appointment funding amount
            if (!newDistribution.isAppointmentFundingDeleteIndicator()
                    && newDistribution.getAppointmentRequestedAmount() != null
                    && newDistribution.getAppointmentRequestedAmount().isNonZero()
                    && newDistribution.getAppointmentRequestedTimePercent() != null
                    && (newDistribution.getAppointmentRequestedTimePercent().compareTo(BigDecimal.ZERO) != 0)) {

                KualiInteger oldRequestedAmount = oldDistributionAmounts
                        .get(buildAppointmentFundingKey(newDistribution));

                amountChange = newDistribution.getAppointmentRequestedAmount().subtract(
                        oldRequestedAmount);
            }

            // check for leave: if the appointment funding is not deleted and the requested leave amount is not empty and not zero and the leave time percent is not empty and the time percent is not zero then add the leave difference (new leave amount - old leave amount) to the amount change
            if (!newDistribution.isAppointmentFundingDeleteIndicator()
                    && newDistribution.getAppointmentRequestedCsfAmount() != null
                    && newDistribution.getAppointmentRequestedCsfAmount().isNonZero()
                    && newDistribution.getAppointmentRequestedCsfTimePercent() != null
                    && (newDistribution.getAppointmentRequestedCsfTimePercent().compareTo(BigDecimal.ZERO) != 0)) {

                KualiInteger oldLeaveRequestedAmount = oldLeaveDistributionAmounts
                        .get(buildAppointmentFundingKey(newDistribution));

                KualiInteger leaveAmountChange = newDistribution.getAppointmentRequestedCsfAmount().subtract(
                        oldLeaveRequestedAmount);
                amountChange = amountChange.add(leaveAmountChange);
            }

            // if there was a change in the requested amount then process
            if (amountChange != null && amountChange.isNonZero()) {
                // check if PBGL line already in the map
                String key = buildPBGLKeyFromAppointmentFunding(newDistribution);
                PendingBudgetConstructionGeneralLedger pendingRecord = null;
                // add
                if (newPBGLLinesMap.containsKey(key)) {
                    pendingRecord = newPBGLLinesMap.get(key);
                } else {

                    pendingRecord = getPBGLEntry(newDistribution);

                    if (ObjectUtils.isNotNull(pendingRecord)) {
                        oldAmounts.put(buildPendingBudgetConstructionGeneralLedgerKey(pendingRecord), new KualiInteger(
                                pendingRecord.getAccountLineAnnualBalanceAmount().bigIntegerValue()));
                    }
                }

                if (ObjectUtils.isNotNull(pendingRecord)) {
                    KualiInteger newAmount = pendingRecord.getAccountLineAnnualBalanceAmount().add(
                            amountChange);
                    pendingRecord.setAccountLineAnnualBalanceAmount(newAmount);
                    newPBGLLinesMap.put(key, pendingRecord);

                    String docNumber = pendingRecord.getDocumentNumber();
                    if (!affectedEdocs.containsKey(docNumber)) {
                        AffectedEdocInfo affectedEdocInfo = new AffectedEdocInfo(docNumber,
                                pendingRecord.getUniversityFiscalYear(), pendingRecord.getChartOfAccountsCode(),
                                pendingRecord.getAccountNumber(), pendingRecord.getSubAccountNumber(), amountChange);
                        affectedEdocs.put(docNumber, affectedEdocInfo);
                    } else {
                        AffectedEdocInfo currentInfo = affectedEdocs.get(docNumber);
                        KualiInteger currentAmount = currentInfo.changeAmount;
                        currentInfo.changeAmount = currentAmount.add(amountChange);
                        affectedEdocs.put(docNumber, currentInfo);
                    }
                }
            }
        }

        return newPBGLLinesMap;
    }

    /**
     * Gets the corresponding PBGL entry for a given
     * PendingBudgetConstructionAppointmentFunding entry.
     * 
     * @param appointmentFunding the appointment funding for which we want to retrieve the
     * PBGL line
     * @return the corresponding PBGL entry for a given
     * PendingBudgetConstructionAppointmentFunding entry
     */
    private PendingBudgetConstructionGeneralLedger getPBGLEntry(
            PendingBudgetConstructionAppointmentFunding appointmentFunding) {
        PendingBudgetConstructionGeneralLedger pbglEntry = null;
        Map<String, String> keyValues = new HashMap<String, String>();
        keyValues.put(CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.UNIVERSITY_FISCAL_YEAR,
                appointmentFunding.getUniversityFiscalYear().toString());
        keyValues.put(CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.CHART_CD,
                appointmentFunding.getChartOfAccountsCode());
        keyValues.put(CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.ACCOUNT_NBR,
                appointmentFunding.getAccountNumber());
        keyValues.put(CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.SUB_ACCOUNT_NBR,
                appointmentFunding.getSubAccountNumber());
        keyValues.put(CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.FINANCIAL_OBJECT_CODE,
                appointmentFunding.getFinancialObjectCode());
        keyValues.put(CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.FINANCIAL_SUB_OBJECT_CODE,
                appointmentFunding.getFinancialSubObjectCode());

        Collection<PendingBudgetConstructionGeneralLedger> pbGLEntries = businessObjectService.findMatching(
                PendingBudgetConstructionGeneralLedger.class, keyValues);

        if (pbGLEntries != null && pbGLEntries.size() > 0) {
            if (pbGLEntries.iterator().hasNext()) {
                pbglEntry = pbGLEntries.iterator().next();
            }
        }

        return pbglEntry;
    }

    /**
     * Builds a key for the given PendingBudgetConstructionAppointmentFunding entry.
     * 
     * @param appointmentFunding
     * @return a key for the given PendingBudgetConstructionAppointmentFunding entry
     */
    private String buildAppointmentFundingKey(PendingBudgetConstructionAppointmentFunding appointmentFunding) {
        String key = appointmentFunding.getUniversityFiscalYear() + appointmentFunding.getEmplid()
                + appointmentFunding.getPositionNumber() + appointmentFunding.getChartOfAccountsCode()
                + appointmentFunding.getAccountNumber() + appointmentFunding.getSubAccountNumber()
                + appointmentFunding.getFinancialObjectCode() + appointmentFunding.getFinancialSubObjectCode();
        return key;
    }

    private String buildPBGLKeyFromAppointmentFunding(PendingBudgetConstructionAppointmentFunding appointmentFunding) {
        String key = appointmentFunding.getUniversityFiscalYear() + appointmentFunding.getChartOfAccountsCode()
                + appointmentFunding.getAccountNumber() + appointmentFunding.getSubAccountNumber()
                + appointmentFunding.getFinancialObjectCode() + appointmentFunding.getFinancialSubObjectCode();
        return key;
    }

    /**
     * 
     * @see edu.cornell.kfs.module.bc.document.service.SipDistributionService#persistNewPBGL(java.util.Map)
     */
    public void persistNewPBGL(Map<String, PendingBudgetConstructionGeneralLedger> newPBGLList) {

        persistEntriesToPBGL(newPBGLList.values());

    }

    /**
     * Persists the updated annual benefits
     * 
     * @param newAnnualBenefitsList
     */
    public void persistNewAnnualBenefits(Map<String, PendingBudgetConstructionGeneralLedger> newAnnualBenefitsList) {

        persistEntriesToPBGL(newAnnualBenefitsList.values());

    }

    /**
     * Persists updated entries to PBGL.
     * 
     * @param pbglEntries
     */
    private void persistEntriesToPBGL(Collection<PendingBudgetConstructionGeneralLedger> pbglEntries) {
        for (PendingBudgetConstructionGeneralLedger pbglEntry : pbglEntries) {

            // pbglEntry.setVersionNumber(pbglEntry.getVersionNumber() + 1);
            businessObjectService.save(pbglEntry);
        }
    }

    /**
     * Delete the 2PLG entries.
     * 
     * @param pbglEntries
     */
    private void deletePBGLEntries(List<PendingBudgetConstructionGeneralLedger> pbglEntries) {
        businessObjectService.delete(pbglEntries);

    }

    /**
     * Persist the new monthly benefits.
     * 
     * @param newMonthlyBenefitsList
     */
    public void persistNewMonthlyBenefits(Map<String, BudgetConstructionMonthly> newMonthlyBenefitsList) {

        for (BudgetConstructionMonthly benefit : newMonthlyBenefitsList.values()) {

            // benefit.setVersionNumber(benefit.getVersionNumber() + 1);
            businessObjectService.save(benefit);
        }

    }

    /**
     * Calculates updated annual benefits for the given PBGL entries as well as the
     * monthly benefits where needed.
     * 
     * @param newPBGLEntries
     * @param oldPBGLAmounts
     * @param benefitsMap
     * @param monthlyBenefitsMap
     * @param requestBenefitsMap
     * @param oldAnnualBenefitsAmount
     * @param oldMonthlyBenefitsAmount
     * @param affectedEdocs
     */
    public void calculatBenefits(
            Map<String, PendingBudgetConstructionGeneralLedger> newPBGLEntries,
            Map<String, KualiInteger> oldPBGLAmounts,
            Map<String, PendingBudgetConstructionGeneralLedger> benefitsMap,
            Map<String, BudgetConstructionMonthly> monthlyBenefitsMap,
            Map<String, List<RequestBenefits>> requestBenefitsMap,
            Map<String, KualiInteger> oldAnnualBenefitsAmount,
            Map<String, KualiInteger> oldMonthlyBenefitsAmount,
            Map<String, AffectedEdocInfo> affectedEdocs) {

        if (newPBGLEntries != null && newPBGLEntries.size() > 0) {

            Integer fiscalYear = null;

            String finObjTypeExpenditureexpCd = null;

            for (PendingBudgetConstructionGeneralLedger newPBGLEntry : newPBGLEntries.values()) {

                if (fiscalYear == null) {
                    fiscalYear = newPBGLEntry.getUniversityFiscalYear();
                    finObjTypeExpenditureexpCd = optionsService.getOptions(fiscalYear).getFinObjTypeExpenditureexpCd();
                }

                String laborBenefitsRateCategoryCode = newPBGLEntry.getAccount()
                        .getLaborBenefitRateCategoryCode();
                List<LaborLedgerPositionObjectBenefit> positionObjectBenefits = newPBGLEntry.getPositionObjectBenefit();
                // create the request benefits list
                List<RequestBenefits> requestBenefits = new ArrayList<RequestBenefits>();

                for (LaborLedgerPositionObjectBenefit benefit : positionObjectBenefits) {

                    Map<String, String> keyFields = new HashMap<String, String>();

                    keyFields.put("universityFiscalYear", String.valueOf(fiscalYear));
                    keyFields.put("chartOfAccountsCode", newPBGLEntry.getChartOfAccountsCode());
                    keyFields.put("positionBenefitTypeCode", benefit.getFinancialObjectBenefitsTypeCode());
                    keyFields.put("laborBenefitRateCategoryCode", laborBenefitsRateCategoryCode);

                    BenefitsCalculation benefitsCalculation = (BenefitsCalculation) businessObjectService
                            .findByPrimaryKey(BenefitsCalculation.class, keyFields);

                    if (ObjectUtils.isNotNull(benefitsCalculation)
                            && laborBenefitsRateCategoryCode.equalsIgnoreCase(benefitsCalculation
                                    .getLaborBenefitRateCategoryCode())) {

                        String benefitsObjectCode = benefitsCalculation.getPositionFringeBenefitObjectCode();
                        KualiPercent benefitsPercent = benefitsCalculation.getPositionFringeBenefitPercent();

                        if (benefitsPercent.isNonZero()) {

                            KualiInteger oldPBGLAmount = oldPBGLAmounts
                                    .get(buildPendingBudgetConstructionGeneralLedgerKey(newPBGLEntry));

                            // create and add new request Benefit
                            requestBenefits.add(createRequestBenefit(newPBGLEntry, benefit, benefitsCalculation,
                                    benefitsPercent));

                            // get the benefit line if exists, create a new one otherwise
                            createOrUpdateBenefitsLine(benefitsMap, monthlyBenefitsMap, newPBGLEntry, oldPBGLAmount,
                                    benefitsObjectCode, finObjTypeExpenditureexpCd, benefitsPercent,
                                    oldAnnualBenefitsAmount,
                                    oldMonthlyBenefitsAmount, affectedEdocs);

                            requestBenefitsMap.put(buildPendingBudgetConstructionGeneralLedgerKey(newPBGLEntry),
                                    requestBenefits);
                        }

                    }

                }

            }
        }

    }

    /**
     * Creates a request benefit object for reporting purposes from the given benefit
     * entry.
     * 
     * @param newPBGLEntry
     * @param benefit
     * @param benefitsCalculation
     * @param benefitsPercent
     * @return returns a RequestBenefits object
     */
    private RequestBenefits createRequestBenefit(PendingBudgetConstructionGeneralLedger newPBGLEntry,
            LaborLedgerPositionObjectBenefit benefit, BenefitsCalculation benefitsCalculation,
            KualiPercent benefitsPercent) {
        // create the request benefit
        RequestBenefits requestBenefit = new RequestBenefits();
        requestBenefit.setChartOfAccountsCode(newPBGLEntry.getChartOfAccountsCode());
        requestBenefit.setAccountNumber(newPBGLEntry.getAccountNumber());
        requestBenefit.setFinancialObjectBenefitsTypeCode(benefit.getFinancialObjectBenefitsTypeCode());
        requestBenefit.setFinancialObjectBenefitsTypeDescription(benefitsCalculation
                .getLaborLedgerBenefitsType().getPositionBenefitTypeDescription());
        requestBenefit.setPositionFringeBenefitObjectCode(benefitsCalculation.getPositionFringeBenefitObjectCode());
        requestBenefit.setPositionFringeBenefitObjectCodeName(benefitsCalculation
                .getPositionFringeBenefitObject().getFinancialObjectCodeName());

        // set the benefit percent to be display in the results
        requestBenefit.setPositionFringeBenefitPercent(benefitsPercent);

        requestBenefit.setLaborBenefitRateCategoryCode(benefitsCalculation.getLaborBenefitRateCategoryCode());
        requestBenefit.setLaborBenefitRateCategoryCodeDesc(benefitsCalculation.getLaborBenefitRateCategory()
                .getCodeDesc());

        KualiInteger fringeDetailAmount = (newPBGLEntry.getAccountLineAnnualBalanceAmount().multiply(
                benefitsPercent.bigDecimalValue().divide(new BigDecimal(100))));
        requestBenefit.setFringeDetailAmount(fringeDetailAmount);

        return requestBenefit;
    }

    /**
     * Gets a benefit line with a benefit amount for the given PBGL entry. If a benefits
     * line already existed it will update the benefits amount.
     * 
     * @param glEntry
     * @param benefitsObjectCode
     * @param finObjTypeExpenditureexpCd
     * @param benefitPercent
     */
    private void createOrUpdateBenefitsLine(
            Map<String, PendingBudgetConstructionGeneralLedger> benefitsMap,
            Map<String, BudgetConstructionMonthly> monthlyBenefitsMap,
            PendingBudgetConstructionGeneralLedger glEntry,
            KualiInteger oldPBGLAmount,
            String benefitsObjectCode, String finObjTypeExpenditureexpCd, KualiPercent benefitPercent,
            Map<String, KualiInteger> oldAnnualBenefitsAmount,
            Map<String, KualiInteger> oldMonthlyBenefitsAmount,
            Map<String, AffectedEdocInfo> affectedEdocs) {

        PendingBudgetConstructionGeneralLedger benefitsPbglEntry = null;

        //first check if the benefits entry has already been retrieved or created
        String key = buildBenefitKey(glEntry, benefitsObjectCode, finObjTypeExpenditureexpCd);

        if (benefitsMap.containsKey(key)) {
            benefitsPbglEntry = benefitsMap.get(key);

        } else {

            // try to retrieve benefits entry if exists
            Map<String, String> keyValues = new HashMap<String, String>();
            keyValues.put(CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.DOC_NBR,
                    glEntry.getDocumentNumber());
            keyValues.put(
                    CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.UNIVERSITY_FISCAL_YEAR,
                    glEntry.getUniversityFiscalYear().toString());
            keyValues.put(CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.CHART_CD,
                    glEntry.getChartOfAccountsCode());
            keyValues.put(CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.ACCOUNT_NBR,
                    glEntry.getAccountNumber());
            keyValues.put(CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.SUB_ACCOUNT_NBR,
                    glEntry.getSubAccountNumber());
            keyValues.put(CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.FINANCIAL_OBJECT_CODE,
                    benefitsObjectCode);
            keyValues.put(
                    CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.FINANCIAL_SUB_OBJECT_CODE,
                    KFSConstants.getDashFinancialSubObjectCode());
            keyValues.put(
                    CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.FINANCIAL_BALANCE_TYP_CD,
                    KFSConstants.BALANCE_TYPE_BASE_BUDGET);
            keyValues.put(CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.FINANCIAL_OBJ_TYP_CD,
                    finObjTypeExpenditureexpCd);

            benefitsPbglEntry = (PendingBudgetConstructionGeneralLedger) businessObjectService.findByPrimaryKey(
                    PendingBudgetConstructionGeneralLedger.class, keyValues);

        }

        if (ObjectUtils.isNotNull(benefitsPbglEntry)) {

            KualiInteger oldBenefitAmount = new KualiInteger(benefitsPbglEntry.getAccountLineAnnualBalanceAmount()
                    .intValue());

            oldAnnualBenefitsAmount.put(key, oldBenefitAmount);

            BudgetConstructionMonthly monthlyBenefit = getMonthlyBenefit(monthlyBenefitsMap, benefitsPbglEntry,
                    finObjTypeExpenditureexpCd);

            if (ObjectUtils.isNotNull(monthlyBenefit)) {

                oldMonthlyBenefitsAmount.put(buildMonthlyBenefitKey(monthlyBenefit),
                        new KualiInteger(monthlyBenefit.getFinancialDocumentMonth1LineAmount().intValue()));
            }

            updateBenefitsAmount(glEntry, benefitsPbglEntry, monthlyBenefit, benefitPercent, affectedEdocs,
                    oldPBGLAmount);

            if (ObjectUtils.isNotNull(monthlyBenefit)) {

                monthlyBenefitsMap.put(buildMonthlyBenefitKey(monthlyBenefit), monthlyBenefit);
            }

            benefitsMap.put(key, benefitsPbglEntry);
        }

    }

    /**
     * Gets the monthly benefit entry for the given fringe benefit.
     * 
     * @param monthlyBenefitsMap
     * @param benefitEntry
     * @param finObjTypeExpenditureexpCd
     * @return
     */
    private BudgetConstructionMonthly getMonthlyBenefit(Map<String, BudgetConstructionMonthly> monthlyBenefitsMap,
            PendingBudgetConstructionGeneralLedger benefitEntry, String finObjTypeExpenditureexpCd) {
        BudgetConstructionMonthly monthlyEntry = new BudgetConstructionMonthly();

        String key = buildPendingBudgetConstructionGeneralLedgerKey(benefitEntry);
        if (monthlyBenefitsMap.containsKey(key)) {
            monthlyEntry = monthlyBenefitsMap.get(key);

        } else {
            Map<String, String> keyValues = new HashMap<String, String>();

            keyValues.put(CUBCPropertyConstants.BudgetConstructionMonthlyProperties.DOC_NBR,
                    benefitEntry.getDocumentNumber());
            keyValues.put(
                    CUBCPropertyConstants.BudgetConstructionMonthlyProperties.UNIVERSITY_FISCAL_YEAR,
                    benefitEntry.getUniversityFiscalYear().toString());
            keyValues.put(CUBCPropertyConstants.BudgetConstructionMonthlyProperties.CHART_CD,
                    benefitEntry.getChartOfAccountsCode());
            keyValues.put(CUBCPropertyConstants.BudgetConstructionMonthlyProperties.ACCOUNT_NBR,
                    benefitEntry.getAccountNumber());
            keyValues.put(CUBCPropertyConstants.BudgetConstructionMonthlyProperties.SUB_ACCOUNT_NBR,
                    benefitEntry.getSubAccountNumber());
            keyValues.put(CUBCPropertyConstants.BudgetConstructionMonthlyProperties.FINANCIAL_OBJ_CD,
                    benefitEntry.getFinancialObjectCode());
            keyValues.put(
                    CUBCPropertyConstants.BudgetConstructionMonthlyProperties.FINANCIAL_SUB_OBJ_CD,
                    KFSConstants.getDashFinancialSubObjectCode());
            keyValues.put(
                    CUBCPropertyConstants.BudgetConstructionMonthlyProperties.FINANCIAL_BAL_TYP_CD,
                    KFSConstants.BALANCE_TYPE_BASE_BUDGET);
            keyValues.put(CUBCPropertyConstants.BudgetConstructionMonthlyProperties.FINANCIAL_OBJ_TYP_CD,
                    finObjTypeExpenditureexpCd);

            monthlyEntry = (BudgetConstructionMonthly) businessObjectService.findByPrimaryKey(
                    BudgetConstructionMonthly.class, keyValues);
        }

        return monthlyEntry;
    }

    /**
     * Computes and updates the benefit amount on a given benefit line.
     * 
     * @param glEntry
     * @param benefitsPbglEntry
     * @param benefitPercent
     */
    private void updateBenefitsAmount(PendingBudgetConstructionGeneralLedger glEntry,
            PendingBudgetConstructionGeneralLedger benefitsPbglEntry, BudgetConstructionMonthly monthlyBenefitEntry,
            KualiPercent benefitPercent, Map<String, AffectedEdocInfo> affectedEdocs,
            KualiInteger oldPBGLAmount) {
        // compute benefits amount

        KualiInteger accountLineAnnualBalanceAmount = KualiInteger.ZERO;
        if (benefitPercent.isNonZero()) {
            KualiInteger amountToAdd = getAmountToAdd(glEntry, benefitsPbglEntry, benefitPercent, oldPBGLAmount);
            accountLineAnnualBalanceAmount = benefitsPbglEntry.getAccountLineAnnualBalanceAmount().add(amountToAdd
                    );

            ///add to the affected edocs
            String docNumber = benefitsPbglEntry.getDocumentNumber();

            if (!affectedEdocs.containsKey(docNumber)) {
                AffectedEdocInfo affectedEdocInfo = new AffectedEdocInfo(docNumber,
                        benefitsPbglEntry.getUniversityFiscalYear(), benefitsPbglEntry.getChartOfAccountsCode(),
                        benefitsPbglEntry.getAccountNumber(), benefitsPbglEntry.getSubAccountNumber(), amountToAdd);
                affectedEdocs.put(docNumber, affectedEdocInfo);
            } else {
                AffectedEdocInfo currentInfo = affectedEdocs.get(docNumber);
                KualiInteger currentAmount = currentInfo.changeAmount;
                currentInfo.changeAmount = currentAmount.add(amountToAdd);
                affectedEdocs.put(docNumber, currentInfo);
            }

            benefitsPbglEntry.setAccountLineAnnualBalanceAmount(accountLineAnnualBalanceAmount);

            if (ObjectUtils.isNotNull(monthlyBenefitEntry)) {
                KualiInteger base = KualiInteger.ZERO;
                if (monthlyBenefitEntry
                        .getFinancialDocumentMonth1LineAmount() != null) {
                    base = monthlyBenefitEntry
                            .getFinancialDocumentMonth1LineAmount();
                }
                monthlyBenefitEntry.setFinancialDocumentMonth1LineAmount(base.add(amountToAdd));
            }

        }
    }

    /**
     * Computes the amount to be added to a benefit line by subtracting the new benefit
     * amount from the old amount.
     * 
     * @param glEntry
     * @param benefitsPbglEntry
     * @param benefitPercent
     * @param oldPBGLAmount
     * @return the amount to be added
     */
    private KualiInteger getAmountToAdd(PendingBudgetConstructionGeneralLedger glEntry,
            PendingBudgetConstructionGeneralLedger benefitsPbglEntry, KualiPercent benefitPercent,
            KualiInteger oldPBGLAmount) {

        KualiInteger amountToAdd = KualiInteger.ZERO;

        if (benefitPercent.isNonZero()) {
            KualiDecimal newBenefitAmount = KualiDecimal.ZERO;
            // new benefit amount
            newBenefitAmount = new KualiDecimal(glEntry.getAccountLineAnnualBalanceAmount().bigDecimalValue()
                    .multiply(benefitPercent.bigDecimalValue().divide(new BigDecimal(100))));

            KualiDecimal oldBenefitAmount = new KualiDecimal(oldPBGLAmount.bigDecimalValue()
                    .multiply(benefitPercent.bigDecimalValue().divide(new BigDecimal(100))));

            amountToAdd =
                    new KualiInteger((newBenefitAmount.subtract(oldBenefitAmount)).bigDecimalValue());
        }
        return amountToAdd;

    }

    /**
     * Retrieve the 2PLG entries for the given BC edocs that have been affected by SIP
     * 
     * @param affectedEdocs
     * @return a list of 2PLG entries for the given BC edocs that have been affected by
     * SIP
     */
    private List<PendingBudgetConstructionGeneralLedger> get2PLGEntriesToDelete(
            Map<String, AffectedEdocInfo> affectedEdocs) {

        List<PendingBudgetConstructionGeneralLedger> _2PLGEntriesToDelete = new ArrayList<PendingBudgetConstructionGeneralLedger>();

        int fiscalYear = BudgetParameterFinder.getBaseFiscalYear() + 1;
        String objectTypeCode = optionsService.getOptions(fiscalYear).getFinObjTypeExpenditureexpCd();

        for (AffectedEdocInfo affectedEdocInfo : affectedEdocs.values()) {

            PendingBudgetConstructionGeneralLedger _2PlugEntry = new PendingBudgetConstructionGeneralLedger();

            // try to retrieve 2PLG entry if exists
            Map<String, String> keyValues = new HashMap<String, String>();
            keyValues.put(CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.DOC_NBR,
                    affectedEdocInfo.docNumber);
            keyValues.put(
                    CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.UNIVERSITY_FISCAL_YEAR,
                    String.valueOf(fiscalYear));
            keyValues.put(CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.CHART_CD,
                    affectedEdocInfo.chart);
            keyValues.put(CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.ACCOUNT_NBR,
                    affectedEdocInfo.accountNumber);
            keyValues.put(CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.SUB_ACCOUNT_NBR,
                    affectedEdocInfo.subAccount);
            keyValues.put(CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.FINANCIAL_OBJECT_CODE,
                    KFSConstants.BudgetConstructionConstants.OBJECT_CODE_2PLG);
            keyValues.put(
                    CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.FINANCIAL_SUB_OBJECT_CODE,
                    KFSConstants.getDashFinancialSubObjectCode());
            keyValues.put(
                    CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.FINANCIAL_BALANCE_TYP_CD,
                    KFSConstants.BALANCE_TYPE_BASE_BUDGET);
            keyValues.put(CUBCPropertyConstants.PendingBudgetConstructionGeneralLedgerProperties.FINANCIAL_OBJ_TYP_CD,
                    objectTypeCode);

            _2PlugEntry = (PendingBudgetConstructionGeneralLedger) businessObjectService.findByPrimaryKey(
                    PendingBudgetConstructionGeneralLedger.class, keyValues);

            if (ObjectUtils.isNotNull(_2PlugEntry)) {

                _2PLGEntriesToDelete.add(_2PlugEntry);
            }

        }

        return _2PLGEntriesToDelete;

    }

    /**
     * Retrieves the SIP pool entries for the BC edocs.
     * 
     * @param affectedEdocs
     * @return the SIP pool entries to be deleted
     */
    private List<PendingBudgetConstructionGeneralLedger> getSIPPoolEntriesToDelete(
            Map<String, AffectedEdocInfo> affectedEdocs) {

        int fiscalYear = BudgetParameterFinder.getBaseFiscalYear() + 1;

        return sipDistributionDao.getSipPoolEntries(new ArrayList<String>(affectedEdocs.keySet()),
                sipDistributionDao.getSipLevelObjectCodes(fiscalYear), fiscalYear);

    }

    /**
     * Calculates the new plug entries: PBGL entries with object code 6995 that will hold
     * the difference amount between the revenues and the expenditures (expenditure
     * amounts after SIP has been added and 2PLG entries and SIP pools removed).
     * 
     * @param affectedEdocs
     * @return a list of plug entries created as described above
     */
    public List<PendingBudgetConstructionGeneralLedger> calculateNewPlugEntries(
            Map<String, AffectedEdocInfo> affectedEdocs) {

        List<PendingBudgetConstructionGeneralLedger> newPlugEntries = new ArrayList<PendingBudgetConstructionGeneralLedger>();

        int fiscalYear = BudgetParameterFinder.getBaseFiscalYear();

        for (String docNumber : affectedEdocs.keySet()) {

            KualiInteger expenditureTotal = sipDistributionDao.getExpendituresTotal(docNumber, fiscalYear + 1);
            KualiInteger revenuesTotal = sipDistributionDao.getRevenuesTotal(docNumber, fiscalYear + 1);
            AffectedEdocInfo affectedEdocInfo = affectedEdocs.get(docNumber);
            KualiInteger changeAmount = affectedEdocInfo.changeAmount;

            KualiInteger _2PLGAmount = sipDistributionDao.get2PLGAmount(docNumber, fiscalYear + 1);

            List<String> sipLevelObjectCodes = sipDistributionDao.getSipLevelObjectCodes(fiscalYear + 1);
            KualiInteger sipPoolAmount = sipDistributionDao
                    .getSipPoolAmount(docNumber, sipLevelObjectCodes, fiscalYear + 1);

            // total amount to be subtracted
            KualiInteger totalAmountToSubtract = sipPoolAmount.add(_2PLGAmount);

            KualiInteger newPlugEntryAmount = revenuesTotal.subtract((expenditureTotal.add(changeAmount)
                    .subtract(totalAmountToSubtract)));
            
            // create or update plug entry
            String objectCode = CUBCConstants.SIP_PLUG_OBJECT_CODE;
            String subObjectCode = KFSConstants.getDashFinancialSubObjectCode();
            String objectTypeCode = optionsService.getOptions(fiscalYear + 1).getFinObjTypeExpenditureexpCd();

            PendingBudgetConstructionGeneralLedger newPlugEntry = new PendingBudgetConstructionGeneralLedger();

            newPlugEntry.setDocumentNumber(docNumber);
            newPlugEntry.setUniversityFiscalYear(fiscalYear + 1);
            newPlugEntry.setChartOfAccountsCode(affectedEdocInfo.chart);
            newPlugEntry.setAccountNumber(affectedEdocInfo.accountNumber);
            newPlugEntry.setSubAccountNumber(affectedEdocInfo.subAccount);

            newPlugEntry.setFinancialBalanceTypeCode(KFSConstants.BALANCE_TYPE_BASE_BUDGET);
            newPlugEntry.setFinancialObjectCode(objectCode);
            newPlugEntry.setFinancialObjectTypeCode(objectTypeCode);
            newPlugEntry.setFinancialSubObjectCode(subObjectCode);

            // check if plug entry exists and if it does only update that

            PendingBudgetConstructionGeneralLedger retrievedEntry = (PendingBudgetConstructionGeneralLedger) businessObjectService.retrieve(newPlugEntry);

            if (ObjectUtils.isNotNull(retrievedEntry)) {
                newPlugEntry.setFinancialBeginningBalanceLineAmount(retrievedEntry.getFinancialBeginningBalanceLineAmount());
                newPlugEntry.setAccountLineAnnualBalanceAmount(newPlugEntryAmount);
                newPlugEntry.setPersistedAccountLineAnnualBalanceAmount(newPlugEntry.getAccountLineAnnualBalanceAmount());

                newPlugEntry.setVersionNumber(retrievedEntry.getVersionNumber());
            } else {
                // otherwise create the new plug entry

                newPlugEntry.setFinancialBeginningBalanceLineAmount(KualiInteger.ZERO);
                newPlugEntry.setAccountLineAnnualBalanceAmount(newPlugEntryAmount);
                newPlugEntry.setPersistedAccountLineAnnualBalanceAmount(newPlugEntry.getAccountLineAnnualBalanceAmount());

                newPlugEntry.setVersionNumber(new Long(1));
            }

            newPlugEntries.add(newPlugEntry);
        }

        return newPlugEntries;

    }

    /**
     * Build report entries for invalid SIP import entries
     * 
     * @param reportEntries
     * @param sipImportDataCollection
     */
    protected void buildReportEntriesForInvalidEntries(StringBuilder reportEntries,
            List<SipImportData> sipImportDataCollection) {

        StringBuilder header = new StringBuilder();
        header.append("\nSIP entries that are not saved for SIP to HR batch job and are not distributed to BC \n\n");

        header.append("Unit" + SEPARATOR);
        header.append("HR Dept" + SEPARATOR);
        header.append("KFS Dept" + SEPARATOR);
        header.append("Department Name" + SEPARATOR);
        header.append("Position" + SEPARATOR);
        header.append("Position Description" + SEPARATOR);
        header.append("Emplid" + SEPARATOR);
        header.append("Person Name" + SEPARATOR);
        header.append("SIP Eligible?" + SEPARATOR);
        header.append("Empl Type" + SEPARATOR);
        header.append("Empl Rcd" + SEPARATOR);
        header.append("Job Code" + SEPARATOR);
        header.append("Job Code Desc" + SEPARATOR);
        header.append("Job Family" + SEPARATOR);
        header.append("Position Fte" + SEPARATOR);
        header.append("Position Grade Default" + SEPARATOR);
        header.append("CU State Cert" + SEPARATOR);
        header.append("Comp Freq" + SEPARATOR);
        header.append("Annual Rate" + SEPARATOR);
        header.append("Comp Rate" + SEPARATOR);
        header.append("Job Std Hrs" + SEPARATOR);
        header.append("Wrk Months" + SEPARATOR);
        header.append("Job Func" + SEPARATOR);
        header.append("Job Func Desc" + SEPARATOR);
        header.append("Increase to Min" + SEPARATOR);
        header.append("Equity" + SEPARATOR);
        header.append("Merit" + SEPARATOR);
        header.append("Note" + SEPARATOR);
        header.append("Deferred" + SEPARATOR);
        header.append("CU ABBR Flag" + SEPARATOR);
        header.append("Appt Tot Intended Amt" + SEPARATOR);
        header.append("Appt Request Fte Qty" + SEPARATOR);
        header.append("Position Type" + SEPARATOR);
        header.append("SIP Load Errors" + SEPARATOR + "\n");

        reportEntries.append(header);

        if (sipImportDataCollection != null && sipImportDataCollection.size() > 0) {
            for (SipImportData importData : sipImportDataCollection) {
                reportEntries.append(buildReportEntryForSipImportDataWithErrors(importData) + "\n");
            }
        }

    }

    /**
     * Builds report entries for HR valid SIP import entries.
     * 
     * @param reportEntries
     * @param sipImportDataCollection
     */
    protected void buildSipTotalsForHRValidEntriesReportEntries(StringBuilder reportEntries,
            List<SipImportData> sipImportDataCollection) {

        StringBuilder header = new StringBuilder();
        header.append("\nSIP Totals for entries that will be saved for SIP to HR batch job \n\n");

        header.append("Unit" + SEPARATOR);
        header.append("HR Dept" + SEPARATOR);
        header.append("KFS Dept" + SEPARATOR);
        header.append("Department Name" + SEPARATOR);
        header.append("Position" + SEPARATOR);
        header.append("Position Description" + SEPARATOR);
        header.append("Emplid" + SEPARATOR);
        header.append("Person Name" + SEPARATOR);
        header.append("Increase to Min" + SEPARATOR);
        header.append("Equity" + SEPARATOR);
        header.append("Merit" + SEPARATOR);
        header.append("Comp Rate Before SIP" + SEPARATOR);
        header.append("Comp Rate After SIP" + SEPARATOR);
        header.append("Annual Rate Before SIP" + SEPARATOR);
        header.append("Annual Rate After SIP" + "\n");

        reportEntries.append(header);

        if (sipImportDataCollection != null && sipImportDataCollection.size() > 0) {
            for (SipImportData importData : sipImportDataCollection) {
                reportEntries.append(buildReportEntryForSipImportDataWithTotals(importData) + "\n");
            }
        }
    }

    /**
     * Builds report entries for SIP import data valid for BC distribution.
     * 
     * @param reportEntries
     * @param sipImportDataCollection
     */
    protected void buildSipTotalsReportEntries(StringBuilder reportEntries, List<SipImportData> sipImportDataCollection) {

        StringBuilder header = new StringBuilder();
        header.append("\nSIP Totals for entries that will be distributed to Budget Construction \n\n");

        header.append("Unit" + SEPARATOR);
        header.append("HR Dept" + SEPARATOR);
        header.append("KFS Dept" + SEPARATOR);
        header.append("Department Name" + SEPARATOR);
        header.append("Position" + SEPARATOR);
        header.append("Position Description" + SEPARATOR);
        header.append("Emplid" + SEPARATOR);
        header.append("Person Name" + SEPARATOR);
        header.append("Increase to Min" + SEPARATOR);
        header.append("Equity" + SEPARATOR);
        header.append("Merit" + SEPARATOR);
        header.append("Comp Rate Before SIP" + SEPARATOR);
        header.append("Comp Rate After SIP" + SEPARATOR);
        header.append("Annual Rate Before SIP" + SEPARATOR);
        header.append("Annual Rate After SIP" + "\n");

        reportEntries.append(header);

        if (sipImportDataCollection != null && sipImportDataCollection.size() > 0) {
            for (SipImportData importData : sipImportDataCollection) {
                reportEntries.append(buildReportEntryForSipImportDataWithTotals(importData) + "\n");
            }
        }
    }

    /**
     * Builds a report entry for a SIP import line that has errors
     * 
     * @param importData
     * @return
     */
    private StringBuilder buildReportEntryForSipImportDataWithErrors(SipImportData importData) {

        StringBuilder reportEntry = new StringBuilder();

        reportEntry.append(importData.getUnitId() + SEPARATOR);
        reportEntry.append(importData.getHrDeptId() + SEPARATOR);
        reportEntry.append(importData.getKfsDeptId() + SEPARATOR);
        reportEntry.append(importData.getDeptName() + SEPARATOR);
        reportEntry.append(importData.getPositionNbr() + SEPARATOR);
        reportEntry.append(importData.getPosDescr() + SEPARATOR);
        reportEntry.append(importData.getEmplId() + SEPARATOR);
        reportEntry.append(importData.getPersonNm() + SEPARATOR);
        reportEntry.append(importData.getSipEligFlag() + SEPARATOR);
        reportEntry.append(importData.getEmplType() + SEPARATOR);
        reportEntry.append(importData.getEmplRcd() + SEPARATOR);
        reportEntry.append(importData.getJobCode() + SEPARATOR);
        reportEntry.append(importData.getJobCdDescShrt() + SEPARATOR);
        reportEntry.append(importData.getJobFamily() + SEPARATOR);
        reportEntry.append(importData.getPosFte() + SEPARATOR);
        reportEntry.append(importData.getPosGradeDflt() + SEPARATOR);
        reportEntry.append(importData.getCuStateCert() + SEPARATOR);
        reportEntry.append(importData.getCompFreq() + SEPARATOR);
        reportEntry.append(importData.getAnnlRt() + SEPARATOR);
        reportEntry.append(importData.getCompRt() + SEPARATOR);
        reportEntry.append(importData.getJobStdHrs() + SEPARATOR);
        reportEntry.append(importData.getWrkMnths() + SEPARATOR);
        reportEntry.append(importData.getJobFunc() + SEPARATOR);
        reportEntry.append(importData.getJobFuncDesc() + SEPARATOR);
        reportEntry.append(importData.getIncToMin() + SEPARATOR);
        reportEntry.append(importData.getEquity() + SEPARATOR);
        reportEntry.append(importData.getMerit() + SEPARATOR);
        reportEntry.append(importData.getNote() + SEPARATOR);
        reportEntry.append(importData.getDeferred() + SEPARATOR);
        reportEntry.append(importData.getCuAbbrFlag() + SEPARATOR);
        reportEntry.append(importData.getApptTotIntndAmt() + SEPARATOR);
        reportEntry.append(importData.getApptRqstFteQty() + SEPARATOR);
        reportEntry.append(importData.getPositionType() + SEPARATOR);
        reportEntry.append(importData.getSipLoadErrors());
        return reportEntry;

    }

    /**
     * Builds a report entry for a SIP import line with new totals. This method is used
     * for valid SIP entries.
     * 
     * @param importData
     * @return
     */
    private StringBuilder buildReportEntryForSipImportDataWithTotals(SipImportData importData) {

        StringBuilder reportEntry = new StringBuilder();

        reportEntry.append(importData.getUnitId() + SEPARATOR);
        reportEntry.append(importData.getHrDeptId() + SEPARATOR);
        reportEntry.append(importData.getKfsDeptId() + SEPARATOR);
        reportEntry.append(importData.getDeptName() + SEPARATOR);
        reportEntry.append(importData.getPositionNbr() + SEPARATOR);
        reportEntry.append(importData.getPosDescr() + SEPARATOR);
        reportEntry.append(importData.getEmplId() + SEPARATOR);
        reportEntry.append(importData.getPersonNm() + SEPARATOR);
        reportEntry.append(importData.getIncToMin() + SEPARATOR);
        reportEntry.append(importData.getEquity() + SEPARATOR);
        reportEntry.append(importData.getMerit() + SEPARATOR);
        reportEntry.append(importData.getCompRt() + SEPARATOR);
        reportEntry.append(importData.getNewCompRate() + SEPARATOR);
        reportEntry.append(importData.getAnnlRt() + SEPARATOR);
        reportEntry.append(importData.getNewAnnualRate());
        return reportEntry;

    }

    /**
     * Builds report entries for the updates appointment funding entries (the new
     * distributions after SIP).
     * 
     * @param reportEntries
     * @param newDistributions
     */
    protected void buildSipDistributionsReportEntries(StringBuilder reportEntries, SipImportData sipImportData,
            List<PendingBudgetConstructionAppointmentFunding> newDistributions,
            Map<String, KualiInteger> oldDisttributionAmounts,
            Map<String, KualiInteger> oldLeaveDisttributionAmounts) {

        if (newDistributions != null && newDistributions.size() > 0) {

            for (PendingBudgetConstructionAppointmentFunding appointmentFunding : newDistributions) {

                if (!appointmentFunding.isAppointmentFundingDeleteIndicator()
                        && appointmentFunding.getAppointmentRequestedAmount() != null
                        && appointmentFunding.getAppointmentRequestedAmount().isNonZero()
                        && appointmentFunding.getAppointmentRequestedTimePercent() != null
                        && (appointmentFunding.getAppointmentRequestedTimePercent().compareTo(BigDecimal.ZERO) != 0)
                        && appointmentFunding.getAppointmentRequestedFteQuantity() != null
                        && (appointmentFunding.getAppointmentRequestedFteQuantity().compareTo(BigDecimal.ZERO) != 0)) {

                    reportEntries.append(buildReportEntryForNewDistribution(sipImportData, appointmentFunding,
                            oldDisttributionAmounts.get(buildAppointmentFundingKey(appointmentFunding)))
                            + "\n");
                }

                if (appointmentFunding.getAppointmentRequestedCsfAmount() != null
                        && appointmentFunding.getAppointmentRequestedCsfAmount().isNonZero()
                        && appointmentFunding.getAppointmentRequestedCsfTimePercent() != null
                        && appointmentFunding.getAppointmentRequestedCsfTimePercent().compareTo(BigDecimal.ZERO) != 0
                        && appointmentFunding.getAppointmentRequestedCsfFteQuantity() != null
                        && (appointmentFunding.getAppointmentRequestedCsfFteQuantity().compareTo(BigDecimal.ZERO) != 0)) {

                    reportEntries.append(buildReportEntryForNewLeaveDistribution(sipImportData, appointmentFunding,
                            oldLeaveDisttributionAmounts.get(buildAppointmentFundingKey(appointmentFunding)))
                            + "\n");
                }
            }
        }
    }

    /**
     * Build report entries for the updated PBGL entries.
     * 
     * @param reportEntries
     * @param newDistributions
     */
    protected void buildSipNewPBGLReportEntries(StringBuilder reportEntries,
            Map<String, PendingBudgetConstructionGeneralLedger> newPBGLEntries, Map<String, KualiInteger> oldAmounts) {

        StringBuilder header = new StringBuilder();
        header.append("\nUpdated Expenditure Lines \n\n");
        reportEntries.append(header);
        reportEntries.append(buildReportTitleForPBGL());

        if (newPBGLEntries != null && newPBGLEntries.size() > 0) {
            for (PendingBudgetConstructionGeneralLedger pbgl : newPBGLEntries.values()) {
                KualiInteger oldAmount = oldAmounts.get(buildPendingBudgetConstructionGeneralLedgerKey(pbgl));
                reportEntries.append(buildReportEntryForPBGL(pbgl, oldAmount) + "\n");
            }
        }
    }

    /**
     * Build report entries for the updated benefits together with details on benefit
     * percent, object code and amount.
     * 
     * @param reportEntries
     * @param newDistributions
     */
    protected void buildSipNewPBGLReportEntriesWithBenefitsDetails(StringBuilder reportEntries,
            Map<String, PendingBudgetConstructionGeneralLedger> newPBGLEntries, Map<String, KualiInteger> oldAmounts,
            Map<String, List<RequestBenefits>> requestBenefitsMap) {

        StringBuilder header = new StringBuilder();
        header.append("\nUpdated Expenditure Lines With Detailed Benefits\n\n");
        reportEntries.append(header);
        reportEntries.append(buildReportTitleForPBGLWithDetailedBenefits());

        if (newPBGLEntries != null && newPBGLEntries.size() > 0) {
            for (PendingBudgetConstructionGeneralLedger pbgl : newPBGLEntries.values()) {
                String key = buildPendingBudgetConstructionGeneralLedgerKey(pbgl);
                KualiInteger oldAmount = oldAmounts.get(key);
                List<RequestBenefits> requestBenefits = requestBenefitsMap.get(key);

                String pbglReportEntry = buildReportEntryForPBGL(pbgl, oldAmount);

                if (requestBenefits != null && requestBenefits.size() > 0) {
                    for (RequestBenefits benefit : requestBenefits) {
                        reportEntries.append(pbglReportEntry + buildSipReportEntryForRequestBenefits(benefit) + "\n");
                    }
                }

            }
        }
    }

    /**
     * Build a report entry for
     * 
     * @param requestBenefits
     * @return
     */
    protected String buildSipReportEntryForRequestBenefits(RequestBenefits requestBenefits) {
        StringBuilder reportEntry = new StringBuilder();

        reportEntry.append(requestBenefits.getFinancialObjectBenefitsTypeCode() + SEPARATOR);
        reportEntry.append(requestBenefits.getFinancialObjectBenefitsTypeDescription() + SEPARATOR);
        reportEntry.append(requestBenefits.getLaborBenefitRateCategoryCode() + SEPARATOR);
        reportEntry.append(requestBenefits.getLaborBenefitRateCategoryCodeDesc() + SEPARATOR);
        reportEntry.append(requestBenefits.getPositionFringeBenefitPercent() + SEPARATOR);
        reportEntry.append(requestBenefits.getPositionFringeBenefitObjectCode() + SEPARATOR);
        reportEntry.append(requestBenefits.getPositionFringeBenefitObjectCodeName() + SEPARATOR);
        reportEntry.append(requestBenefits.getFringeDetailAmount());

        return reportEntry.toString();

    }

    /**
     * This method ...
     * 
     * @param reportEntries
     * @param newDistributions
     */
    protected void buildSipNewAnnualBenefitsReportEntries(StringBuilder reportEntries,
            Map<String, PendingBudgetConstructionGeneralLedger> newAnnualBenefitsEntries,
            Map<String, KualiInteger> oldAnnualBenefitsMap) {

        StringBuilder header = new StringBuilder();
        header.append("\nUpdated Benefits Lines \n\n");
        reportEntries.append(header);
        reportEntries.append(buildReportTitleForBenefit());

        if (newAnnualBenefitsEntries != null && newAnnualBenefitsEntries.size() > 0) {
            for (String key : newAnnualBenefitsEntries.keySet()) {
                KualiInteger oldAmount = oldAnnualBenefitsMap.get(key);
                PendingBudgetConstructionGeneralLedger newBenefit = newAnnualBenefitsEntries.get(key);
                reportEntries.append(buildReportEntryForBenefit(newBenefit, oldAmount) + "\n");
            }
        }
    }

    protected void buildSipNewMonthlyBenefitsReportEntries(StringBuilder reportEntries,
            Map<String, BudgetConstructionMonthly> monthlyBenefitsMap, Map<String, KualiInteger> oldAmounts) {

        StringBuilder header = new StringBuilder();
        header.append("\nUpdated Monthly Benefits\n\n");
        reportEntries.append(header);
        reportEntries.append(buildReportTitleForMonthlyBenefit());

        if (monthlyBenefitsMap != null && monthlyBenefitsMap.size() > 0) {
            for (BudgetConstructionMonthly newBenefit : monthlyBenefitsMap.values()) {
                reportEntries.append(buildReportEntryForMonthlyBenefit(newBenefit, oldAmounts) + "\n");
            }
        }
    }

    /**
     * This method ...
     * 
     * @param reportEntries
     * @param newDistributions
     */
    protected void buildSipNewPlugReportEntries(StringBuilder reportEntries,
            List<PendingBudgetConstructionGeneralLedger> newPlugEntries) {

        StringBuilder header = new StringBuilder();
        header.append("\nNew Plug Entries \n\n");
        reportEntries.append(header);
        reportEntries.append(buildReportTitleForPlug());

        if (newPlugEntries != null && newPlugEntries.size() > 0) {

            for (PendingBudgetConstructionGeneralLedger pbgl : newPlugEntries) {
                if (ObjectUtils.isNotNull(pbgl)) {
                    reportEntries.append(buildReportEntryForPlug(pbgl) + "\n");
                }
            }
        }
    }

    /**
     * This method ...
     * 
     * @param reportEntries
     * @param newDistributions
     */
    protected void buildSipPoolReportEntries(StringBuilder reportEntries,
            List<PendingBudgetConstructionGeneralLedger> newPlugEntries) {

        StringBuilder header = new StringBuilder();
        header.append("\n SIP Pool Entries To Be Deleted \n\n");
        reportEntries.append(header);
        reportEntries.append(buildReportTitleForPlug());

        if (newPlugEntries != null && newPlugEntries.size() > 0) {
            for (PendingBudgetConstructionGeneralLedger pbgl : newPlugEntries) {
                reportEntries.append(buildReportEntryForPlug(pbgl) + "\n");
            }
        }
    }

    /**
     * This method ...
     * 
     * @param reportEntries
     * @param newDistributions
     */
    protected void buildSip2PlugReportEntries(StringBuilder reportEntries,
            List<PendingBudgetConstructionGeneralLedger> newPlugEntries) {

        StringBuilder header = new StringBuilder();
        header.append("\n 2Plug Entries To Be Deleted \n\n");
        reportEntries.append(header);
        reportEntries.append(buildReportTitleForPlug());

        if (newPlugEntries != null && newPlugEntries.size() > 0) {
            for (PendingBudgetConstructionGeneralLedger pbgl : newPlugEntries) {
                reportEntries.append(buildReportEntryForPlug(pbgl) + "\n");
            }
        }
    }

    private String buildReportTitleForBenefit() {

        StringBuilder reportEntry = new StringBuilder();

        reportEntry.append("Account" + SEPARATOR);
        reportEntry.append("Sub Account" + SEPARATOR);
        reportEntry.append("Object" + SEPARATOR);
        reportEntry.append("Sub Object" + SEPARATOR);
        reportEntry.append("Amount Before SIP" + SEPARATOR);
        reportEntry.append("Amount After SIP" + SEPARATOR + "\n");

        return reportEntry.toString();

    }

    /**
     * This method ...
     * 
     * @param benefit
     * @return
     */
    private String buildReportEntryForBenefit(PendingBudgetConstructionGeneralLedger benefit, KualiInteger oldAmount) {

        StringBuilder reportEntry = new StringBuilder();

        reportEntry.append(benefit.getAccountNumber() + SEPARATOR);
        reportEntry.append(benefit.getSubAccountNumber() + SEPARATOR);
        reportEntry.append(benefit.getFinancialObjectCode() + SEPARATOR);
        reportEntry.append(benefit.getFinancialSubObjectCode() + SEPARATOR);
        reportEntry.append(oldAmount + SEPARATOR);
        reportEntry.append(benefit.getAccountLineAnnualBalanceAmount());

        return reportEntry.toString();

    }

    private String buildReportTitleForMonthlyBenefit() {

        StringBuilder reportEntry = new StringBuilder();

        reportEntry.append("Account" + SEPARATOR);
        reportEntry.append("Sub Account" + SEPARATOR);
        reportEntry.append("Object" + SEPARATOR);
        reportEntry.append("Sub Object" + SEPARATOR);
        reportEntry.append("Month 1 Amount Before SIP" + SEPARATOR);
        reportEntry.append("Month 1 Amount After SIP\n");

        return reportEntry.toString();

    }

    private String buildReportEntryForMonthlyBenefit(BudgetConstructionMonthly benefit,
            Map<String, KualiInteger> oldAmounts) {

        StringBuilder reportEntry = new StringBuilder();

        reportEntry.append(benefit.getAccountNumber() + SEPARATOR);
        reportEntry.append(benefit.getSubAccountNumber() + SEPARATOR);
        reportEntry.append(benefit.getFinancialObjectCode() + SEPARATOR);
        reportEntry.append(benefit.getFinancialSubObjectCode() + SEPARATOR);
        reportEntry.append(oldAmounts.get(buildMonthlyBenefitKey(benefit)) + SEPARATOR);
        reportEntry.append(benefit.getFinancialDocumentMonth1LineAmount());

        return reportEntry.toString();

    }

    private String buildReportTitleForPBGL() {
        StringBuilder reportEntry = new StringBuilder();

        reportEntry.append("Account" + SEPARATOR);
        reportEntry.append("Sub Account" + SEPARATOR);
        reportEntry.append("Object" + SEPARATOR);
        reportEntry.append("Sub Object" + SEPARATOR);
        reportEntry.append("Amount Before SIP" + SEPARATOR);
        reportEntry.append("Amount After SIP" + "\n");

        return reportEntry.toString();

    }

    private String buildReportTitleForPBGLWithDetailedBenefits() {
        StringBuilder reportEntry = new StringBuilder();

        reportEntry.append("Account" + SEPARATOR);
        reportEntry.append("Sub Account" + SEPARATOR);
        reportEntry.append("Object" + SEPARATOR);
        reportEntry.append("Sub Object" + SEPARATOR);
        reportEntry.append("Amount Before SIP" + SEPARATOR);
        reportEntry.append("Amount After SIP" + SEPARATOR);
        reportEntry.append("Labor Benefits Type Code" + SEPARATOR);
        reportEntry.append("Labor Benefits Description" + SEPARATOR);
        reportEntry.append("Labor Benefit Rate Category Code" + SEPARATOR);
        reportEntry.append("Labor Benefit Rate Category Description" + SEPARATOR);
        reportEntry.append("Position Fringe Benefit Percent" + SEPARATOR);
        reportEntry.append("Position Fringe Benefit Object Code" + SEPARATOR);
        reportEntry.append("Position Fringe Benefit Object Code Name" + SEPARATOR);
        reportEntry.append("Benefit Amount" + "\n");

        return reportEntry.toString();

    }

    private String buildReportEntryForPBGL(PendingBudgetConstructionGeneralLedger pbgl, KualiInteger oldAmount) {
        StringBuilder reportEntry = new StringBuilder();

        reportEntry.append(pbgl.getAccountNumber() + SEPARATOR);
        reportEntry.append(pbgl.getSubAccountNumber() + SEPARATOR);
        reportEntry.append(pbgl.getFinancialObjectCode() + SEPARATOR);
        reportEntry.append(pbgl.getFinancialSubObjectCode() + SEPARATOR);
        reportEntry.append(oldAmount + SEPARATOR);
        reportEntry.append(pbgl.getAccountLineAnnualBalanceAmount() + SEPARATOR);

        return reportEntry.toString();

    }

    private String buildReportTitlesForNewDistribution() {
        StringBuilder reportEntry = new StringBuilder();

        reportEntry.append("Appointment Funding Duration Code" + SEPARATOR);
        reportEntry.append("Unit" + SEPARATOR);
        reportEntry.append("HR Dept" + SEPARATOR);
        reportEntry.append("KFS Dept" + SEPARATOR);
        reportEntry.append("Department Name" + SEPARATOR);
        reportEntry.append("Position" + SEPARATOR);
        reportEntry.append("Position Description" + SEPARATOR);
        reportEntry.append("Emplid" + SEPARATOR);
        reportEntry.append("Person Name" + SEPARATOR);
        reportEntry.append("Comp Rate Before SIP" + SEPARATOR);
        reportEntry.append("Annual Rate Before SIP" + SEPARATOR);
        reportEntry.append("Increase to Min" + SEPARATOR);
        reportEntry.append("Equity" + SEPARATOR);
        reportEntry.append("Merit" + SEPARATOR);
        reportEntry.append("Comp Rate After SIP" + SEPARATOR);
        reportEntry.append("Annual Rate After SIP" + SEPARATOR);
        reportEntry.append("Chart" + SEPARATOR);
        reportEntry.append("Account" + SEPARATOR);
        reportEntry.append("Sub Account" + SEPARATOR);
        reportEntry.append("Object Code" + SEPARATOR);
        reportEntry.append("Sub Object Code" + SEPARATOR);
        reportEntry.append("Distribution" + SEPARATOR);
        reportEntry.append("Amount Before SIP" + SEPARATOR);
        reportEntry.append("Amount After SIP" + "\n");

        return reportEntry.toString();

    }

    private String buildReportEntryForNewDistribution(SipImportData sipImportData,
            PendingBudgetConstructionAppointmentFunding appointmentFunding,
            KualiInteger oldAmount) {
        StringBuilder reportEntry = new StringBuilder();

        reportEntry.append(" " + SEPARATOR);
        reportEntry.append(sipImportData.getUnitId() + SEPARATOR);
        reportEntry.append(sipImportData.getHrDeptId() + SEPARATOR);
        reportEntry.append(sipImportData.getKfsDeptId() + SEPARATOR);
        reportEntry.append(sipImportData.getDeptName() + SEPARATOR);
        reportEntry.append(appointmentFunding.getPositionNumber() + SEPARATOR);
        reportEntry.append(sipImportData.getPosDescr() + SEPARATOR);
        reportEntry.append(appointmentFunding.getEmplid() + SEPARATOR);
        reportEntry.append(sipImportData.getPersonNm() + SEPARATOR);
        reportEntry.append(sipImportData.getCompRt() + SEPARATOR);
        reportEntry.append(sipImportData.getAnnlRt() + SEPARATOR);
        reportEntry.append(sipImportData.getIncToMin() + SEPARATOR);
        reportEntry.append(sipImportData.getEquity() + SEPARATOR);
        reportEntry.append(sipImportData.getMerit() + SEPARATOR);
        reportEntry.append(sipImportData.getNewCompRate() + SEPARATOR);
        reportEntry.append(sipImportData.getNewAnnualRate() + SEPARATOR);
        reportEntry.append(appointmentFunding.getChartOfAccountsCode() + SEPARATOR);
        reportEntry.append(appointmentFunding.getAccountNumber() + SEPARATOR);
        reportEntry.append(appointmentFunding.getSubAccountNumber() + SEPARATOR);
        reportEntry.append(appointmentFunding.getFinancialObjectCode() + SEPARATOR);
        reportEntry.append(appointmentFunding.getFinancialSubObjectCode() + SEPARATOR);
        reportEntry.append(appointmentFunding.getAppointmentRequestedTimePercent() + SEPARATOR);
        reportEntry.append(oldAmount + SEPARATOR);
        reportEntry.append(appointmentFunding.getAppointmentRequestedAmount());

        return reportEntry.toString();

    }

    private String buildReportEntryForNewLeaveDistribution(SipImportData sipImportData,
            PendingBudgetConstructionAppointmentFunding appointmentFunding,
            KualiInteger oldAmount) {
        StringBuilder reportEntry = new StringBuilder();

        reportEntry.append(appointmentFunding.getAppointmentFundingDurationCode() + SEPARATOR);
        reportEntry.append(sipImportData.getUnitId() + SEPARATOR);
        reportEntry.append(sipImportData.getHrDeptId() + SEPARATOR);
        reportEntry.append(sipImportData.getKfsDeptId() + SEPARATOR);
        reportEntry.append(sipImportData.getDeptName() + SEPARATOR);
        reportEntry.append(appointmentFunding.getPositionNumber() + SEPARATOR);
        reportEntry.append(sipImportData.getPosDescr() + SEPARATOR);
        reportEntry.append(appointmentFunding.getEmplid() + SEPARATOR);
        reportEntry.append(sipImportData.getPersonNm() + SEPARATOR);
        reportEntry.append(sipImportData.getCompRt() + SEPARATOR);
        reportEntry.append(sipImportData.getAnnlRt() + SEPARATOR);
        reportEntry.append(sipImportData.getIncToMin() + SEPARATOR);
        reportEntry.append(sipImportData.getEquity() + SEPARATOR);
        reportEntry.append(sipImportData.getMerit() + SEPARATOR);
        reportEntry.append(sipImportData.getNewCompRate() + SEPARATOR);
        reportEntry.append(sipImportData.getNewAnnualRate() + SEPARATOR);
        reportEntry.append(appointmentFunding.getChartOfAccountsCode() + SEPARATOR);
        reportEntry.append(appointmentFunding.getAccountNumber() + SEPARATOR);
        reportEntry.append(appointmentFunding.getSubAccountNumber() + SEPARATOR);
        reportEntry.append(appointmentFunding.getFinancialObjectCode() + SEPARATOR);
        reportEntry.append(appointmentFunding.getFinancialSubObjectCode() + SEPARATOR);
        reportEntry.append(appointmentFunding.getAppointmentRequestedCsfTimePercent() + SEPARATOR);
        reportEntry.append(oldAmount + SEPARATOR);
        reportEntry.append(appointmentFunding.getAppointmentRequestedCsfAmount());

        return reportEntry.toString();

    }

    /**
     * Builds a key string for a PendingBudgetConstructionGeneralLedger that consists of
     * the primary key field values.
     * 
     * @param pbglEntry
     * @return a key string for a PendingBudgetConstructionGeneralLedger that consists of
     * the primary key field values.
     */
    private String buildPendingBudgetConstructionGeneralLedgerKey(PendingBudgetConstructionGeneralLedger pbglEntry) {

        StringBuilder key = new StringBuilder();

        key.append(pbglEntry.getDocumentNumber());
        key.append(pbglEntry.getUniversityFiscalYear());
        key.append(pbglEntry.getChartOfAccountsCode());
        key.append(pbglEntry.getAccountNumber());
        key.append(pbglEntry.getSubAccountNumber());
        key.append(pbglEntry.getFinancialObjectCode());
        key.append(pbglEntry.getFinancialSubObjectCode());
        key.append(pbglEntry.getFinancialObjectTypeCode());
        key.append(pbglEntry.getFinancialBalanceTypeCode());

        return key.toString();

    }

    private String buildBenefitKey(PendingBudgetConstructionGeneralLedger pbglEntry, String benefitsObjectCode,
            String finObjTypeExpenditureexpCd) {

        StringBuilder key = new StringBuilder();

        key.append(pbglEntry.getDocumentNumber());
        key.append(pbglEntry.getUniversityFiscalYear());
        key.append(pbglEntry.getChartOfAccountsCode());
        key.append(pbglEntry.getAccountNumber());
        key.append(pbglEntry.getSubAccountNumber());
        key.append(benefitsObjectCode);
        key.append(KFSConstants.getDashFinancialSubObjectCode());
        key.append(finObjTypeExpenditureexpCd);
        key.append(KFSConstants.BALANCE_TYPE_BASE_BUDGET);

        return key.toString();

    }

    private String buildMonthlyBenefitKey(BudgetConstructionMonthly monthly) {

        StringBuilder key = new StringBuilder();

        key.append(monthly.getDocumentNumber());
        key.append(monthly.getUniversityFiscalYear());
        key.append(monthly.getChartOfAccountsCode());
        key.append(monthly.getAccountNumber());
        key.append(monthly.getSubAccountNumber());
        key.append(monthly.getFinancialObjectCode());
        key.append(KFSConstants.getDashFinancialSubObjectCode());
        key.append(monthly.getFinancialObjectTypeCode());
        key.append(KFSConstants.BALANCE_TYPE_BASE_BUDGET);

        return key.toString();

    }

    private String buildReportTitleForPlug() {
        StringBuilder reportEntry = new StringBuilder();
        reportEntry.append("Account" + SEPARATOR);
        reportEntry.append("Sub Account" + SEPARATOR);
        reportEntry.append("Object" + SEPARATOR);
        reportEntry.append("Sub Object" + SEPARATOR);
        reportEntry.append("Annual Amount" + SEPARATOR + "\n");

        return reportEntry.toString();

    }

    private String buildReportEntryForPlug(PendingBudgetConstructionGeneralLedger pbgl) {
        StringBuilder reportEntry = new StringBuilder();
        reportEntry.append(pbgl.getAccountNumber() + SEPARATOR);
        reportEntry.append(pbgl.getSubAccountNumber() + SEPARATOR);
        reportEntry.append(pbgl.getFinancialObjectCode() + SEPARATOR);
        reportEntry.append(pbgl.getFinancialSubObjectCode() + SEPARATOR);
        reportEntry.append(pbgl.getAccountLineAnnualBalanceAmount() + SEPARATOR);

        return reportEntry.toString();

    }

    /**
     * Gets the businessObjectService.
     * 
     * @return businessObjectService
     */
    public BusinessObjectService getBusinessObjectService() {
        return businessObjectService;
    }

    /**
     * Sets the businessObjectService.
     * 
     * @param businessObjectService
     */
    public void setBusinessObjectService(BusinessObjectService businessObjectService) {
        this.businessObjectService = businessObjectService;
    }

    /**
     * Gets the optionsService.
     * 
     * @return optionsService
     */
    public OptionsService getOptionsService() {
        return optionsService;
    }

    /**
     * Sets the optionsService.
     * 
     * @param optionsService
     */
    public void setOptionsService(OptionsService optionsService) {
        this.optionsService = optionsService;
    }

    public SipDistributionDao getSipDistributionDao() {
        return sipDistributionDao;
    }

    public void setSipDistributionDao(SipDistributionDao sipDistributionDao) {
        this.sipDistributionDao = sipDistributionDao;
    }

    private class AffectedEdocInfo {
        protected String docNumber;
        protected int fiscalYear;
        protected String chart;
        protected String accountNumber;
        protected String subAccount;

        protected KualiInteger changeAmount;

        public AffectedEdocInfo(String docNumber,
                int fiscalYear,
                String chart,
                String accountNumber,
                String subAccount,
                KualiInteger changeAmount) {

            this.docNumber = docNumber;

            this.fiscalYear = fiscalYear;
            this.accountNumber = accountNumber;
            this.chart = chart;
            this.subAccount = subAccount;

            this.changeAmount = changeAmount;
        }

    }

}
