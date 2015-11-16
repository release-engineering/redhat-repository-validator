package com.redhat.repository.validator.impl.suspicious;

import java.io.File;

public class SuspiciousFileException extends Exception {

    private static final long serialVersionUID = 1L;

    private final File file;

    public SuspiciousFileException(File file, String msg) {
        super("File " + file + " is " + msg);
        this.file = file;
    }

    public File getFile() {
        return file;
    }

}