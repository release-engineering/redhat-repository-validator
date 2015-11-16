package com.redhat.repository.validator.filter;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Test;

import com.redhat.repository.validator.filter.DependencyNotFoundExceptionFilter;
import com.redhat.repository.validator.impl.DependencyNotFoundException;

import java.io.File;

public class TestDependencyNotFoundExceptionFilter extends AbstractExceptionFilterTest {
    private static final String DEFAULT_MISSING_ARTIFACT_REGEX = "com.acme:finance.*:war:.*";
    private static final Artifact DEFAULT_VALIDATED_ARTIFACT = new DefaultArtifact("com.acme", "acme-parent", "pom", "1.0.0");

    @Test
    public void shouldIgnoreExceptionWithMatchingArtifact() {
        DependencyNotFoundExceptionFilter filter = new DependencyNotFoundExceptionFilter(DEFAULT_MISSING_ARTIFACT_REGEX);
        Artifact artifact = new DefaultArtifact("com.acme", "finance-stuff", "war", "1.0-redhat-2");
        DependencyNotFoundException ex = new DependencyNotFoundException(new Exception(), artifact, DEFAULT_VALIDATED_ARTIFACT);
        assertExceptionIgnored(filter, ex, new File("some-file"));
    }

    @Test
    public void shouldNotIgnoreExceptionOnlyWithMatchingArtifact() {
        DependencyNotFoundExceptionFilter filter = new DependencyNotFoundExceptionFilter(DEFAULT_MISSING_ARTIFACT_REGEX);
        // Exception != DependencyNotFoundException, so it should not be ignored
        Exception ex = new Exception("Some message");
        assertExceptionNotIgnored(filter, ex, new File("some-file"));
    }

    @Test
    public void shouldNotIgnoreExceptionOnlyWithMatchingType() {
        DependencyNotFoundExceptionFilter filter = new DependencyNotFoundExceptionFilter(DEFAULT_MISSING_ARTIFACT_REGEX);
        // "wrong-finance-stuff" artifactId does not match the defined regex
        Artifact artifact = new DefaultArtifact("com.acme", "wrong-finance-stuff", "war", "1.0-redhat-2");
        DependencyNotFoundException ex = new DependencyNotFoundException(new Exception(), artifact, DEFAULT_VALIDATED_ARTIFACT);
        assertExceptionNotIgnored(filter, ex, new File("some-file"));
    }

    @Test
    public void shouldIgnoreOnlyExceptionComingFromSpecifiedPom() {
        String validatedArtifactRegex = "com.acme:parent:pom:1.0.1";
        DependencyNotFoundExceptionFilter filter =
                new DependencyNotFoundExceptionFilter(DEFAULT_MISSING_ARTIFACT_REGEX, validatedArtifactRegex);
        Artifact missingArtifact = new DefaultArtifact("com.acme", "finance-stuff", "war", "1.0-redhat-2");
        Artifact validatedArtifact = new DefaultArtifact("com.acme", "parent", "pom", "1.0.1");
        DependencyNotFoundException ex = new DependencyNotFoundException(new Exception(), missingArtifact, validatedArtifact);
        assertExceptionIgnored(filter, ex, new File("some-file"));

        // now use the validated artifact that does not match the pattern used by the filter
        validatedArtifact = new DefaultArtifact("com.acme", "some-artifact", "pom", "2.0");
        ex = new DependencyNotFoundException(new Exception(), missingArtifact, validatedArtifact);
        assertExceptionNotIgnored(filter, ex, new File("some-file"));
    }

    @Test
    public void shouldHandleRegexWithClassifierAndArtifactWithoutClassifier() {
        String artifactRegexWithClassifier = "com.acme:finance.*:jar:.*:.*";
        DependencyNotFoundExceptionFilter filter = new DependencyNotFoundExceptionFilter(artifactRegexWithClassifier);
        // the regex specifies classifier .*, it should match the empty classifier of the artifact
        Artifact artifact = new DefaultArtifact("com.acme", "finance-stuff", "jar", "1.0-redhat-2");
        DependencyNotFoundException ex = new DependencyNotFoundException(new Exception(), artifact, DEFAULT_VALIDATED_ARTIFACT);
        assertExceptionIgnored(filter, ex, new File("some-file"));

        artifactRegexWithClassifier = "com.acme:finance.*:jar:classes:.*";
        filter = new DependencyNotFoundExceptionFilter(artifactRegexWithClassifier);
        // the regex specifies classifier `classes`, it should _not_ match the empty classifier of the artifact
        artifact = new DefaultArtifact("com.acme", "finance-stuff", "jar", "1.0-redhat-2");
        ex = new DependencyNotFoundException(new Exception(), artifact, DEFAULT_VALIDATED_ARTIFACT);
        assertExceptionNotIgnored(filter, ex, new File("some-file"));
    }

    @Test
    public void shouldHandleRegexWithoutClassifierAndArtifactWithClassifier() {
        String artifactRegexWithoutClassifier = "com.acme:finance.*:jar:.*";
        DependencyNotFoundExceptionFilter filter = new DependencyNotFoundExceptionFilter(artifactRegexWithoutClassifier);
        // the artifact has classifier, but the regex does not, so the classifier should be ignored in the comparison
        // and the exception then ignored
        Artifact artifact = new DefaultArtifact("com.acme", "finance-stuff", "classes", "jar", "1.0-redhat-2");
        DependencyNotFoundException ex = new DependencyNotFoundException(new Exception(), artifact, DEFAULT_VALIDATED_ARTIFACT);
        assertExceptionIgnored(filter, ex, new File("some-file"));
    }

}
