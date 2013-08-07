package org.jboss.wolf.validator.impl;

import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.nameFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.notFileFilter;

import java.io.File;
import java.io.IOException;

import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration
public class TestSuspiciousFileValidator extends AbstractTest {

    @Configuration
    public static class TestConfiguration {
        
        @Resource(name = "expectedRootFilesFilter")
        private IOFileFilter expectedRootFilesFilter;

        @Bean
        public IOFileFilter suspiciousFileValidatorFilter() {
            return and(
                    notFileFilter(expectedRootFilesFilter),
                    notFileFilter(nameFileFilter("expected-directory")),
                    notFileFilter(nameFileFilter("expected-file")));
        }

    }

    @Before
    public void initRepo() {
        touch("readme.txt");
        touch("README.MD");
        touch("example-settings.xml");
        touch("JBossEULA.txt");
        touch("com/acme/foo/1.0/foo-1.0.pom");
        touch("com/acme/foo/1.0/foo-1.0.pom.md5");
        touch("com/acme/foo/1.0/foo-1.0.pom.sha1");
        touch("com/acme/foo/1.0/foo-1.0.jar");
        touch("com/acme/foo/1.0/foo-1.0.jar.md5");
        touch("com/acme/foo/1.0/foo-1.0.jar.sha1");
        touch("com/acme/foo/1.0/foo-1.0-sources.jar");
        touch("com/acme/foo/1.0/foo-1.0-sources.jar.md5");
        touch("com/acme/foo/1.0/foo-1.0-sources.jar.sha1");
        touch("com/acme/foo/1.0/foo-1.0-javadoc.jar");
        touch("com/acme/foo/1.0/foo-1.0-javadoc.jar.md5");
        touch("com/acme/foo/1.0/foo-1.0-javadoc.jar.sha1");
    }

    @Test
    public void shouldSuccess() {
        validator.validate(ctx);
        assertSuccess();
    }

    @Test
    public void shouldFindSuspiciousRootFile() {
        touch("foo.txt");
        validator.validate(ctx);
        assertExpectedException(SuspiciousFileException.class, "File foo.txt is suspicious");
    }

    @Test
    public void shouldFindSuspiciousEmptyDirectory() throws IOException {
        FileUtils.forceMkdir(new File(repoFooDir, "com/acme/bar"));
        validator.validate(ctx);
        assertExpectedException(SuspiciousFileException.class, "File com/acme/bar is empty directory");
    }

    @Test
    public void shouldFindSuspiciousChecksums() throws IOException {
        FileUtils.forceDelete(new File(repoFooDir, "com/acme/foo/1.0/foo-1.0-sources.jar"));
        validator.validate(ctx);
        assertExpectedException(SuspiciousFileException.class, "File com/acme/foo/1.0/foo-1.0-sources.jar.md5 is checksum without source file");
        assertExpectedException(SuspiciousFileException.class, "File com/acme/foo/1.0/foo-1.0-sources.jar.sha1 is checksum without source file");
    }

    @Test
    public void shouldFindSuspiciousJar() throws IOException {
        touch("com/acme/bar/1.0/bar-1.0.jar");
        validator.validate(ctx);
        assertExpectedException(SuspiciousFileException.class, "File com/acme/bar/1.0/bar-1.0.jar is jar file without pom");
    }

    @Test
    public void shouldFindSuspiciousSources() throws IOException {
        touch("com/acme/bar/1.0/bar-1.0-sources.jar");
        validator.validate(ctx);
        assertExpectedException(SuspiciousFileException.class, "File com/acme/bar/1.0/bar-1.0-sources.jar is artifact sources.jar without primary jar file");
    }

    @Test
    public void shouldFindSuspiciousWar() throws IOException {
        touch("com/acme/foo/1.0/foo-1.0.war");
        validator.validate(ctx);
        assertExpectedException(SuspiciousFileException.class, "File com/acme/foo/1.0/foo-1.0.war is suspicious");
    }
    
    @Test
    public void shouldFindSuspiciousClassifiers() {
        touch("com/acme/foo/1.0/foo-1.0-native.jar");
        validator.validate(ctx);
        assertExpectedException(SuspiciousFileException.class, "File com/acme/foo/1.0/foo-1.0-native.jar is jar file without pom, but there is other pom in directory foo-1.0.pom");
    }

    @Test
    public void shouldIgnoreExpectedDirectory() throws IOException {
        FileUtils.forceMkdir(new File(repoFooDir, "expected-directory"));
        validator.validate(ctx);
        assertSuccess();
    }

    @Test
    public void shouldIgnoreExpectedFile() {
        touch("expected-file");
        validator.validate(ctx);
        assertSuccess();
    }

    private void touch(String file) {
        try {
            FileUtils.touch(new File(repoFooDir, file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}