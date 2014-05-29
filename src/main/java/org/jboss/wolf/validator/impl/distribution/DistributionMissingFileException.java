package org.jboss.wolf.validator.impl.distribution;

import java.io.File;

public class DistributionMissingFileException extends Exception {

    private static final long serialVersionUID = 1L;

    private final File missingFile;

    public DistributionMissingFileException(File missingFile) {
        super("Distribution doesn't contain file from repository: " + missingFile);
        this.missingFile = missingFile;
    }

    public File getMissingFile() {
        return missingFile;
    }

}