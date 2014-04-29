package org.jboss.wolf.validator.filter;

import org.jboss.wolf.validator.impl.DependencyNotFoundException;
import org.jboss.wolf.validator.impl.bom.BomDependencyNotFoundException;

public class BomDependencyNotFoundExceptionFilter extends DependencyNotFoundExceptionFilter {

    public BomDependencyNotFoundExceptionFilter(String missingArtifactRegex, String validatedArtifactRegex) {
        super(missingArtifactRegex, validatedArtifactRegex);
    }

    public BomDependencyNotFoundExceptionFilter(String missingArtifactRegex) {
        super(missingArtifactRegex, null);
    }

    @Override
    protected Class<? extends DependencyNotFoundException> getExceptionType() {
        return BomDependencyNotFoundException.class;
    }

    @Override
    public String toString() {
        return "BomDependencyNotFoundExceptionFilter{" +
                "missingArtifactRegex=" + getMissingArtifactRegex() +
                ", validatedArtifactRegex=" + getValidatedArtifactRegex() +
                '}';
    }

}
