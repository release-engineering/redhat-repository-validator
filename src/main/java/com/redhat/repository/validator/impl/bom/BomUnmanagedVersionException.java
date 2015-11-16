package com.redhat.repository.validator.impl.bom;

public class BomUnmanagedVersionException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String gav;

    public BomUnmanagedVersionException(String gav) {
        super("Artifact " + gav + " is unmanaged, its version is not managed in BOMs");
        this.gav = gav;
    }

    public String getGav() {
        return gav;
    }

}