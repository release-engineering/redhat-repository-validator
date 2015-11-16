package com.redhat.repository.validator.impl.bom;

import static com.redhat.repository.validator.impl.TestUtil.dependency;
import static com.redhat.repository.validator.impl.TestUtil.pom;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.model.Model;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import com.redhat.repository.validator.impl.AbstractTest;
import com.redhat.repository.validator.impl.bom.BomAmbiguousVersionException;

@ContextConfiguration
public class TestBomAmbiguousVersionValidator extends AbstractTest {

    @Configuration
    public static class TestConfiguration {

        @Bean
        public IOFileFilter bomAmbiguousVersionValidatorFilter() {
            return FileFilterUtils.trueFileFilter();
        }

    }

    @Test
    public void shouldFindAmbiguousVersions() {
        Model bar1 = pom().artifactId("bar").version("1.0").create(repoBarDir);
        Model bar2 = pom().artifactId("bar").version("2.0").create(repoBarDir);

        pom().artifactId("foo-bom-a").packaging("pom").dependencyManagement(bar1).create(repoFooDir);
        pom().artifactId("foo-bom-b").packaging("pom").dependencyManagement(bar2).create(repoFooDir);

        validationExecutor.execute(ctx);

        assertExpectedException(BomAmbiguousVersionException.class, "ambiguous version for dependency com.acme:bar:jar");
    }

    @Test
    public void shouldFindAmbiguousVersionsWithInterpolation() {
        Model barApi1 = pom().artifactId("bar-api").version("1.0").create(repoBarDir);
        Model barImpl1 = pom().artifactId("bar-impl").version("1.0").create(repoBarDir);

        Model barApi2 = pom().artifactId("bar-api").version("2.0").create(repoBarDir);
        Model barImpl2 = pom().artifactId("bar-impl").version("2.0").create(repoBarDir);

        pom().artifactId("foo-bom-a").
                packaging("pom").
                property("version.bar", "1.0").
                dependencyManagement(dependency().to(barApi1).version("${version.bar}").build()).
                dependencyManagement(dependency().to(barImpl1).version("${version.bar}").build()).
                create(repoFooDir);

        pom().artifactId("foo-bom-b").
                packaging("pom").
                property("version.bar", "2.0").
                dependencyManagement(dependency().to(barApi2).version("${version.bar}").build()).
                dependencyManagement(dependency().to(barImpl2).version("${version.bar}").build()).
                create(repoFooDir);

        validationExecutor.execute(ctx);

        assertExpectedException(BomAmbiguousVersionException.class, "ambiguous version for dependency com.acme:bar-api:jar");
        assertExpectedException(BomAmbiguousVersionException.class, "ambiguous version for dependency com.acme:bar-impl:jar");
    }

    @Test
    public void shouldIgnoreAmbiguousVersionsForDifferentType() {
        Model barJar1 = pom().artifactId("bar").version("1.0").create(repoBarDir);
        Model barWar2 = pom().artifactId("bar").version("2.0").packaging("war").create(repoBarDir);

        pom().artifactId("foo-bom-a").
                packaging("pom").
                dependencyManagement(dependency().to(barJar1).build()).
                create(repoFooDir);

        pom().artifactId("foo-bom-b").
                packaging("pom").
                dependencyManagement(dependency().to(barWar2).type("war").build()).
                create(repoFooDir);

        validationExecutor.execute(ctx);

        assertSuccess();
    }

}