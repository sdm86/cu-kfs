package edu.cornell.kfs.module.bc.businessobject;

import java.util.LinkedHashMap;

import org.kuali.rice.core.api.util.type.KualiDecimal;
import org.kuali.rice.krad.bo.PersistableBusinessObjectBase;
import org.kuali.rice.krad.util.ObjectUtils;

@SuppressWarnings("serial")
public class SipImportData extends PersistableBusinessObjectBase {
    protected String unitId;
    protected String hrDeptId;
    protected String kfsDeptId;
    protected String deptName;
    protected String positionNbr;
    protected String posDescr;
    protected String emplId;
    protected String personNm;
    protected String sipEligFlag;
    protected String emplType;
    protected String emplRcd;
    protected String jobCode;
    protected String jobCdDescShrt;
    protected String jobFamily;
    protected KualiDecimal posFte;
    protected String posGradeDflt;
    protected String cuStateCert;
    protected String compFreq;
    protected KualiDecimal annlRt;
    protected KualiDecimal compRt;
    protected KualiDecimal jobStdHrs;
    protected KualiDecimal wrkMnths;
    protected String jobFunc;
    protected String jobFuncDesc;
    protected KualiDecimal incToMin;
    protected KualiDecimal equity;
    protected KualiDecimal merit;
    protected String note;
    protected KualiDecimal deferred;
    protected String cuAbbrFlag;
    protected KualiDecimal apptTotIntndAmt;
    protected KualiDecimal apptRqstFteQty;
    protected String positionType;
    protected KualiDecimal newCompRate;
    protected KualiDecimal newAnnualRate;
    
    // The following are not part of the SIP import but instead are used to (the distribution process) determine
    //   if the line passed validation and if not what rules were responsible for it not passing validation.
    
    protected String passedValidation;
    protected String validationErrors;
    protected String sipLoadErrors;

    public SipImportData() {
        super();
    }

    @SuppressWarnings("rawtypes")
	protected LinkedHashMap toStringMapper() {
        return null;
    }

    /**
     * @return the unitId
     */
    public String getUnitId() {
        return unitId;
    }

    /**
     * @param unitId the unitId to set
     */
    public void setUnitId(String unitId) {
        this.unitId = (ObjectUtils.isNull(unitId)) ? "" : unitId;
    }

    /**
     * @return the hrDeptId
     */
    public String getHrDeptId() {
        return hrDeptId;
    }

    /**
     * @param hrDeptId the hrDeptId to set
     */
    public void setHrDeptId(String hrDeptId) {
        this.hrDeptId = (ObjectUtils.isNull(hrDeptId)) ? "" : hrDeptId;
    }

    /**
     * @return the kfsDeptId
     */
    public String getKfsDeptId() {
        return kfsDeptId;
    }

    /**
     * @param kfsDeptId the kfsDeptId to set
     */
    public void setKfsDeptId(String kfsDeptId) {
        this.kfsDeptId = (ObjectUtils.isNull(kfsDeptId)) ? "" : kfsDeptId;
    }

    /**
     * @return the deptName
     */
    public String getDeptName() {
        return deptName;
    }

    /**
     * @param deptName the deptName to set
     */
    public void setDeptName(String deptName) {
        this.deptName = (ObjectUtils.isNull(deptName)) ? "" : deptName;
    }

    /**
     * @return the positionNbr
     */
    public String getPositionNbr() {
        return positionNbr;
    }

    /**
     * @param positionNbr the positionNbr to set
     */
    public void setPositionNbr(String positionNbr) {
        this.positionNbr = (ObjectUtils.isNull(positionNbr)) ? "" : positionNbr;
    }

    /**
     * @return the posDescr
     */
    public String getPosDescr() {
        return posDescr;
    }

    /**
     * @param posDescr the posDescr to set
     */
    public void setPosDescr(String posDescr) {
        this.posDescr = (ObjectUtils.isNull(posDescr)) ? "" : posDescr;
    }

    /**
     * @return the emplId
     */
    public String getEmplId() {
        return emplId;
    }

    /**
     * @param emplId the emplId to set
     */
    public void setEmplId(String emplId) {
        this.emplId = (ObjectUtils.isNull(emplId)) ? "" : emplId;
    }

    /**
     * @return the personNm
     */
    public String getPersonNm() {
        return personNm;
    }

    /**
     * @param personNm the personNm to set
     */
    public void setPersonNm(String personNm) {
        this.personNm = (ObjectUtils.isNull(personNm)) ? "" : personNm;
    }

    /**
     * @return the sipEligFlag
     */
    public String getSipEligFlag() {
        return sipEligFlag;
    }

    /**
     * @param sipEligFlag the sipEligFlag to set
     */
    public void setSipEligFlag(String sipEligFlag) {
        this.sipEligFlag = (ObjectUtils.isNull(sipEligFlag)) ? "" : sipEligFlag;
    }

    /**
     * @return the emplType
     */
    public String getEmplType() {
        return emplType;
    }

    /**
     * @param emplType the emplType to set
     */
    public void setEmplType(String emplType) {
        this.emplType = (ObjectUtils.isNull(emplType)) ? "" : emplType;
    }

    /**
     * @return the emplRcd
     */
    public String getEmplRcd() {
        return emplRcd;
    }

    /**
     * @param emplRcd the emplRcd to set
     */
    public void setEmplRcd(String emplRcd) {
        this.emplRcd = (ObjectUtils.isNull(emplRcd)) ? "" : emplRcd;
    }

    /**
     * @return the jobCode
     */
    public String getJobCode() {
        return jobCode;
    }

    /**
     * @param jobCode the jobCode to set
     */
    public void setJobCode(String jobCode) {
        this.jobCode = (ObjectUtils.isNull(jobCode)) ? "" : jobCode;
    }

    /**
     * @return the jobCdDescShrt
     */
    public String getJobCdDescShrt() {
        return jobCdDescShrt;
    }

    /**
     * @param jobCdDescShrt the jobCdDescShrt to set
     */
    public void setJobCdDescShrt(String jobCdDescShrt) {
        this.jobCdDescShrt = (ObjectUtils.isNull(jobCdDescShrt)) ? "" : jobCdDescShrt;
    }

    /**
     * @return the jobFamily
     */
    public String getJobFamily() {
        return jobFamily;
    }

    /**
     * @param jobFamily the jobFamily to set
     */
    public void setJobFamily(String jobFamily) {
        this.jobFamily = jobFamily;
    }

    /**
     * @return the posFte
     */
    public KualiDecimal getPosFte() {
		return (ObjectUtils.isNull(posFte) ? KualiDecimal.ZERO : posFte);
    }

    /**
     * @param posFte the posFte to set
     */
    public void setPosFte(KualiDecimal posFte) {
        this.posFte = posFte;
    }

    /**
     * @return the posGradeDflt
     */
    public String getPosGradeDflt() {
        return posGradeDflt;
    }

    /**
     * @param posGradeDflt the posGradeDflt to set
     */
    public void setPosGradeDflt(String posGradeDflt) {
        this.posGradeDflt = (ObjectUtils.isNull(posGradeDflt)) ? "" : posGradeDflt;
    }

    /**
     * @return the cuStateCert
     */
    public String getCuStateCert() {
        return cuStateCert;
    }

    /**
     * @param cuStateCert the cuStateCert to set
     */
    public void setCuStateCert(String cuStateCert) {
        this.cuStateCert = (ObjectUtils.isNull(cuStateCert)) ? "" : cuStateCert;
    }

    /**
     * @return the compFreq
     */
    public String getCompFreq() {
        return compFreq;
    }

    /**
     * @param compFreq the compFreq to set
     */
    public void setCompFreq(String compFreq) {
        this.compFreq = (ObjectUtils.isNull(compFreq)) ? "" : compFreq;
    }

    /**
     * @return the annlRt
     */
    public KualiDecimal getAnnlRt() {
		return (ObjectUtils.isNull(annlRt) ? KualiDecimal.ZERO : annlRt);
    }

    /**
     * @param annlRt the annlRt to set
     */
    public void setAnnlRt(KualiDecimal annlRt) {
        this.annlRt = annlRt;
    }

    /**
     * @return the compRt
     */
    public KualiDecimal getCompRt() {
		return (ObjectUtils.isNull(compRt) ? KualiDecimal.ZERO : compRt);
    }

    /**
     * @param compRt the compRt to set
     */
    public void setCompRt(KualiDecimal compRt) {
        this.compRt = compRt;
    }

    /**
     * @return the jobStdHrs
     */
    public KualiDecimal getJobStdHrs() {
		return (ObjectUtils.isNull(jobStdHrs) ? KualiDecimal.ZERO : jobStdHrs);
    }

    /**
     * @param jobStdHrs the jobStdHrs to set
     */
    public void setJobStdHrs(KualiDecimal jobStdHrs) {
        this.jobStdHrs = jobStdHrs;
    }

    /**
     * @return the wrkMnths
     */
    public KualiDecimal getWrkMnths() {
		return (ObjectUtils.isNull(wrkMnths) ? KualiDecimal.ZERO : wrkMnths);
    }

    /**
     * @param wrkMnths the wrkMnths to set
     */
    public void setWrkMnths(KualiDecimal wrkMnths) {
        this.wrkMnths = wrkMnths;
    }

    /**
     * @return the jobFunc
     */
    public String getJobFunc() {
        return jobFunc;
    }

    /**
     * @param jobFunc the jobFunc to set
     */
    public void setJobFunc(String jobFunc) {
        this.jobFunc = (ObjectUtils.isNull(jobFunc)) ? "" : jobFunc;
    }

    /**
     * @return the jobFuncDesc
     */
    public String getJobFuncDesc() {
        return jobFuncDesc;
    }

    /**
     * @param jobFuncDesc the jobFuncDesc to set
     */
    public void setJobFuncDesc(String jobFuncDesc) {
        this.jobFuncDesc = (ObjectUtils.isNull(jobFuncDesc)) ? "" : jobFuncDesc;
    }

    /**
     * @return the incToMin
     */
    public KualiDecimal getIncToMin() {
		return (ObjectUtils.isNull(incToMin) ? KualiDecimal.ZERO : incToMin);
    }

    /**
     * @param incToMin the incToMin to set
     */
    public void setIncToMin(KualiDecimal incToMin) {
        this.incToMin = incToMin;
    }

    /**
     * @return the equity
     */
    public KualiDecimal getEquity() {
		return (ObjectUtils.isNull(equity) ? KualiDecimal.ZERO : equity);
    }

    /**
     * @param equity the equity to set
     */
    public void setEquity(KualiDecimal equity) {
        this.equity = equity;
    }

    /**
     * @return the merit
     */
    public KualiDecimal getMerit() {
		return (ObjectUtils.isNull(merit) ? KualiDecimal.ZERO : merit);
    }

    /**
     * @param merit the merit to set
     */
    public void setMerit(KualiDecimal merit) {
        this.merit = merit;
    }

    /**
     * @return the note
     */
    public String getNote() {
        return note;
    }

    /**
     * @param note the note to set
     */
    public void setNote(String note) {
        this.note = (ObjectUtils.isNull(note)) ? "" : note;
    }

    /**
     * @return the deferred
     */
    public KualiDecimal getDeferred() {
		return (ObjectUtils.isNull(deferred) ? KualiDecimal.ZERO : deferred);
    }

    /**
     * @param deferred the deferred to set
     */
    public void setDeferred(KualiDecimal deferred) {
        this.deferred = deferred;
    }

    /**
     * @return the cuAbbrFlag
     */
    public String getCuAbbrFlag() {
        return cuAbbrFlag;
    }

    /**
     * @param cuAbbrFlag the cuAbbrFlag to set
     */
    public void setCuAbbrFlag(String cuAbbrFlag) {
        this.cuAbbrFlag = (ObjectUtils.isNull(cuAbbrFlag)) ? "" : cuAbbrFlag;
    }

    /**
     * @return the apptTotIntndAmt
     */
    public KualiDecimal getApptTotIntndAmt() {
        return (ObjectUtils.isNull(apptTotIntndAmt) ? KualiDecimal.ZERO : apptTotIntndAmt);
    }

    /**
     * @param apptTotIntndAmt the apptTotIntndAmt to set
     */
    public void setApptTotIntndAmt(KualiDecimal apptTotIntndAmt) {
        this.apptTotIntndAmt = apptTotIntndAmt;
    }

    /**
     * @return the apptRqstFteQty
     */
    public KualiDecimal getApptRqstFteQty() {
        return (ObjectUtils.isNull(apptRqstFteQty) ? KualiDecimal.ZERO : apptRqstFteQty);
    }

    /**
     * @param apptRqstFteQty the apptRqstFteQty to set
     */
    public void setApptRqstFteQty(KualiDecimal apptRqstFteQty) {
        this.apptRqstFteQty = apptRqstFteQty;
    }

    /**
     * @return the positionType
     */
    public String getPositionType() {
        return positionType;
    }

    /**
     * @param positionType the positionType to set
     */
    public void setPositionType(String positionType) {
        this.positionType = (ObjectUtils.isNull(positionType)) ? "" : positionType;
    }

    /**
     * Gets the newCompRate.
     * 
     * @return newCompRate
     */
    public KualiDecimal getNewCompRate() {
        return newCompRate;
    }

    /**
     * Sets the newCompRate.
     * 
     * @param newCompRate
     */
    public void setNewCompRate(KualiDecimal newCompRate) {
        this.newCompRate = newCompRate;
    }

    /**
     * Gets the newAnnualRate.
     * 
     * @return newAnnualRate
     */
    public KualiDecimal getNewAnnualRate() {
        return newAnnualRate;
    }

    /**
     * Sets the newAnnualRate.
     * 
     * @param newAnnualRate
     */
    public void setNewAnnualRate(KualiDecimal newAnnualRate) {
        this.newAnnualRate = newAnnualRate;
    }

	/**
	 * @return the passedValidation
	 */
	public String getPassedValidation() {
		return passedValidation;
	}

	/**
	 * @param passedValidation the passedValidation to set
	 */
	public void setPassedValidation(String passedValidation) {
		this.passedValidation = passedValidation;
	}

	/**
	 * @return the validationErrors
	 */
	public String getValidationErrors() {
		return validationErrors;
	}

	/**
	 * @param validationErrors the validationErrors to set
	 */
	public void setValidationErrors(String validationErrors) {
		this.validationErrors = validationErrors;
	}

    public String getSipLoadErrors() {
        return sipLoadErrors;
    }

    public void setSipLoadErrors(String sipLoadErrors) {
        this.sipLoadErrors = sipLoadErrors;
    }

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SipImportData [unitId=" + unitId + ", hrDeptId=" + hrDeptId
				+ ", kfsDeptId=" + kfsDeptId + ", deptName=" + deptName
				+ ", positionNbr=" + positionNbr + ", posDescr=" + posDescr
				+ ", emplId=" + emplId + ", personNm=" + personNm
				+ ", sipEligFlag=" + sipEligFlag + ", emplType=" + emplType
				+ ", emplRcd=" + emplRcd + ", jobCode=" + jobCode
				+ ", jobCdDescShrt=" + jobCdDescShrt + ", jobFamily="
				+ jobFamily + ", posFte=" + posFte + ", posGradeDflt="
				+ posGradeDflt + ", cuStateCert=" + cuStateCert + ", compFreq="
				+ compFreq + ", annlRt=" + annlRt + ", compRt=" + compRt
				+ ", jobStdHrs=" + jobStdHrs + ", wrkMnths=" + wrkMnths
				+ ", jobFunc=" + jobFunc + ", jobFuncDesc=" + jobFuncDesc
				+ ", incToMin=" + incToMin + ", equity=" + equity + ", merit="
				+ merit + ", note=" + note + ", deferred=" + deferred
				+ ", cuAbbrFlag=" + cuAbbrFlag + ", apptTotIntndAmt="
				+ apptTotIntndAmt + ", apptRqstFteQty=" + apptRqstFteQty
				+ ", positionType=" + positionType + ", newCompRate="
				+ newCompRate + ", newAnnualRate=" + newAnnualRate
				+ ", passedValidation=" + passedValidation
				+ ", validationErrors=" + validationErrors + ", sipLoadErrors="
				+ sipLoadErrors + "]";
	}
}
