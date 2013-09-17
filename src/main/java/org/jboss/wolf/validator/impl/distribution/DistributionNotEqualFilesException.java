package org.jboss.wolf.validator.impl.distribution;

import java.io.File;

public class DistributionNotEqualFilesException extends Exception {

    private static final long serialVersionUID = 1L;

    private final File fileFromRepo;
    private final File fileFromDist;

    public DistributionNotEqualFilesException(File fileFromRepo, File fileFromDist) {
        super("File from REPOSITORY " + fileFromRepo + " is not same as file from DISTRIBUTION " + fileFromDist);
        this.fileFromRepo = fileFromRepo;
        this.fileFromDist = fileFromDist;
    }

    public DistributionNotEqualFilesException(File fileFromRepo, File fileFromDist, Throwable cause) {
        super("File from REPOSITORY " + fileFromRepo + " is not same as file from DISTRIBUTION " + fileFromDist, cause);
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