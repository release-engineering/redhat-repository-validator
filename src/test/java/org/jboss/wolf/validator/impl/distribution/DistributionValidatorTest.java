package org.jboss.wolf.validator.impl.distribution;

import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;
import static org.jboss.wolf.validator.impl.TestUtil.pom;
import static org.jboss.wolf.validator.impl.suspicious.TestSuspiciousFileValidator.touch;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.model.Model;
import org.jboss.wolf.validator.impl.AbstractTest;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

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
    public void shouldSkipValidationIfDirectoryDontExists() throws IOException {
        FileUtils.deleteDirectory(distributionDir);
        
        validationExecutor.execute(ctx);
        assertSuccess();
    }

    @Test
    public void shouldSuccess() throws IOException {
        pom().artifactId("foo").create(repoFooDir);
        touch("com/acme/foo/1.0/foo-1.0-sources.jar");
        touch("com/acme/foo/1.0/foo-1.0-javadoc.jar");
        touch("com/acme/foo/1.0/foo-1.0-tests.jar");
        touch("com/acme/foo/1.0/foo-1.0-test-sources.jar");
        FileUtils.copyFile(new File(repoFooDir + "/com/acme/foo/1.0/foo-1.0.jar"), distFooJar);
        
        validationExecutor.execute(ctx);
        assertSuccess();
    }

    @Test
    public void shouldFindCoruptedFiles() throws IOException {
        pom().artifactId("foo").create(repoFooDir);
        FileUtils.copyFile(new File("target/test-classes/empty-signed-damaged.jar"), distFooJar);
    
        validationExecutor.execute(ctx);
        assertExpectedException(DistributionCoruptedFileException.class, "File in distribution foo-1.0.jar has same name like file in repository com/acme/foo/1.0/foo-1.0.jar, but has different content");
    }

    @Test
    public void shouldFindDuplicatedFiles() throws IOException {
        pom().artifactId("foo").create(repoFooDir);
        FileUtils.copyFile(new File(repoFooDir + "/com/acme/foo/1.0/foo-1.0.jar"), distFooJar);
        FileUtils.copyFile(new File(repoFooDir + "/com/acme/foo/1.0/foo-1.0.jar"), distBarJar);
        FileUtils.copyFile(new File(repoFooDir + "/com/acme/foo/1.0/foo-1.0.jar"), distBazJar);
    
        validationExecutor.execute(ctx);
        assertExpectedException(DistributionDuplicateFilesException.class, "Duplicate files in distribution: bar-1.0.jar, baz-1.0.jar, foo-1.0.jar");
    }

    @Test
    public void shouldFindMisnomerFiles() throws IOException {
        pom().artifactId("foo").create(repoFooDir);
        FileUtils.copyFile(new File(repoFooDir + "/com/acme/foo/1.0/foo-1.0.jar"), distBarJar);
    
        validationExecutor.execute(ctx);
        assertExpectedException(DistributionMisnomerFileException.class, "File in distribution bar-1.0.jar has same content like file in repository com/acme/foo/1.0/foo-1.0.jar, but has different name");
    }

    @Test
    public void shouldFindMissingFiles() throws IOException {
        pom().artifactId("foo").create(repoFooDir);
        
        validationExecutor.execute(ctx);
        assertExpectedException(DistributionMissingFileException.class, "Distribution doesn't contains file from repository: com/acme/foo/1.0/foo-1.0.jar");
    }

    @Test
    public void shouldFindRedundantFiles() throws IOException {
        pom().artifactId("foo").create(repoFooDir);
        FileUtils.copyFile(new File(repoFooDir + "/com/acme/foo/1.0/foo-1.0.jar"), distFooJar);
        FileUtils.deleteQuietly(new File(repoFooDir + "/com/acme/foo/1.0/foo-1.0.jar"));

        validationExecutor.execute(ctx);
        assertExpectedException(DistributionRedundantFileException.class, "Distribution contains file, which is not in repository: foo-1.0.jar");
    }

    @Test
    public void shouldNotFindRedundantFilesIfTheyComeFromRemoteRepositoriesDueTransitiveDependencies() throws IOException {
        Model barPom = pom().artifactId("bar").create(repoBarDir, "target/test-classes/empty-signed.jar");
        pom().artifactId("foo").dependency(barPom).create(repoFooDir);

        FileUtils.copyFile(new File(repoFooDir + "/com/acme/foo/1.0/foo-1.0.jar"), distFooJar);
        FileUtils.copyFile(new File(repoBarDir + "/com/acme/bar/1.0/bar-1.0.jar"), distBarJar);
        FileUtils.copyDirectory(repoFooDir, repoLocalDir);
        FileUtils.copyDirectory(repoBarDir, repoLocalDir);

        validationExecutor.execute(ctx);

        assertSuccess();
    }

}