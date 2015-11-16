package com.redhat.repository.validator.impl.osgi;

public class OsgiVersionException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String gav;

    public OsgiVersionException(String gav, Exception cause) {
        super("Artifact " + gav + " hasn't OSGI compatible version: " + cause.getMessage(), cause);
        this.gav = gav;
    }

    public String getGav() {
        return gav;
    }

}