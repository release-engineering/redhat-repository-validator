package org.jboss.wolf.validator.impl;

import java.io.File;

public class UnknownArtifactTypeException extends Exception {

    private static final long serialVersionUID = 1L;

    private final File pomFile;

    public UnknownArtifactTypeException(File pomFile) {
        super("Unknown artifact type in pom " + pomFile);
        this.pomFile = pomFile;
    }

    public File getPomFile() {
        return pomFile;
    }

}