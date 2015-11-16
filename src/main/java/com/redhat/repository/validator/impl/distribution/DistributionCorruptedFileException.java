package com.redhat.repository.validator.impl.distribution;

import java.io.File;

public class DistributionCorruptedFileException extends Exception {

    private static final long serialVersionUID = 1L;

    private final File fileFromRepo;
    private final File fileFromDist;

    public DistributionCorruptedFileException(File fileFromRepo, File fileFromDist) {
        super("File in distribution " + fileFromDist + " has same name like file in repository " + fileFromRepo + ", but has different content");
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