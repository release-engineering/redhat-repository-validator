package org.jboss.wolf.validator.impl.signature;

import java.io.File;

public class JarSignatureVerificationException extends Exception {

    private static final long serialVersionUID = 1L;

    private final File file;
    private final String output;

    public JarSignatureVerificationException(File file, String output) {
        super("Unable to verify signature for file " + file + ", see process output: " + output);
        this.file = file;
        this.output = output;
    }

    public JarSignatureVerificationException(File file, Throwable cause) {
        super("Unable to verify signature for file " + file, cause);
        this.file = file;
        this.output = null;
    }

    public File getFile() {
        return file;
    }

    public String getOutput() {
        return output;
    }

}