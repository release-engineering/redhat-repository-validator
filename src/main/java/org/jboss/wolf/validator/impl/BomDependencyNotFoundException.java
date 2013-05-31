package org.jboss.wolf.validator.impl;

import java.io.File;

public class BomDependencyNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    private final File bomFile;

    public BomDependencyNotFoundException(File bomFile, Exception cause) {
        super("BOM " + bomFile.getName() + " dependency not found: " + cause.getMessage(), cause);
        this.bomFile = bomFile;
    }

    public File getBomFile() {
        return bomFile;
    }

}