package org.jboss.wolf.validator.impl.bom;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.jboss.wolf.validator.impl.DependencyNotFoundException;

public class BomDependencyNotFoundException extends DependencyNotFoundException {
    private static final long serialVersionUID = 1L;

    public BomDependencyNotFoundException(Exception cause, Artifact missingArtifact, Artifact validatedArtifact, DependencyNode dependencyNode) {
        super(cause, missingArtifact, validatedArtifact, dependencyNode);
    }

}