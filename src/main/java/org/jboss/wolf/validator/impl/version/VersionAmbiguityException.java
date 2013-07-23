package org.jboss.wolf.validator.impl.version;

import org.apache.commons.lang3.StringUtils;

public class VersionAmbiguityException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String ga;
    private final String[] versions;

    public VersionAmbiguityException(String ga, String[] versions) {
        super("Artifact " + ga + " has multiple versions " + StringUtils.join(versions, ", "));
        this.ga = ga;
        this.versions = versions;
    }

    public String getGa() {
        return ga;
    }

    public String[] getVersions() {
        return versions;
    }

}