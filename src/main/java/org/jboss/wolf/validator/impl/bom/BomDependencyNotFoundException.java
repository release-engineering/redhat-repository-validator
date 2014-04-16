package org.jboss.wolf.validator.impl.bom;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.DependencyResolutionException;

public class BomDependencyNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    private final DependencyResolutionException dependencyResolutionException;

    private final Artifact missingArtifact;

    private final DependencyNode dependencyNode;

    public BomDependencyNotFoundException(DependencyResolutionException dependencyResolutionException, Artifact missingArtifact) {
        super(dependencyResolutionException);
        this.dependencyResolutionException = dependencyResolutionException;
        this.missingArtifact = missingArtifact;
        this.dependencyNode = dependencyResolutionException.getResult().getRoot();
    }

    public DependencyResolutionException getDependencyResolutionException() {
        return dependencyResolutionException;
    }

    public Artifact getMissingArtifact() {
        return missingArtifact;
    }

    public DependencyNode getDependencyNode() {
        return dependencyNode;
    }

}