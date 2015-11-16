package com.redhat.repository.validator.impl.osgi;

import static com.redhat.repository.validator.impl.TestUtil.pom;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import com.redhat.repository.validator.impl.AbstractTest;
import com.redhat.repository.validator.impl.osgi.OsgiVersionException;

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