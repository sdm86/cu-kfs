package edu.cornell.kfs.fp.businessobject.options;


public class PaymentMethodValuesFinderNonInterdepartmental extends PaymentMethodValuesFinder {
    static {
        filterCriteria.put("interdepartmentalPayment", "N");
    }
}
