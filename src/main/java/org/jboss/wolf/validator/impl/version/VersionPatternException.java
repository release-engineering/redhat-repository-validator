package org.jboss.wolf.validator.impl.version;

public class VersionPatternException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String gav;
    private final String pattern;

    public VersionPatternException(String gav, String pattern) {
        super("Artifact " + gav + " has version, which doesn't match pattern " + pattern);
        this.gav = gav;
        this.pattern = pattern;
    }

    public String getGav() {
        return gav;
    }

    public String getPattern() {
        return pattern;
    }

}