package org.jboss.wolf.validator.impl;

import static org.apache.commons.io.IOCase.INSENSITIVE;
import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.nameFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.notFileFilter;
import static org.jboss.wolf.validator.impl.TestUtil.pom;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration
public class TestChecksumValidator extends AbstractTest {

    @Configuration
    public static class TestConfiguration {

        @Bean
        public IOFileFilter checksumValidatorFilter() {
            return and(
                    notFileFilter(nameFileFilter("example-settings.xml", INSENSITIVE)),
                    notFileFilter(nameFileFilter("readme.txt", INSENSITIVE)),
                    notFileFilter(nameFileFilter("readme.md", INSENSITIVE)));
        }

    }

    private final File fooDir = new File(repoFooDir, "com/acme/foo/1.0");

    @Test
    public void shouldSuccess() {
        pom().artifactId("foo").create(repoFooDir);

        validator.validate(ctx);

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

        validator.validate(ctx);
        assertExpectedException(ChecksumNotExistException.class, "foo-1.0.jar");
    }

    @Test
    public void shouldFindMissingMd5Checksum() {
        pom().artifactId("foo").create(repoFooDir);
        FileUtils.deleteQuietly(new File(fooDir, "foo-1.0.jar.md5"));

        validator.validate(ctx);
        assertExpectedException(ChecksumNotExistException.class, "foo-1.0.jar");
    }

    @Test
    public void shouldFindNotMatchSha1Checksum() throws IOException {
        pom().artifactId("foo").create(repoFooDir);
        FileUtils.writeStringToFile(new File(fooDir, "foo-1.0.jar.sha1"), "checksum");

        validator.validate(ctx);
        assertExpectedException(ChecksumNotMatchException.class, "foo-1.0.jar");
    }

    @Test
    public void shouldFindNotMatchMd5Checksum() throws IOException {
        pom().artifactId("foo").create(repoFooDir);
        FileUtils.writeStringToFile(new File(fooDir, "foo-1.0.jar.md5"), "checksum");

        validator.validate(ctx);
        assertExpectedException(ChecksumNotMatchException.class, "foo-1.0.jar");
    }
    
    @Test
    public void shouldIgnoreReadmeAndExampleSettings() throws IOException {
        FileUtils.touch(new File(repoFooDir, "example-settings.xml"));
        FileUtils.touch(new File(repoFooDir, "readme.txt"));
        FileUtils.touch(new File(repoFooDir, "readme.md"));
        
        validator.validate(ctx);
        assertSuccess();
    }

}