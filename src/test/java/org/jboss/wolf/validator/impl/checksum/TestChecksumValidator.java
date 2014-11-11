package org.jboss.wolf.validator.impl.checksum;

import static org.apache.commons.io.filefilter.FileFilterUtils.notFileFilter;
import static org.jboss.wolf.validator.impl.TestUtil.pom;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.jboss.wolf.validator.impl.AbstractTest;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration
public class TestChecksumValidator extends AbstractTest {

    @Configuration
    public static class TestConfiguration {
        
        @Resource(name = "expectedRootFilesFilter")
        private IOFileFilter expectedRootFilesFilter;

        @Bean
        public IOFileFilter checksumValidatorFilter() {
            return notFileFilter(expectedRootFilesFilter);
        }

    }

    private final File fooDir = new File(repoFooDir, "com/acme/foo/1.0");

    @Test
    public void shouldSuccess() {
        pom().artifactId("foo").create(repoFooDir);

        validationExecutor.execute(ctx);

        assertSuccess();
        assertTrue(new File(fooDir, "foo-1.0.jar.md5").exists());
        assertTrue(new File(fooDir, "foo-1.0.jar.sha1").exists());
        assertTrue(new File(fooDir, "foo-1.0.pom.md5").exists());
        assertTrue(new File(fooDir, "foo-1.0.pom.sha1").exists());
    }

    @Test
    public void shouldFindMissingSha1Checksum() {
        pom().artifactId("foo").create(repoFooDir);
        FileUtils.deleteQuietly(new File(fooDir, "foo-1.0.jar.sha1"));

        validationExecutor.execute(ctx);
        assertExpectedException(ChecksumNotExistException.class, "foo-1.0.jar");
    }

    @Test
    public void shouldFindMissingMd5Checksum() {
        pom().artifactId("foo").create(repoFooDir);
        FileUtils.deleteQuietly(new File(fooDir, "foo-1.0.jar.md5"));

        validationExecutor.execute(ctx);
        assertExpectedException(ChecksumNotExistException.class, "foo-1.0.jar");
    }

    @Test
    public void shouldFindNotMatchSha1Checksum() throws IOException {
        pom().artifactId("foo").create(repoFooDir);
        FileUtils.writeStringToFile(new File(fooDir, "foo-1.0.jar.sha1"), "checksum");

        validationExecutor.execute(ctx);
        assertExpectedException(ChecksumNotMatchException.class, "foo-1.0.jar");
    }

    @Test
    public void shouldFindNotMatchMd5Checksum() throws IOException {
        pom().artifactId("foo").create(repoFooDir);
        FileUtils.writeStringToFile(new File(fooDir, "foo-1.0.jar.md5"), "checksum");

        validationExecutor.execute(ctx);
        assertExpectedException(ChecksumNotMatchException.class, "foo-1.0.jar");
    }
    
    @Test
    public void shouldIgnoreReadmeAndExampleSettingsAndMavenRepository() throws IOException {
        FileUtils.touch(new File(repoFooDir, "example-settings.xml"));
        FileUtils.touch(new File(repoFooDir, "readme.txt"));
        FileUtils.touch(new File(repoFooDir, "readme.md"));
        FileUtils.touch(new File(repoFooDir, ".maven-repository"));
        
        validationExecutor.execute(ctx);
        assertSuccess();
    }

}