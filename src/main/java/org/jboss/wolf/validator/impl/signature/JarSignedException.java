package org.jboss.wolf.validator.impl.signature;

import java.io.File;

public class JarSignedException extends Exception {

    private static final long serialVersionUID = 1L;

    private final File file;

    public JarSignedException(File file) {
        super("File " + file + " is signed");
        this.file = file;
    }

    public File getFile() {
        return file;
    }

}