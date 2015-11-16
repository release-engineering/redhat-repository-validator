package com.redhat.repository.validator.filter;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.aether.artifact.Artifact;

import com.redhat.repository.validator.ExceptionFilter;
import com.redhat.repository.validator.impl.DependencyNotFoundException;

import java.io.File;
import java.util.regex.Pattern;

/**
 * Filters the {@link com.redhat.repository.validator.impl.DependencyNotFoundException}s using the specified
 * missing artifact regex and validated artifact regex.
 *
 * The validated artifact regex may not be specified and in that case the filter will ignore the it
 * altogether.
 *
 * The artifact regexs are in format `groupId:artifactId:extension[:classifier]:version`, classifier is optional.
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

    public String getMissingArtifactRegex() {
        return missingArtifactPattern.pattern();
    }

    public String getValidatedArtifactRegex() {
        return validatedArtifactPattern == null ? null : validatedArtifactPattern.pattern();
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
            String missingArtifactStr = createArtifactString(missingArtifact, hasClassifier(getMissingArtifactRegex()));
            // exception type already checked, now compare the missing artifact (+ the validated artifact is specified)
            if (validatedArtifactPattern != null) {
                Artifact validatedArtifact = ((DependencyNotFoundException) ex).getValidatedArtifact();
                String validatedArtifactStr = createArtifactString(validatedArtifact, hasClassifier(getValidatedArtifactRegex()));
                return missingArtifactPattern.matcher(missingArtifactStr).matches() &&
                        validatedArtifactPattern.matcher(validatedArtifactStr).matches();
            } else {
                return missingArtifactPattern.matcher(missingArtifactStr).matches();
            }
        } else {
            return false;
        }
    }

    /**
     * Determines is the specified regex also contains classifier. The method expects well formed artifact regex
     * in format `groupId:artifactId:extension:[classifier]:version`. The classifier is optional.
     */
    private boolean hasClassifier(String artifactRegex) {
        // classifier is optional, so count the number of ":" to determine if it was specified or not
        // groupId:artifactId:extension:classifier:version has 4 ":"
        // groupId:artifactId:extension:version has 3 ":"
        if (artifactRegex == null) {
            return false;
        } else {
            return StringUtils.countMatches(artifactRegex, ":") == 4;
        }
    }

    /**
     * Converts the artifact into string with format `groupId:artifactId:extension[:classifier]:version`. The
     * classifier is included only if specified by the flag. If the classifier is empty string and the flag requests the
     * classifier to be present, the empty string is returned in place of the classifier. This is different from what
     * would the {@link org.eclipse.aether.artifact.DefaultArtifact#toString()} return.
     *
     * Using own method instead of {@link org.eclipse.aether.artifact.DefaultArtifact#toString()}, because even if the
     * classifier is empty string it needs to be returned in case the flag says so. The default toString() will ignore
     * the classifier and there is no easy way how to include it. The resulting string would have to manually parsed
     * and updated. Own method seems like a cleaner solution.
     */
    private String createArtifactString(Artifact artifact, boolean includeClassifier) {
        StringBuilder buffer = new StringBuilder(128);
        buffer.append(artifact.getGroupId());
        buffer.append(':').append(artifact.getArtifactId());
        buffer.append(':').append(artifact.getExtension());
        if (includeClassifier) {
            buffer.append(':').append(artifact.getClassifier());
        }
        buffer.append(':').append(artifact.getVersion());
        return buffer.toString();
    }

    @Override
    public String toString() {
        return "DependencyNotFoundExceptionFilter{" +
                "missingArtifactRegex=" + getMissingArtifactRegex() +
                ", validatedArtifactRegex=" + getValidatedArtifactRegex() +
                '}';
    }

}
