package org.jboss.wolf.validator.impl.version;

import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;
import static org.jboss.wolf.validator.impl.TestUtil.pom;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.jboss.wolf.validator.impl.AbstractTest;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration
public class TestVersionAmbiguityValidator extends AbstractTest {

    @Configuration
    public static class TestConfiguration {

        @Bean
        public IOFileFilter versionAmbiguityValidatorFilter() {
            return trueFileFilter();
        }

    }

    @Test
    public void shouldFindAmbiguousVersions() {
        pom().version("1.1").create(repoFooDir);
        pom().version("1.2").create(repoFooDir);
        pom().version("1.3").create(repoFooDir);

        validator.validate(ctx);

        assertExpectedException(VersionAmbiguityException.class, "Artifact com.acme:foo has multiple versions 1.3, 1.2, 1.1");
    }

}