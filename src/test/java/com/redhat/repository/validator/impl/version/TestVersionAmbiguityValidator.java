package com.redhat.repository.validator.impl.version;

import static com.redhat.repository.validator.impl.TestUtil.pom;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import com.redhat.repository.validator.impl.AbstractTest;
import com.redhat.repository.validator.impl.version.VersionAmbiguityException;

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

        validationExecutor.execute(ctx);

        assertExpectedException(VersionAmbiguityException.class, "Artifact com.acme:foo has multiple versions: 1.1, 1.2, 1.3");
    }

}