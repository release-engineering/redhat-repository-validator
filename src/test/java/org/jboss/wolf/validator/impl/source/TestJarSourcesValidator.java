package org.jboss.wolf.validator.impl.source;

import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;
import static org.jboss.wolf.validator.impl.TestUtil.pom;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.jboss.wolf.validator.impl.AbstractTest;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration
public class TestJarSourcesValidator extends AbstractTest {
    @Configuration
    public static class TestConfiguration {
        @Bean
        public IOFileFilter jarSourcesValidatorFilter() {
            return trueFileFilter();
        }
    }

    private final File fooJarSources = new File(repoFooDir, "com/acme/foo/1.0/foo-1.0-sources.jar");
    private final File fooJarTests = new File(repoFooDir, "com/acme/foo/1.0/foo-1.0-tests.jar");
    private final File fooJarJavaDoc = new File(repoFooDir, "com/acme/foo/1.0/foo-1.0-javadoc.jar");

    @Test
    public void containsSourcesJar_shouldSuccess() throws IOException {
        pom().artifactId("foo").create(repoFooDir);
        FileUtils.touch(fooJarSources);
        validator.validate(ctx);
        assertSuccess();
    }

    @Test
    public void removeSourcesJar_shouldFail() throws IOException {
        pom().artifactId("foo").create(repoFooDir);
        FileUtils.deleteQuietly(fooJarSources);
        validator.validate(ctx);

        assertExpectedException(JarSourcesVerificationException.class, "Unable to find sources for file com/acme/foo/1.0/foo-1.0.jar");
    }

    @Test
    public void addTestsJar_shouldFail() throws IOException {
        pom().artifactId("foo").create(repoFooDir);
        FileUtils.touch(fooJarTests);
        validator.validate(ctx);

        assertExpectedException(JarSourcesVerificationException.class, "Unable to find sources for file com/acme/foo/1.0/foo-1.0.jar");
    }

    @Test
    public void addJavaDocJar_shouldFail() throws IOException {
        pom().artifactId("foo").create(repoFooDir);
        FileUtils.touch(fooJarJavaDoc);
        validator.validate(ctx);

        assertExpectedException(JarSourcesVerificationException.class, "Unable to find sources for file com/acme/foo/1.0/foo-1.0.jar");
    }

    @Test
    public void addSourcesJar_shouldSuccess() throws IOException {
        pom().artifactId("foo").create(repoFooDir);
        FileUtils.touch(fooJarSources);
        validator.validate(ctx);

        assertSuccess();
    }
}
