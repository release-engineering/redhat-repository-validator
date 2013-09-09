package org.jboss.wolf.validator.impl.source;

import java.io.File;

public class JarSourcesVerificationException extends Exception {

    private static final long serialVersionUID = 1L;

    private final File file;

    public JarSourcesVerificationException(File file) {
        super("Unable to find sources for file " + file);
        this.file = file;
    }

    public JarSourcesVerificationException(File file, Throwable cause) {
        super("Unable to find sources for file " + file, cause);
        this.file = file;
    }

    public File getFile() {
        return file;
    }
}