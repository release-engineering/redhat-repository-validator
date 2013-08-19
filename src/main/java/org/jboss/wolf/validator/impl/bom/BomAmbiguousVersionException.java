package org.jboss.wolf.validator.impl.bom;

import static org.apache.commons.lang3.SystemUtils.LINE_SEPARATOR;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.model.Dependency;

public class BomAmbiguousVersionException extends Exception {

    private static final long serialVersionUID = 1L;

    private static String formatMessage(String dependencyKey, List<Pair<Dependency, File>> ambiguousDependencies) {
        String msg = "BOMs contains ambiguous version for dependency " + dependencyKey;
        for (Pair<Dependency, File> pair : ambiguousDependencies) {
            msg += LINE_SEPARATOR;
            msg += "    bom " + pair.getValue() + " defines version " + pair.getKey().getVersion();
        }
        return msg;
    }

    private final String dependencyKey;
    private final List<Pair<Dependency, File>> ambiguousDependencies;

    public BomAmbiguousVersionException(String dependencyKey, List<Pair<Dependency, File>> ambiguousDependencies) {
        super(formatMessage(dependencyKey, ambiguousDependencies));
        this.dependencyKey = dependencyKey;
        this.ambiguousDependencies = Collections.unmodifiableList(new ArrayList<Pair<Dependency, File>>(ambiguousDependencies));
    }

    public String getDependencyKey() {
        return dependencyKey;
    }

    public List<Pair<Dependency, File>> getAmbiguousDependencies() {
        return ambiguousDependencies;
    }

}