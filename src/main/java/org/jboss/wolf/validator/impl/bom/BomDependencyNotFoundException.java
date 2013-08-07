package org.jboss.wolf.validator.impl.bom;

import org.eclipse.aether.resolution.DependencyResolutionException;

public class BomDependencyNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    private final DependencyResolutionException dependencyResolutionException;

    public BomDependencyNotFoundException(DependencyResolutionException dependencyResolutionException) {
        super(dependencyResolutionException);
        this.dependencyResolutionException = dependencyResolutionException;
    }

    public DependencyResolutionException getDependencyResolutionException() {
        return dependencyResolutionException;
    }

}