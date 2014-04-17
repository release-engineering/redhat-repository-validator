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

    private final Pattern missingArtifactPattern;
    /**
     * Artifact from which the exception comes from, e.g. the one declaring the missing dependency.
     * <p/>
     * Can be 'null' and it that case the filter will ignore all the missing artifacts exceptions, not looking at the
     * artifact from which they were originated.
     */
    private final Pattern validatedArtifactPattern;


    public DependencyNotFoundExceptionFilter(String missingArtifactRegex, String validatedArtifactRegex) {
        this.missingArtifactPattern = Pattern.compile(missingArtifactRegex);
        if (validatedArtifactRegex == null) {
            validatedArtifactPattern = null;
        } else {
            this.validatedArtifactPattern = Pattern.compile(validatedArtifactRegex);
        }
    }

    public DependencyNotFoundExceptionFilter(String missingArtifactRegex) {
        this(missingArtifactRegex, null);
    }

    protected Class<? extends DependencyNotFoundException> getExceptionType() {
        return DependencyNotFoundException.class;
    }

    @Override
    public boolean shouldIgnore(Exception ex, File file) {
        // need to match the exact class, because the the Bom*Filter should filter out only {@link BomDependencyNotException}
        // and not for example its super class {@link DependencyNotFoundException}
        if (ex.getClass().equals(getExceptionType())) {
            // the 'file' is deliberately ignored here, all the needed info is captured in the exception
            Artifact missingArtifact = ((DependencyNotFoundException) ex).getMissingArtifact();
            String missingArtifactStr = createArtifactString(missingArtifact);
            // exception type already checked, now compare the missing artifact (+ the validated artifact is specified)
            if (validatedArtifactPattern != null) {
                Artifact validatedArtifact = ((DependencyNotFoundException) ex).getValidatedArtifact();
                String validatedArtifactStr = createArtifactString(validatedArtifact);
                return missingArtifactPattern.matcher(missingArtifactStr).matches() &&
                        validatedArtifactPattern.matcher(validatedArtifactStr).matches();
            } else {
                return missingArtifactPattern.matcher(missingArtifactStr).matches();
            }
        } else {
            return false;
        }
    }

    private String createArtifactString(Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion() + ":" + artifact.getExtension();
    }

}
