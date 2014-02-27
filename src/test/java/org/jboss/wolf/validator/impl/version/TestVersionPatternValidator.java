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
public class TestVersionPatternValidator extends AbstractTest {

    @Configuration
    public static class TestConfiguration {

        @Bean
        public IOFileFilter versionPatternValidatorFilter() {
            return trueFileFilter();
        }

    }

    @Test
    public void shouldSuccess() {
        String[] versions = {
                "1-redhat-1",
                "1-redhat-1",
                "1-redhat-1234567890",
                "1.2.3-redhat-1",
                "1.2.GA-redhat-2",
                "1.23.456.Final-redhat-333",
                "1.redhat-1",
                "1.redhat-1234567890",
                "1.2.3.redhat-1",
                "1.2.GA.redhat-2",
                "1.23.456.Final.redhat-333"
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
                "1-redHat-1",
                "1-redhat",
                "1-redhat-",
                "1-redhat-alfa",
                "1-redHat-1",
                "1.redhat",
                "1.redhat-",
                "1.redhat-alfa"
        };

        for (String version : versions) {
            init();
            pom().version(version).create(repoFooDir);
            validationExecutor.execute(ctx);
            assertExpectedException(VersionPatternException.class, version);
        }
    }

}