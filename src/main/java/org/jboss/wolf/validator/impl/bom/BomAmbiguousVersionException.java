package org.jboss.wolf.validator.impl.bom;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.model.Dependency;

public class BomAmbiguousVersionException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String dependencyKey;
    private final List<Pair<Dependency, File>> ambiguousDependencies;

    public BomAmbiguousVersionException(String dependencyKey, List<Pair<Dependency, File>> ambiguousDependencies) {
        super("BOMs contain ambiguous version for dependency " + dependencyKey);
        this.dependencyKey = dependencyKey;
        this.ambiguousDependencies = Collections.unmodifiableList(new ArrayList<Pair<Dependency, File>>(ambiguousDependencies));
    }

    public String getDependencyKey() {
        return dependencyKey;
    }

    public List<Pair<Dependency, File>> getAmbiguousDependencies() {
        return ambiguousDependencies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BomAmbiguousVersionException that = (BomAmbiguousVersionException) o;

        if (ambiguousDependencies != null ? !ambiguousDependencies.equals(that.ambiguousDependencies) : that.ambiguousDependencies != null)
            return false;
        if (dependencyKey != null ? !dependencyKey.equals(that.dependencyKey) : that.dependencyKey != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = dependencyKey != null ? dependencyKey.hashCode() : 0;
        result = 31 * result + (ambiguousDependencies != null ? ambiguousDependencies.hashCode() : 0);
        return result;
    }

}