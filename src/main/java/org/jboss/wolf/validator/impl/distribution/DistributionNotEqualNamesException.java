package org.jboss.wolf.validator.impl.distribution;

import java.io.File;

public class DistributionNotEqualNamesException extends Exception {

    private static final long serialVersionUID = 1L;

    private final File fileFromRepo;
    private final File fileFromDist;

    public DistributionNotEqualNamesException(File fileFromRepo, File fileFromDist) {
        super("File from REPOSITORY " + fileFromRepo + " does not have the same name as file from DISTRIBUTION " + fileFromDist);
        this.fileFromRepo = fileFromRepo;
        this.fileFromDist = fileFromDist;
    }

    public File getFileFromRepo() {
        return fileFromRepo;
    }

    public File getFileFromDist() {
        return fileFromDist;
    }
}