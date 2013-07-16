package org.jboss.wolf.validator.impl.bom;

import static org.apache.commons.lang3.SystemUtils.LINE_SEPARATOR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

public class BomAmbiguousVersionException extends Exception {

    private static final long serialVersionUID = 1L;

    private static String formatMessage(String dependencyKey, List<Pair<Dependency, Model>> ambiguousDependencies) {
        String msg = "BOMs contains ambiguous version for dependency " + dependencyKey;
        for (Pair<Dependency, Model> pair : ambiguousDependencies) {
            msg += LINE_SEPARATOR;
            msg += "    bom " + pair.getValue().getId() + " defines version " + pair.getKey().getVersion();
        }
        return msg;
    }

    private final String dependencyKey;
    private final List<Pair<Dependency, Model>> ambiguousDependencies;

    public BomAmbiguousVersionException(String dependencyKey, List<Pair<Dependency, Model>> ambiguousDependencies) {
        super(formatMessage(dependencyKey, ambiguousDependencies));
        this.dependencyKey = dependencyKey;
        this.ambiguousDependencies = Collections.unmodifiableList(new ArrayList<Pair<Dependency, Model>>(ambiguousDependencies));
    }

    public String getDependencyKey() {
        return dependencyKey;
    }

    public List<Pair<Dependency, Model>> getAmbiguousDependencies() {
        return ambiguousDependencies;
    }

}