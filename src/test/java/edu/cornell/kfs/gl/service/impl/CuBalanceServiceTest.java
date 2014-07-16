package edu.cornell.kfs.gl.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.kuali.kfs.coa.service.ObjectTypeService;
import org.kuali.kfs.gl.GeneralLedgerConstants;
import org.kuali.kfs.gl.businessobject.Balance;
import org.kuali.kfs.sys.ConfigureContext;
import org.kuali.kfs.sys.businessobject.SystemOptions;
import org.kuali.kfs.sys.context.KualiTestBase;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.kfs.sys.dataaccess.UnitTestSqlDao;
import org.kuali.kfs.sys.service.OptionsService;
import org.kuali.kfs.sys.service.impl.KfsParameterConstants;
import org.kuali.rice.core.api.util.type.KualiDecimal;
import org.kuali.rice.coreservice.framework.parameter.ParameterService;

import edu.cornell.kfs.gl.service.CuBalanceService;


/**
 * various tests for CuBalanceService
 */
@ConfigureContext
public class CuBalanceServiceTest extends KualiTestBase {
	
	private CuBalanceService balanceService;
	private static int closingFiscalYear;
	private static List<String> charts;
	
    private final static String ACCOUNT_NUMBER = "1000710";
    private final static String ACCOUNT_NUMBER_CUMULATIVE_BALANCES_TO_FORWARD = "1006094";
    private final static String SUB_ACCOUNT_CUMULATIVE_BALANCES_TO_FORWARD = "-----";
    private final static String CHART = "IT";
    private final static String SUB_ACCT_NUMBER = "sub";
    private final static String SUB_OBJECT_CODE = "123";
    private final static String BALANCE_TYPE_AC = "AC";
    private final static String OBJECT_TYPE_ASSET = "AS";
    private final static String OBJECT_TYPE_EX = "EX";
    private final static String OBJECT_TYPE_EE = "EE";
    private final static String OBJECT_TYPE_IN = "IN";
    private final static String OBJECT_CODE = "5000";
    private final static String OBJECT_CODE_CUMULATIVE_BALANCES_TO_FORWARD = "4430";

    private static String DELETE_BALANCES = "delete from GL_BALANCE_T where ";
    private static String RAW_BALANCES = "select * from GL_BALANCE_T where ";
    private static String INSERT_BALANCE = "insert into GL_BALANCE_T(FIN_COA_CD,UNIV_FISCAL_YR,ACCOUNT_NBR,SUB_ACCT_NBR,FIN_SUB_OBJ_CD,FIN_OBJECT_CD,FIN_BALANCE_TYP_CD,FIN_OBJ_TYP_CD,FIN_BEG_BAL_LN_AMT,ACLN_ANNL_BAL_AMT,CONTR_GR_BB_AC_AMT) values('" + CHART + "',";
    private UnitTestSqlDao unitTestSqlDao;
    private static boolean runOnce = true;
    

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        balanceService = SpringContext.getBean(CuBalanceService.class);
        unitTestSqlDao = SpringContext.getBean(UnitTestSqlDao.class);
        
        if (runOnce) {
        	closingFiscalYear = new Integer(SpringContext.getBean(ParameterService.class).getParameterValueAsString(KfsParameterConstants.GENERAL_LEDGER_BATCH.class, GeneralLedgerConstants.ANNUAL_CLOSING_FISCAL_YEAR_PARM));
        	charts = new ArrayList<>(); 
        	charts.add(CHART);
        	
            DELETE_BALANCES += "UNIV_FISCAL_YR=" + closingFiscalYear + " AND FIN_COA_CD='" + CHART + "'";
            INSERT_BALANCE += closingFiscalYear + ",";
            RAW_BALANCES += "UNIV_FISCAL_YR=" + closingFiscalYear + " AND FIN_COA_CD='" + CHART + "'";
            
            runOnce = false; // do not run again
        }

    }
    
    /**
     * 
     * This method generates and calls and SQL command to remove all test data from the database.
     */
    public void purgeTestData() {
        unitTestSqlDao.sqlCommand(DELETE_BALANCES);

        List results = unitTestSqlDao.sqlSelect(RAW_BALANCES);
        assertNotNull("List shouldn't be null", results);
        assertEquals("Should return 0 results", 0, results.size());

    }
    
    /**
     * This method creates and makes and SQL command call to perform and insert passing in the provided parameters.
     * 
     * @param insertSQL The insert SQL string
     * @param account The account number to be inserted.
     * @param subAccount The sub account number to be inserted.
     * @param subObject The sub object code to be inserted.
     * @param objectTypeCode The object type code to be inserted.
     * @param balanceTypeCode The balance type code to be inserted.
     * @param objectCode The object code to be inserted.
     * @param beginningAmount The beginning amount to be inserted.
     * @param finalAmount The final amount to be inserted.
     * @param contractsGrantsBeginningBalanceAmount The contractsGrantsBeginningBalanceAmount to be inserted.
     */
    private void insertBalance(String insertSQL,String account, String subAccount, String subObject, String objectTypeCode, String balanceTypeCode, String objectCode, KualiDecimal beginningAmount, KualiDecimal finalAmount, KualiDecimal contractsGrantsBeginningBalanceAmount) {
        unitTestSqlDao.sqlCommand(insertSQL + "'" + account + "','" + subAccount + "','"  + subObject + "','" + objectCode + "','" + balanceTypeCode + "','" + objectTypeCode + "'," + beginningAmount + "," + finalAmount + "," + contractsGrantsBeginningBalanceAmount +")");
    } 
    
    public void testCountBalancesForFiscalYear(){
    	purgeTestData();
    	insertBalance(INSERT_BALANCE, ACCOUNT_NUMBER, SUB_ACCT_NUMBER, SUB_OBJECT_CODE, OBJECT_TYPE_EE, BALANCE_TYPE_AC, OBJECT_CODE, new KualiDecimal(1), new KualiDecimal(1), KualiDecimal.ZERO);
    	List newBalances = unitTestSqlDao.sqlSelect(RAW_BALANCES);
    	
    	assertEquals(1, newBalances.size());

    	assertEquals(1, balanceService.countBalancesForFiscalYear(closingFiscalYear, charts));
    }
    
    public void testFindNominalActivityBalancesForFiscalYear(){
        
    	purgeTestData();
    	insertBalance(INSERT_BALANCE, ACCOUNT_NUMBER, SUB_ACCT_NUMBER, SUB_OBJECT_CODE, OBJECT_TYPE_EX, BALANCE_TYPE_AC, OBJECT_CODE, new KualiDecimal(1), new KualiDecimal(1), KualiDecimal.ZERO);
    	
    	Iterator<Balance> balanceIterator = balanceService.findNominalActivityBalancesForFiscalYear(closingFiscalYear, charts);
    	int nominalActivityBalancesCounter = 0;
    	
    	while (balanceIterator.hasNext()) {
    		Balance balance = balanceIterator.next();
    		nominalActivityBalancesCounter++;
    		if(!(nominalActivityBalancesCounter==0 || nominalActivityBalancesCounter==1)){
    			break;
    		}
    	}
    	
    	assertEquals(1,nominalActivityBalancesCounter);
    }
    
    public void testFindGeneralBalancesToForwardForFiscalYear(){
        
    	purgeTestData();
    	insertBalance(INSERT_BALANCE, ACCOUNT_NUMBER, SUB_ACCT_NUMBER, SUB_OBJECT_CODE, OBJECT_TYPE_ASSET, BALANCE_TYPE_AC, OBJECT_CODE, new KualiDecimal(1), new KualiDecimal(2), new KualiDecimal(3));
    	
    	Iterator<Balance> balanceIterator = balanceService.findGeneralBalancesToForwardForFiscalYear(closingFiscalYear, charts);
    	int nominalActivityBalancesCounter = 0;
    	
    	while (balanceIterator.hasNext()) {
    		Balance balance = balanceIterator.next();
    		nominalActivityBalancesCounter++;
    		if(!(nominalActivityBalancesCounter==0 || nominalActivityBalancesCounter==1)){
    			break;
    		}
    	}
    	
    	assertEquals(1,nominalActivityBalancesCounter);
    }
    
    public void testFindCumulativeBalancesToForwardForFiscalYear(){
        
    	purgeTestData();
    	insertBalance(INSERT_BALANCE, ACCOUNT_NUMBER_CUMULATIVE_BALANCES_TO_FORWARD, SUB_ACCOUNT_CUMULATIVE_BALANCES_TO_FORWARD, SUB_OBJECT_CODE, OBJECT_TYPE_IN, BALANCE_TYPE_AC, OBJECT_CODE_CUMULATIVE_BALANCES_TO_FORWARD, new KualiDecimal(1), new KualiDecimal(2), new KualiDecimal(3));
    	
    	Iterator<Balance> balanceIterator = balanceService.findCumulativeBalancesToForwardForFiscalYear(closingFiscalYear, charts);
    	int nominalActivityBalancesCounter = 0;
    	
    	while (balanceIterator.hasNext()) {
    		Balance balance = balanceIterator.next();
    		nominalActivityBalancesCounter++;
    		if(!(nominalActivityBalancesCounter==0 || nominalActivityBalancesCounter==1)){
    			break;
    		}
    	}
    	
    	assertEquals(1,nominalActivityBalancesCounter);
    }

}
