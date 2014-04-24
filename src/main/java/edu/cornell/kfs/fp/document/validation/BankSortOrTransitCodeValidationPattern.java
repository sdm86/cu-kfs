package edu.cornell.kfs.fp.document.validation;

import org.kuali.rice.kns.datadictionary.validation.FieldLevelValidationPattern;

public class BankSortOrTransitCodeValidationPattern extends FieldLevelValidationPattern {

    @Override
    protected String getPatternTypeName() {
        return "bankSortOrTransitCode";
    }

}
