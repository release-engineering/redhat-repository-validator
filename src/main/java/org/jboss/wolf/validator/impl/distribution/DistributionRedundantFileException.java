package org.jboss.wolf.validator.impl.distribution;

import java.io.File;

public class DistributionRedundantFileException extends Exception {

    private static final long serialVersionUID = 1L;
    
    private final File redundantFile;
    
    public DistributionRedundantFileException(File redundantFile) {
        super("Distribution contains file, which is not in repository: " + redundantFile);
        this.redundantFile = redundantFile;
    }
    
    public File getRedundantFile() {
        return redundantFile;
    }

}