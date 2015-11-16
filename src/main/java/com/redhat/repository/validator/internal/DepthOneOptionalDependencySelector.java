package com.redhat.repository.validator.internal;

import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;

public class DepthOneOptionalDependencySelector implements DependencySelector {

    private final int depth;

    public DepthOneOptionalDependencySelector() {
        depth = 0;
    }

    private DepthOneOptionalDependencySelector(int depth) {
        this.depth = depth;
    }

    public boolean selectDependency(Dependency dependency) {
        return depth < 1 || !dependency.isOptional();
    }

    public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
        if (depth >= 1) {
            return this;
        }
        return new DepthOneOptionalDependencySelector(depth + 1);
    }

    @Override
    public boolean equals(Object obj) {
        DepthOneOptionalDependencySelector that = (DepthOneOptionalDependencySelector) obj;
        return depth == that.depth;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode() * 31 + depth;
    }

}