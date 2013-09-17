package org.jboss.wolf.validator.impl.distribution;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.jboss.wolf.validator.impl.AbstractTest;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;
import java.io.IOException;

import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;
import static org.jboss.wolf.validator.impl.TestUtil.pom;

@ContextConfiguration
public class DistributionValidatorTest extends AbstractTest {

    @Configuration
    public static class TestConfiguration {
        @Bean
        public IOFileFilter distributionValidatorFilter() {
            return trueFileFilter();
        }
    }

    private final File distFooJar = new File(distributionDir, "foo-1.0.jar");
    private final File distBarJar = new File(distributionDir, "bar-1.0.jar");
    private final File distBazJar = new File(distributionDir, "baz-1.0.jar");

    @Test
    public void deleteDistFolder_shouldSkipValidation() throws IOException {
        FileUtils.deleteDirectory(distributionDir);

        validator.validate(ctx);
        assertSuccess();
    }

    @Test
    public void sameJarInRepoAndDist_shouldSuccess() throws IOException {
        pom().artifactId("foo").create(repoFooDir);

        FileUtils.copyFile(new File(repoFooDir + "/com/acme/foo/1.0/foo-1.0.jar"), distFooJar);

        validator.validate(ctx);
        assertSuccess();
    }

    @Test
    public void noJarInDist_shouldFail() throws IOException {
        pom().artifactId("foo").create(repoFooDir);

        //FileUtils.copyFile(new File(repoFooDir + "/com/acme/foo/1.0/foo-1.0.jar"), distFooJar);

        validator.validate(ctx);
        assertExpectedException(DistributionFileException.class, "File com/acme/foo/1.0/foo-1.0.jar is not present in DISTRIBUTION");
    }

    @Test
    public void noJarInRepo_shouldFail() throws IOException {
        pom().artifactId("foo").create(repoFooDir);

        FileUtils.copyFile(new File(repoFooDir + "/com/acme/foo/1.0/foo-1.0.jar"), distFooJar);
        FileUtils.deleteQuietly(new File(repoFooDir + "/com/acme/foo/1.0/foo-1.0.jar"));

        validator.validate(ctx);
        assertExpectedException(DistributionFileException.class, "File foo-1.0.jar is not pressent in REPOSITORY");
    }

    @Test
    public void equalJarsInRepo_shouldFail() throws IOException {
        pom().artifactId("foo").create(repoFooDir);
        pom().artifactId("bar").create(repoFooDir);
        pom().artifactId("baz").create(repoFooDir);

        FileUtils.copyFile(new File(repoFooDir + "/com/acme/foo/1.0/foo-1.0.jar"), distFooJar);

        validator.validate(ctx);
        assertExpectedException(DistributionEqualFilesException.class, "Files from REPOSITORY [com/acme/bar/1.0/bar-1.0.jar, com/acme/baz/1.0/baz-1.0.jar, com/acme/foo/1.0/foo-1.0.jar] are equal");
    }

    @Test
    public void equalJarsInDist_shouldFail() throws IOException {
        pom().artifactId("foo").create(repoFooDir);

        FileUtils.copyFile(new File(repoFooDir + "/com/acme/foo/1.0/foo-1.0.jar"), distFooJar);
        FileUtils.copyFile(new File(repoFooDir + "/com/acme/foo/1.0/foo-1.0.jar"), distBarJar);
        FileUtils.copyFile(new File(repoFooDir + "/com/acme/foo/1.0/foo-1.0.jar"), distBazJar);

        validator.validate(ctx);
        assertExpectedException(DistributionEqualFilesException.class, "Files from DISTRIBUTION [bar-1.0.jar, baz-1.0.jar, foo-1.0.jar] are equal");
    }

    @Test
    public void notEqualJarName_shouldFail() throws IOException {
        pom().artifactId("foo").create(repoFooDir);

        FileUtils.copyFile(new File(repoFooDir + "/com/acme/foo/1.0/foo-1.0.jar"), distBarJar);

        validator.validate(ctx);
        assertExpectedException(DistributionNotEqualNamesException.class, "File from REPOSITORY com/acme/foo/1.0/foo-1.0.jar does not have the same name as file from DISTRIBUTION bar-1.0.jar");
    }

    @Test
    public void notEqualJarSize_shouldFail() throws IOException {
        pom().artifactId("foo").create(repoFooDir);

        FileUtils.copyFile(new File("target/test-classes/empty-signed-damaged.jar"), distFooJar);

        validator.validate(ctx);
        assertExpectedException(DistributionNotEqualSizeException.class, "File from REPOSITORY com/acme/foo/1.0/foo-1.0.jar has not same size as file from DISTRIBUTION foo-1.0.jar");
    }

}