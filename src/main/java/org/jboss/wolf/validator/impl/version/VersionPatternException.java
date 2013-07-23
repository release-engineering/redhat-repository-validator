package org.jboss.wolf.validator.impl.version;

import java.io.File;

public class VersionPatternException extends Exception {

    private static final long serialVersionUID = 1L;

    private final File file;
    private final String version;
    private final String pattern;

    public VersionPatternException(File file, String version, String pattern) {
        super("Artifact " + file + " has version " + version + ", which doesn't match pattern " + pattern);
        this.file = file;
        this.version = version;
        this.pattern = pattern;
    }

    public File getFile() {
        return file;
    }

    public String getVersion() {
        return version;
    }

    public String getPattern() {
        return pattern;
    }

}