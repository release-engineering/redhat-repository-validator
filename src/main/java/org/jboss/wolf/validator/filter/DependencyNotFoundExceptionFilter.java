package org.jboss.wolf.validator.filter;

import org.eclipse.aether.artifact.Artifact;
import org.jboss.wolf.validator.ExceptionFilter;
import org.jboss.wolf.validator.impl.DependencyNotFoundException;

import java.io.File;
import java.util.regex.Pattern;

/**
 * Filters the dependency not found exceptions for specified GAV (+ extension) and optionally file (pom in repo)
 * from which they were originated.
 */
public class DependencyNotFoundExceptionFilter implements ExceptionFilter {

    private final Pattern gavWithExtensionPattern;

    /**
     * POM file from which the exception comes from, e.g. the one declaring the missing dependency.
     * <p/>
     * Can be 'null' in which case the filter will ignore all the missing artifacts exceptions, not looking at the
     * POM file from which they were originated.
     */
    private final File pomFileInRepo;

    public DependencyNotFoundExceptionFilter(String gavWithExtensionRegex, File pomFileInRepo) {
        this.gavWithExtensionPattern = Pattern.compile(gavWithExtensionRegex);
        this.pomFileInRepo = pomFileInRepo;
    }

    public DependencyNotFoundExceptionFilter(String gavWithExtensionRegex) {
        this(gavWithExtensionRegex, null);
    }

    protected Class<? extends Exception> getExceptionType() {
        return DependencyNotFoundException.class;
    }

    protected Artifact retrieveMissingArtifact(Exception ex) {
        if (ex instanceof DependencyNotFoundException) {
            return ((DependencyNotFoundException) ex).getMissingArtifact();
        } else {
            throw new IllegalArgumentException("Can't get missing artifact info from exception with type " + ex.getClass().getName());
        }
    }

    @Override
    public boolean shouldIgnore(Exception ex, File file) {
        if (getExceptionType().isInstance(ex)) {
            Artifact missingArtifact = retrieveMissingArtifact(ex);
            String gavWithExtensionStr = createGavWithExtensionString(missingArtifact);
            // exception type already checked, now compare the missing artifact gav + extensions and file in trepo
            if (pomFileInRepo != null) {
                return gavWithExtensionPattern.matcher(gavWithExtensionStr).matches() &&
                        pomFileInRepo.equals(file);
            } else {

                return gavWithExtensionPattern.matcher(gavWithExtensionStr).matches();
            }

        } else {
            return false;
        }
    }

    private String createGavWithExtensionString(Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion() + ":" + artifact.getExtension();
    }

}
