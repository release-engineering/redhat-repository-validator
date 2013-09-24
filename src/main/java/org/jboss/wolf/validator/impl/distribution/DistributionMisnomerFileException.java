package org.jboss.wolf.validator.impl.distribution;

import java.io.File;

public class DistributionMisnomerFileException extends Exception {

    private static final long serialVersionUID = 1L;

    private final File fileFromRepo;
    private final File fileFromDist;

    public DistributionMisnomerFileException(File fileFromRepo, File fileFromDist) {
        super("File in distribution " + fileFromDist + " has same content like file in repository " + fileFromRepo + ", but has different name");
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