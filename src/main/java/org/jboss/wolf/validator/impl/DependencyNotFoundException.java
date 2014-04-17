package org.jboss.wolf.validator.impl;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;

public class DependencyNotFoundException extends Exception {
    private static final long serialVersionUID = 1L;

    private final Artifact missingArtifact;
    private final Artifact validatedArtifact;
    private final DependencyNode dependencyNode;

    public DependencyNotFoundException(Exception cause, Artifact missingArtifact, Artifact validatedArtifact, DependencyNode dependencyNode) {
        super(cause);
        this.missingArtifact = missingArtifact;
        this.validatedArtifact = validatedArtifact;
        this.dependencyNode = dependencyNode;
    }

    public DependencyNotFoundException(Exception e, Artifact missingArtifact, Artifact validatedArtifact) {
        this(e, missingArtifact, validatedArtifact, null);
    }

    public Artifact getMissingArtifact() {
        return missingArtifact;
    }

    public Artifact getValidatedArtifact() {
        return validatedArtifact;
    }

    public DependencyNode getDependencyNode() {
        return dependencyNode;
    }

}
