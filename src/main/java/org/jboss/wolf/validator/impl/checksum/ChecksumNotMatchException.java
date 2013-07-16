package org.jboss.wolf.validator.impl.checksum;

import java.io.File;

public class ChecksumNotMatchException extends Exception {

    private static final long serialVersionUID = 1L;

    private final File file;
    private final String algorithm;
    private final String expected;
    private final String actual;

    public ChecksumNotMatchException(File file, String algorithm, String expected, String actual) {
        super("Checksum " + algorithm + " for file " + file + " not match");
        this.file = file;
        this.algorithm = algorithm;
        this.expected = expected;
        this.actual = actual;
    }

    public File getFile() {
        return file;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getExpected() {
        return expected;
    }

    public String getActual() {
        return actual;
    }

}