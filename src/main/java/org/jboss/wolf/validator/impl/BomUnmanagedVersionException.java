package org.jboss.wolf.validator.impl;

import org.apache.maven.model.Model;

public class BomUnmanagedVersionException extends Exception {

    private static final long serialVersionUID = 1L;

    private final Model model;

    public BomUnmanagedVersionException(Model model) {
        super("Artifact " + model + " is unmanaged, its version is not managed in BOMs.");
        this.model = model;
    }

    public Model getModel() {
        return model;
    }

}