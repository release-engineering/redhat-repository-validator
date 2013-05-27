package org.jboss.wolf.validator.impl;

import org.apache.maven.model.Model;

public class UnmanagedVersionException extends Exception {

    private static final long serialVersionUID = 1L;

    private final Model project;

    public UnmanagedVersionException(Model project) {
        super("Project " + project + " is unmanaged, its version is not managed in BOMs.");
        this.project = project;
    }

    public Model getProject() {
        return project;
    }

}