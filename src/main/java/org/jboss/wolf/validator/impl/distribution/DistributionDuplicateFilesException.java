package org.jboss.wolf.validator.impl.distribution;

import static org.apache.commons.lang3.StringUtils.join;

import java.io.File;

public class DistributionDuplicateFilesException extends Exception {

    private static final long serialVersionUID = 1L;

    private final File[] duplicateFiles;

    public DistributionDuplicateFilesException(File[] duplicateFiles) {
        super("Duplicate files in distribution: " + join(duplicateFiles, ", "));
        this.duplicateFiles = duplicateFiles;
    }

    public File[] getDuplicateFile() {
        return duplicateFiles;
    }

}