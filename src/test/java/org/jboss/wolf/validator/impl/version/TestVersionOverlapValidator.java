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
public class TestVersionOverlapValidator extends AbstractTest {

    @Configuration
    public static class TestConfiguration {
        
        @Bean
        public IOFileFilter modelValidatorFilter() {
            return trueFileFilter();
        }
        
        @Bean
        public IOFileFilter dependenciesValidatorFilter() {
            return trueFileFilter();
        }

        @Bean
        public IOFileFilter versionOverlapValidatorFilter() {
            return trueFileFilter();
        }

    }

    @Test
    public void shouldSuccess() {
        pom().version("1.1").create(repoFooDir);
        pom().version("1.2").create(repoBarDir);
        pom().version("1.3").create(repoBazDir);

        validationExecutor.execute(ctx);

        assertSuccess();
    }

    @Test
    public void shouldFindVersionOverlap() {
        pom().create(repoFooDir);
        pom().create(repoBarDir);
        pom().create(repoBazDir);

        validationExecutor.execute(ctx);

        assertExpectedException(VersionOverlapException.class, "Artifact com.acme:foo:jar:1.0 has overlap with remote repository: bar");
        assertExpectedException(VersionOverlapException.class, "Artifact com.acme:foo:jar:1.0 has overlap with remote repository: baz");
    }

}