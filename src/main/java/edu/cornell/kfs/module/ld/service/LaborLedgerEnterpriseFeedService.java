package edu.cornell.kfs.module.ld.service;

import java.io.File;

public interface LaborLedgerEnterpriseFeedService {

    /**
     * Creates a disencumbrance file together with a .recon and a .done file for the input encumbrance file.
     * 
     * @param encumbranceFile
     * @return true if the disencumbrance file was created, false otherwise
     */
    public boolean createDisencumbrance(File encumbranceFile);
}
