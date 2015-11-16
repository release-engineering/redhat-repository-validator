package com.redhat.repository.validator.filter;

import com.redhat.repository.validator.impl.DependencyNotFoundException;
import com.redhat.repository.validator.impl.bom.BomDependencyNotFoundException;

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
