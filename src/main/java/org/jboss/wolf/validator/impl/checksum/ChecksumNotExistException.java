package org.jboss.wolf.validator.impl.checksum;

import java.io.File;

public class ChecksumNotExistException extends Exception {

    private static final long serialVersionUID = 1L;

    private final File file;
    private final String algorithm;

    public ChecksumNotExistException(File file, String algorithm) {
        super("Checksum " + algorithm + " for file " + file + " not exist");
        this.file = file;
        this.algorithm = algorithm;
    }

    public File getFile() {
        return file;
    }

    public String getAlgorithm() {
        return algorithm;
    }

}