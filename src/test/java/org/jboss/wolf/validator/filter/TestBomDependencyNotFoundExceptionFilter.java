package org.jboss.wolf.validator.filter;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DependencyNode;
import org.jboss.wolf.validator.impl.bom.BomDependencyNotFoundException;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;

public class TestBomDependencyNotFoundExceptionFilter extends AbstractExceptionFilterTest {
    private static final String MISSING_ARTIFACT_REGEX = "com.acme:finance.*:.*:war";
    private static final Artifact DEFAULT_VALIDATED_ARTIFACT = new DefaultArtifact("com.acme", "acme-parent", "pom", "1.0.0");

    @Test
    public void shouldIgnoreExceptionWithMatchingArtifact() {
        BomDependencyNotFoundExceptionFilter filter = new BomDependencyNotFoundExceptionFilter(MISSING_ARTIFACT_REGEX);
        Artifact artifact = new DefaultArtifact("com.acme", "finance-stuff", "war", "1.0-redhat-2");
        DependencyNode mockDepNode = Mockito.mock(DependencyNode.class);
        BomDependencyNotFoundException ex =
                new BomDependencyNotFoundException(new Exception(), artifact, DEFAULT_VALIDATED_ARTIFACT, mockDepNode);
        assertExceptionIgnored(filter, ex, new File("some-file"));
    }

    @Test
    public void shouldNotIgnoreExceptionOnlyWithMatchingArtifact() {
        BomDependencyNotFoundExceptionFilter filter = new BomDependencyNotFoundExceptionFilter(MISSING_ARTIFACT_REGEX);
        // Exception != DependencyNotFoundException, so it should not be ignored
        Exception ex = new Exception("Some message");
        assertExceptionNotIgnored(filter, ex, new File("some-file"));
    }

    @Test
    public void shouldNotIgnoreExceptionOnlyWithMatchingType() {
        BomDependencyNotFoundExceptionFilter filter = new BomDependencyNotFoundExceptionFilter(MISSING_ARTIFACT_REGEX);
        // "wrong-finance-stuff" artifactId does not match the defined regex
        Artifact artifact = new DefaultArtifact("com.acme", "wrong-finance-stuff", "war", "1.0-redhat-2");
        DependencyNode mockDepNode = Mockito.mock(DependencyNode.class);
        BomDependencyNotFoundException ex =
                new BomDependencyNotFoundException(new Exception(), artifact, DEFAULT_VALIDATED_ARTIFACT, mockDepNode);
        assertExceptionNotIgnored(filter, ex, new File("some-file"));
    }

    @Test
    public void shouldIgnoreOnlyExceptionsComingFromSpecifiedPom() {
        String validatedArtifactRegex = "com.acme:parent:1.0.1:pom";
        BomDependencyNotFoundExceptionFilter filter = new BomDependencyNotFoundExceptionFilter(MISSING_ARTIFACT_REGEX,
                validatedArtifactRegex);
        Artifact missingArtifact = new DefaultArtifact("com.acme", "finance-stuff", "war", "1.0-redhat-2");
        Artifact validatedArtifact = new DefaultArtifact("com.acme", "parent", "pom", "1.0.1");
        DependencyNode mockDepNode = Mockito.mock(DependencyNode.class);
        BomDependencyNotFoundException ex =
                new BomDependencyNotFoundException(new Exception(), missingArtifact, validatedArtifact, mockDepNode);
        assertExceptionIgnored(filter, ex, new File("some-file"));

        // now use the validated artifact that does not match the pattern used by the filter
        validatedArtifact = new DefaultArtifact("com.acme", "some-artifact", "pom", "2.0");
        ex = new BomDependencyNotFoundException(new Exception(), missingArtifact, validatedArtifact, mockDepNode);
        assertExceptionNotIgnored(filter, ex, new File("some-file"));
    }

}
