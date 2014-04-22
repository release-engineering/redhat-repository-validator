package org.jboss.wolf.validator.filter;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.jboss.wolf.validator.impl.DependencyNotFoundException;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;

public class TestDependencyNotFoundExceptionFilter extends AbstractExceptionFilterTest {
    private static final String DEFAULT_MISSING_ARTIFACT_REGEX = "com.acme:finance.*:.*:war";
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
        String validatedArtifactRegex = "com.acme:parent:1.0.1:pom";
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

}
