package org.jboss.wolf.validator.filter;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.jboss.wolf.validator.impl.DependencyNotFoundException;
import org.junit.Test;

import java.io.File;

public class TestDependencyNotFoundExceptionFilter extends AbstractExceptionFilterTest {
    private final String GAV_WITH_EXTENSION_REGEX = "com.acme:finance.*:.*:war";

    @Test
    public void shouldIgnoreExceptionWithMatchingArtifact() {
        DependencyNotFoundExceptionFilter filter = new DependencyNotFoundExceptionFilter(GAV_WITH_EXTENSION_REGEX);
        Artifact artifact = new DefaultArtifact("com.acme", "finance-stuff", "war", "1.0-redhat-2");
        Exception ex = new DependencyNotFoundException(new Exception(), artifact);
        assertExceptionIgnored(filter, ex, new File("some-file"));
    }

    @Test
    public void shouldNotIgnoreExceptionOnlyWithMatchingArtifact() {
        DependencyNotFoundExceptionFilter filter = new DependencyNotFoundExceptionFilter(GAV_WITH_EXTENSION_REGEX);
        // Exception != DependencyNotFoundException, so it should not be ignored
        Exception ex = new Exception("Some message");
        assertExceptionNotIgnored(filter, ex, new File("some-file"));
    }

    @Test
    public void shouldNotIgnoreExceptionOnlyWithMatchingType() {
        DependencyNotFoundExceptionFilter filter = new DependencyNotFoundExceptionFilter(GAV_WITH_EXTENSION_REGEX);
        // "wrong-finance-stuff" artifactId does not match the defined regex
        Artifact artifact = new DefaultArtifact("com.acme", "wrong-finance-stuff", "war", "1.0-redhat-2");
        Exception ex = new DependencyNotFoundException(new Exception(), artifact);
        assertExceptionNotIgnored(filter, ex, new File("some-file"));
    }

    @Test
    public void shouldIgnoreOnlyExceptionComingFromSpecifiedPom() {
        File pomFileInRepo = new File("some-pom-file"); // need just an object of type File, not actual pom
        DependencyNotFoundExceptionFilter filter = new DependencyNotFoundExceptionFilter(GAV_WITH_EXTENSION_REGEX, pomFileInRepo);
        Artifact artifact = new DefaultArtifact("com.acme", "finance-stuff", "war", "1.0-redhat-2");
        Exception ex = new DependencyNotFoundException(new Exception(), artifact);
        assertExceptionIgnored(filter, ex, pomFileInRepo);
        assertExceptionNotIgnored(filter, ex, new File("some-file"));
    }

}
