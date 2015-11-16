package com.redhat.repository.validator.impl;

import java.io.File;

public class UnknownArtifactTypeException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String packaging;
    private final File pomFile;

    public UnknownArtifactTypeException(String packaging, File pomFile) {
        super("Unknown artifact type, packaging is " + packaging + " in pom " + pomFile);
        this.packaging = packaging;
        this.pomFile = pomFile;
    }

    public String getPackaging() {
        return packaging;
    }

    public File getPomFile() {
        return pomFile;
    }

}