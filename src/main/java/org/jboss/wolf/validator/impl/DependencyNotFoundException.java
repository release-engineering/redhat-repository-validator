package org.jboss.wolf.validator.impl;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;

public class DependencyNotFoundException extends Exception {
    private final Artifact missingArtifact;
    private final DependencyNode dependencyNode;

    public DependencyNotFoundException(Exception cause, Artifact missingArtifact, DependencyNode dependencyNode) {
        super(cause);
        this.missingArtifact = missingArtifact;
        this.dependencyNode = dependencyNode;
    }

    public DependencyNotFoundException(Exception e, Artifact missingArtifact) {
        this(e, missingArtifact, null);
    }

    public Artifact getMissingArtifact() {
        return missingArtifact;
    }

    public DependencyNode getDependencyNode() {
        return dependencyNode;
    }

}
