package com.redhat.repository.validator.impl.xml;

import java.io.File;

public class XmlVerificationException extends Exception {

    private static final long serialVersionUID = 1L;

    private final File file;

    public XmlVerificationException(File file, String message) {
        super("Xml file " + file + " has following errors " + message);
        this.file = file;
    }

    public XmlVerificationException(File file, Throwable cause) {
        super("Xml file " + file + " has following errors ", cause);
        this.file = file;
    }

    public File getFile() {
        return file;
    }
}