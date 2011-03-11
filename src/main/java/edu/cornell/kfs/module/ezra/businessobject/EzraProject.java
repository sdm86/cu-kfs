/**
 * 
 */
package edu.cornell.kfs.module.ezra.businessobject;

import java.sql.Date;
import java.util.LinkedHashMap;

import org.kuali.rice.kim.bo.Person;
import org.kuali.rice.kns.bo.PersistableBusinessObjectBase;

/**
 * @author kwk43
 *
 */
public class EzraProject extends PersistableBusinessObjectBase {

	private Long projectId;
	private Long projectDirectorId;
	private Long projectDepartmentId;
	private Date lastUpdated;
	
	
	/**
	 * @return the projectId
	 */
	public Long getProjectId() {
		return projectId;
	}

	/**
	 * @param projectId the projectId to set
	 */
	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	
	/**
	 * @return the projectDirectorId
	 */
	public Long getProjectDirectorId() {
		return projectDirectorId;
	}

	/**
	 * @param projectDirectorId the projectDirectorId to set
	 */
	public void setProjectDirectorId(Long projectDirectorId) {
		this.projectDirectorId = projectDirectorId;
	}

	/**
	 * @return the projectDepartmentId
	 */
	public Long getProjectDepartmentId() {
		return projectDepartmentId;
	}

	/**
	 * @param projectDepartmentId the projectDepartmentId to set
	 */
	public void setProjectDepartmentId(Long projectDepartmentId) {
		this.projectDepartmentId = projectDepartmentId;
	}

	/**
	 * @return the lastUpdated
	 */
	public Date getLastUpdated() {
		return lastUpdated;
	}

	/**
	 * @param lastUpdated the lastUpdated to set
	 */
	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	/* (non-Javadoc)
	 * @see org.kuali.rice.kns.bo.BusinessObjectBase#toStringMapper()
	 */
	@Override
	protected LinkedHashMap toStringMapper() {
		LinkedHashMap m = new LinkedHashMap();

        m.put("projectId", projectId);
	    return m;
	}

}
