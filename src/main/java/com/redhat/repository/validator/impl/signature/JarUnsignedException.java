package com.redhat.repository.validator.impl.signature;

import java.io.File;

public class JarUnsignedException extends Exception {

    private static final long serialVersionUID = 1L;

    private final File file;

    public JarUnsignedException(File file) {
        super("File " + file + " is unsigned");
        this.file = file;
    }

    public File getFile() {
        return file;
    }

}