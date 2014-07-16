package org.jboss.wolf.validator.impl.osgi;

import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;
import static org.jboss.wolf.validator.impl.TestUtil.pom;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.jboss.wolf.validator.impl.AbstractTest;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration
public class TestOsgiVersionValidator extends AbstractTest {

    @Configuration
    public static class TestConfiguration {

        @Bean
        public IOFileFilter osgiVersionValidatorFilter() {
            return trueFileFilter();
        }

    }

    @Test
    public void shouldSuccess() {
        String[] versions = {
                "0.0.0",
                "0.0.0.0",
                "0.0.0.beta",
                "0.0.0.Beta-1",
                "12.345.6789.abc-XYZ_0"
        };

        for (String version : versions) {
            init();
            pom().version(version).create(repoFooDir);
            validationExecutor.execute(ctx);
            assertSuccess();
        }
    }

    @Test
    public void shouldFail() {
        String[] versions = {
                "1",
                "1.2",
                "1.beta",
                "1.2.beta",
                "1.2.3-beta",
                "1.-2"
        };

        for (String version : versions) {
            init();
            pom().version(version).create(repoFooDir);
            validationExecutor.execute(ctx);
            assertExpectedException(OsgiVersionException.class, version);
        }
    }

}