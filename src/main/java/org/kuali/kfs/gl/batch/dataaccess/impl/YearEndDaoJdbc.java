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
package org.kuali.kfs.gl.batch.dataaccess.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.kuali.kfs.gl.batch.dataaccess.YearEndDao;
import org.kuali.rice.kns.dao.jdbc.PlatformAwareDaoBaseJdbc;
import org.springframework.jdbc.core.RowMapper;

/**
 * A JDBC implementation of the YearEndDao, built mainly because OJB is darn slow at some queries
 */
public class YearEndDaoJdbc extends PlatformAwareDaoBaseJdbc implements YearEndDao {

    // All of the Comparators and RowMappers are stateless, so I can simply create them as variables and avoid unnecessary object
    // creation
    protected Comparator<Map<String, String>> subFundGroupPrimaryKeyComparator = new Comparator<Map<String, String>>() {
        public int compare(Map<String, String> firstSubFundGroupPK, Map<String, String> secondSubFundGroupPK) {
            return firstSubFundGroupPK.get("subFundGroupCode").compareTo(secondSubFundGroupPK.get("subFundGroupCode"));
        }
    };

    protected Comparator<Map<String, String>> priorYearAccountPrimaryKeyComparator = new Comparator<Map<String, String>>() {
        public int compare(Map<String, String> firstPriorYearPK, Map<String, String> secondPriorYearPK) {
            if (firstPriorYearPK.get("chartOfAccountsCode").equals(secondPriorYearPK.get("chartOfAccountsCode"))) {
                return firstPriorYearPK.get("accountNumber").compareTo(secondPriorYearPK.get("accountNumber"));
            }
            else {
                return firstPriorYearPK.get("chartOfAccountsCode").compareTo(secondPriorYearPK.get("chartOfAccountsCode"));
            }
        }
    };

    protected RowMapper subFundGroupRowMapper = new RowMapper() {
        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            Map<String, String> subFundGroupKey = new LinkedHashMap<String, String>();
            subFundGroupKey.put("subFundGroupCode", rs.getString("sub_fund_grp_cd"));
            return subFundGroupKey;
        }
    };

    protected RowMapper priorYearAccountRowMapper = new RowMapper() {
        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            Map<String, String> keyMap = new LinkedHashMap<String, String>();
            keyMap.put("chartOfAccountsCode", rs.getString("fin_coa_cd"));
            keyMap.put("accountNumber", rs.getString("account_nbr"));
            return keyMap;
        }
    };

    /**
     * Queries the databse to find missing prior year accounts
     * 
     * @param balanceFiscalyear the fiscal year of balances to check for missing prior year accounts for
     * @return a Set of Maps holding the primary keys of missing prior year accounts
     * @see org.kuali.kfs.gl.batch.dataaccess.YearEndDao#findKeysOfMissingPriorYearAccountsForBalances(java.lang.Integer)
     */
    public Set<Map<String, String>> findKeysOfMissingPriorYearAccountsForBalances(Integer balanceFiscalYear) {
        // 1. get a sorted list of the prior year account keys that are used by balances for the given fiscal year
        List priorYearKeys = getJdbcTemplate().query("select distinct fin_coa_cd, account_nbr from gl_balance_t where univ_fiscal_yr = ? order by fin_coa_cd, account_nbr", new Object[] { balanceFiscalYear }, priorYearAccountRowMapper);

        // 2. go through that list, finding which prior year accounts don't show up in the database
        return selectMissingPriorYearAccounts(priorYearKeys);
    }
    
    /**
     * Queries the database to find missing prior year accounts
     * 
     * @param balanceFiscalyear the fiscal year of balances to check for missing prior year accounts for
     * @param chartsList list of charts to find balances for
     * @return a Set of Maps holding the primary keys of missing prior year accounts
     * @see org.kuali.kfs.gl.batch.dataaccess.YearEndDao#findKeysOfMissingPriorYearAccountsForBalances(java.lang.Integer, List<String>)
     */
    public Set<Map<String, String>> findKeysOfMissingPriorYearAccountsForBalances(Integer balanceFiscalYear, List<String> chartsList) {
    	//0. create the JDBC SQL string to run
    	StringBuilder buildQuery = new StringBuilder();
    	buildQuery.append("select distinct fin_coa_cd, account_nbr from gl_balance_t where univ_fiscal_yr = "+balanceFiscalYear+" and fin_coa_cd in "+formatListForSqlInClause(chartsList)+" order by fin_coa_cd, account_nbr");
    	
    	// 1. get a sorted list of the prior year account keys that are used by balances for the given fiscal year and list of charts
    	List priorYearKeys = getJdbcTemplate().query(buildQuery.toString(), priorYearAccountRowMapper);   	
    	
        // 2. go through that list, finding which prior year accounts don't show up in the database
        return selectMissingPriorYearAccounts(priorYearKeys);
    }
    
    /**
     * This method puts all of the prior year accounts that aren't in the database, based on the list of keys sent in, into the
     * given set
     * 
     * @param priorYearKeys the prior year keys to search for
     * @return the set of those prior year accounts that are missing
     */
    protected Set<Map<String, String>> selectMissingPriorYearAccounts(List priorYearKeys) {
        Set<Map<String, String>> missingPriorYears = new TreeSet<Map<String, String>>(priorYearAccountPrimaryKeyComparator);
        for (Object priorYearKeyAsObject : priorYearKeys) {
            Map<String, String> priorYearKey = (Map<String, String>) priorYearKeyAsObject;
            int count = getJdbcTemplate().queryForInt("select count(*) from ca_prior_yr_acct_t where fin_coa_cd = ? and account_nbr = ? order by sub_fund_grp_cd", new Object[] { priorYearKey.get("chartOfAccountsCode"), priorYearKey.get("accountNumber") });
            if (count == 0) {
                missingPriorYears.add(priorYearKey);
            }
        }
        return missingPriorYears;
    }

    /**
     * Queries the database to find missing sub fund groups
     * 
     * @param balanceFiscalYear the fiscal year of the balance to find missing sub fund groups for
     * @return a Set of Maps holding the primary keys of missing sub fund groups
     * @see org.kuali.kfs.gl.batch.dataaccess.YearEndDao#findKeysOfMissingSubFundGroupsForBalances(java.lang.Integer)
     */
    public Set<Map<String, String>> findKeysOfMissingSubFundGroupsForBalances(Integer balanceFiscalYear) {
        // see algorithm for findKeysOfMissingPriorYearAccountsForBalances
        List subFundGroupKeys = getJdbcTemplate().query("select distinct ca_prior_yr_acct_t.sub_fund_grp_cd from ca_prior_yr_acct_t, gl_balance_t where ca_prior_yr_acct_t.fin_coa_cd = gl_balance_t.fin_coa_cd and ca_prior_yr_acct_t.account_nbr = gl_balance_t.account_nbr and gl_balance_t.univ_fiscal_yr = ? and ca_prior_yr_acct_t.sub_fund_grp_cd is not null order by ca_prior_yr_acct_t.sub_fund_grp_cd", new Object[] { balanceFiscalYear }, subFundGroupRowMapper);
        return selectMissingSubFundGroups(subFundGroupKeys);
    }
    
    /**
     * Queries the database to find missing sub fund groups
     * 
     * @param balanceFiscalYear the fiscal year of the balance to find missing sub fund groups for
     * @param chartsList list of charts to use when finding the balance of missing sub fund groups
     * @return a Set of Maps holding the primary keys of missing sub fund groups
     * @see org.kuali.kfs.gl.batch.dataaccess.YearEndDao#findKeysOfMissingSubFundGroupsForBalances(java.lang.Integer, java.util.List)
     */
    public Set<Map<String, String>> findKeysOfMissingSubFundGroupsForBalances(Integer balanceFiscalYear, List<String> chartsList) {
        // see algorithm for findKeysOfMissingPriorYearAccountsForBalances
    	StringBuilder buildQuery = new StringBuilder();
    	buildQuery.append("select distinct ca_prior_yr_acct_t.sub_fund_grp_cd from ca_prior_yr_acct_t, gl_balance_t where ca_prior_yr_acct_t.fin_coa_cd = gl_balance_t.fin_coa_cd and ca_prior_yr_acct_t.account_nbr = gl_balance_t.account_nbr and gl_balance_t.univ_fiscal_yr = "+balanceFiscalYear+" and gl_balance_t.fin_coa_cd in "+formatListForSqlInClause(chartsList)+" and ca_prior_yr_acct_t.sub_fund_grp_cd is not null order by ca_prior_yr_acct_t.sub_fund_grp_cd");

    	List subFundGroupKeys = getJdbcTemplate().query(buildQuery.toString(), subFundGroupRowMapper);
        return selectMissingSubFundGroups(subFundGroupKeys);
    }

    /**
     * This method puts all of the sub fund groups that are in the given list of subFundGroupKeys but aren't in the database into
     * the given set
     * 
     * @param subFundGroupKeys the list of sub fund group keys to search through
     * @return a set of those sub fund group keys that are missing
     */
    protected Set<Map<String, String>> selectMissingSubFundGroups(List subFundGroupKeys) {
        Set<Map<String, String>> missingSubFundGroups = new TreeSet<Map<String, String>>(subFundGroupPrimaryKeyComparator);
        for (Object subFundGroupKeyAsObject : subFundGroupKeys) {
            Map<String, String> subFundGroupKey = (Map<String, String>) subFundGroupKeyAsObject;
            int count = getJdbcTemplate().queryForInt("select count(*) from ca_sub_fund_grp_t where sub_fund_grp_cd = ?", new Object[] { subFundGroupKey.get("subFundGroupCode") });
            if (count == 0) {
                missingSubFundGroups.add(subFundGroupKey);
            }
        }
        return missingSubFundGroups;
    }

    /**
     * Queries the databsae to find missing prior year account records referred to by encumbrance records
     * 
     * @param encumbranceFiscalYear the fiscal year of balances to find missing encumbrance records for
     * @return a Set of Maps holding the primary keys of missing prior year accounts
     * @see org.kuali.kfs.gl.batch.dataaccess.YearEndDao#findKeysOfMissingPriorYearAccountsForOpenEncumbrances(java.lang.Integer)
     */
    public Set<Map<String, String>> findKeysOfMissingPriorYearAccountsForOpenEncumbrances(Integer encumbranceFiscalYear) {
        List priorYearKeys = getJdbcTemplate().query("select distinct fin_coa_cd, account_nbr from gl_encumbrance_t where univ_fiscal_yr = ? and acln_encum_amt <> acln_encum_cls_amt order by fin_coa_cd, account_nbr", new Object[] { encumbranceFiscalYear }, priorYearAccountRowMapper);
        return selectMissingPriorYearAccounts(priorYearKeys);
    }
    
    /**
     * Queries the database to find missing prior year account records for specified charts referred to by encumbrance records
     * 
     * @param encumbranceFiscalYear the fiscal year of balances to find missing encumbrance records for
     * @param encumbranceCharts list of charts to find missing encumbrances for
     * @return a Set of Maps holding the primary keys of missing prior year accounts
     * @see org.kuali.kfs.gl.batch.dataaccess.YearEndDao#findKeysOfMissingPriorYearAccountsForOpenEncumbrances(java.lang.Integer, java.util.List)
     */
    public Set<Map<String, String>> findKeysOfMissingPriorYearAccountsForOpenEncumbrances(Integer encumbranceFiscalYear, List <String> encumbranceCharts) {
    	
    	StringBuilder buildQuery = new StringBuilder();
    	buildQuery.append("select distinct fin_coa_cd, account_nbr from gl_encumbrance_t where univ_fiscal_yr = "+encumbranceFiscalYear+" and fin_coa_cd in "+formatListForSqlInClause(encumbranceCharts).toString()+" and acln_encum_amt <> acln_encum_cls_amt order by fin_coa_cd, account_nbr");
    	
    	List priorYearKeys = getJdbcTemplate().query(buildQuery.toString(), priorYearAccountRowMapper);
        return selectMissingPriorYearAccounts(priorYearKeys);
    }

    /**
     * Queries the database to find missing sub fund group records referred to by encumbrances
     * 
     * @param  encumbranceFiscalYear the fiscal year of encumbrances to find missing sub fund group records for
     * @return a Set of Maps holding the primary keys of missing sub fund group records
     * @see org.kuali.kfs.gl.batch.dataaccess.YearEndDao#findKeysOfMissingSubFundGroupsForOpenEncumbrances(java.lang.Integer)
     */
    public Set<Map<String, String>> findKeysOfMissingSubFundGroupsForOpenEncumbrances(Integer encumbranceFiscalYear) {
        List subFundGroupKeys = getJdbcTemplate().query("select distinct ca_prior_yr_acct_t.sub_fund_grp_cd from ca_prior_yr_acct_t, gl_encumbrance_t where ca_prior_yr_acct_t.fin_coa_cd = gl_encumbrance_t.fin_coa_cd and ca_prior_yr_acct_t.account_nbr = gl_encumbrance_t.account_nbr and gl_encumbrance_t.univ_fiscal_yr = ? and gl_encumbrance_t.acln_encum_amt <> gl_encumbrance_t.acln_encum_cls_amt and ca_prior_yr_acct_t.sub_fund_grp_cd is not null order by ca_prior_yr_acct_t.sub_fund_grp_cd", new Object[] { encumbranceFiscalYear }, subFundGroupRowMapper);
        return selectMissingSubFundGroups(subFundGroupKeys);
    }
    
    /**
     * Queries the database to find missing sub fund group records referred to by encumbrances
     * 
     * @param  encumbranceFiscalYear the fiscal year of encumbrances to find missing sub fund group records for
     * @param  encumbranceCharts charts of encumbrances to find missing sub fund group records for
     * @return a Set of Maps holding the primary keys of missing sub fund group records
     * @see org.kuali.kfs.gl.batch.dataaccess.YearEndDao#findKeysOfMissingSubFundGroupsForOpenEncumbrances(java.lang.Integer, java.util.List)
     */
    public Set<Map<String, String>> findKeysOfMissingSubFundGroupsForOpenEncumbrances(Integer encumbranceFiscalYear, List<String> encumbranceCharts) {
    	StringBuilder buildQuery = new StringBuilder();
    	buildQuery.append("select distinct ca_prior_yr_acct_t.sub_fund_grp_cd from ca_prior_yr_acct_t, gl_encumbrance_t where ca_prior_yr_acct_t.fin_coa_cd = gl_encumbrance_t.fin_coa_cd and ca_prior_yr_acct_t.account_nbr = gl_encumbrance_t.account_nbr and gl_encumbrance_t.univ_fiscal_yr = "+encumbranceFiscalYear+" and gl_encumbrance_t.fin_coa_cd in "+formatListForSqlInClause(encumbranceCharts)+" and gl_encumbrance_t.acln_encum_amt <> gl_encumbrance_t.acln_encum_cls_amt and ca_prior_yr_acct_t.sub_fund_grp_cd is not null order by ca_prior_yr_acct_t.sub_fund_grp_cd");
    	
   	    List subFundGroupKeys = getJdbcTemplate().query(buildQuery.toString(), subFundGroupRowMapper);
        return selectMissingSubFundGroups(subFundGroupKeys);
    }
        
    /**
     * 
     * @param valuesListToFormat
     * @return String representing the values to use as the "in" clause for JDBC query
     */
    private String formatListForSqlInClause(List<String> valuesListToFormat) {
    	//NOTE: presuming input parameter contains one or more values and is NOT empty as this method should not have been called otherwise
    	Iterator<String> listIterator = valuesListToFormat.iterator();
    	StringBuilder valuesFormattedForInClause = new StringBuilder();
    	valuesFormattedForInClause.append("(");
    	while (listIterator.hasNext()) {
    		valuesFormattedForInClause.append("'");
    		valuesFormattedForInClause.append(listIterator.next().toString());
    		if (listIterator.hasNext()){
    			valuesFormattedForInClause.append("',");
    		}
    		else {    			
    			valuesFormattedForInClause.append("')");   			
    		}
    	}
    	return valuesFormattedForInClause.toString();
    }
    
}