package com.redhat.repository.validator.impl.signature;

import static com.redhat.repository.validator.impl.TestUtil.pom;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import com.redhat.repository.validator.impl.AbstractTest;
import com.redhat.repository.validator.impl.signature.JarSignatureVerificationException;
import com.redhat.repository.validator.impl.signature.JarSignedException;

@ContextConfiguration
public class TestJarSignatureValidator extends AbstractTest {

    @Configuration
    public static class TestConfiguration {

        @Bean
        public IOFileFilter jarSignatureValidatorFilter() {
            return trueFileFilter();
        }

    }

    private final File fooJar = new File(repoFooDir, "com/acme/foo/1.0/foo-1.0.jar");

    @Test
    public void shouldSuccess() throws IOException {
        pom().artifactId("foo").create(repoFooDir);
        validationExecutor.execute(ctx);
        assertSuccess();
    }

    @Test
    public void shouldFindSignedJar() throws IOException {
        pom().artifactId("foo").create(repoFooDir);

        FileUtils.deleteQuietly(fooJar);
        FileUtils.copyFile(new File("target/test-classes/empty-signed.jar"), fooJar);

        validationExecutor.execute(ctx);

        assertExpectedException(JarSignedException.class, "File com/acme/foo/1.0/foo-1.0.jar is signed");
    }

    @Test
    public void shouldFindDamagedSignedJar() throws IOException {
        pom().artifactId("foo").create(repoFooDir);

        FileUtils.deleteQuietly(fooJar);
        FileUtils.copyFile(new File("target/test-classes/empty-signed-damaged.jar"), fooJar);

        validationExecutor.execute(ctx);

        assertExpectedException(JarSignatureVerificationException.class, "Unable to verify signature for file com/acme/foo/1.0/foo-1.0.jar");
    }

}