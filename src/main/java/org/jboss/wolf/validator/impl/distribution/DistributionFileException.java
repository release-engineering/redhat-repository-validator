package org.jboss.wolf.validator.impl.distribution;

import java.io.File;

public class DistributionFileException extends Exception {

    private static final long serialVersionUID = 1L;

    private final File file;

    public DistributionFileException(File file, String msg) {
        super("File " + file + " is " + msg);
        this.file = file;
    }

    public File getFile() {
        return file;
    }

}