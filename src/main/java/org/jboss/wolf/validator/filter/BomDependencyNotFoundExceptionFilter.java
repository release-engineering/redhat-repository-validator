package org.jboss.wolf.validator.filter;

import org.eclipse.aether.artifact.Artifact;
import org.jboss.wolf.validator.impl.bom.BomDependencyNotFoundException;

import java.io.File;

public class BomDependencyNotFoundExceptionFilter extends DependencyNotFoundExceptionFilter {

    public BomDependencyNotFoundExceptionFilter(String gavWithExtensionRegex, File pomFileInRepo) {
        super(gavWithExtensionRegex, pomFileInRepo);
    }

    public BomDependencyNotFoundExceptionFilter(String gavWithExtensionRegex) {
        super(gavWithExtensionRegex, null);
    }

    @Override
    protected Class<? extends Exception> getExceptionType() {
        return BomDependencyNotFoundException.class;
    }

    @Override
    protected Artifact retrieveMissingArtifact(Exception ex) {
        if (ex instanceof BomDependencyNotFoundException) {
            return ((BomDependencyNotFoundException) ex).getMissingArtifact();
        } else {
            throw new IllegalArgumentException("Can't get missing artifact info from exception with type " + ex.getClass().getName());
        }
    }
    
}
