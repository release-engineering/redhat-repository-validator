package org.jboss.wolf.validator.impl;

import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.inject.Inject;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.jboss.wolf.validator.Reporter;
import org.jboss.wolf.validator.ReportingExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class AbstractReporterTest extends AbstractTest {

    @Inject
    protected ReportingExecutor reportingExecutor;
    @Inject
    protected ByteArrayOutputStream out;

    @Configuration
    public static class TestConfiguration {

        @Bean
        public ByteArrayOutputStream outputStream() {
            return new ByteArrayOutputStream();
        }

        @Bean
        public PrintStream defaultReporterStream() {
            return new PrintStream(outputStream());
        }

        @Bean
        public IOFileFilter defaultFilter() {
            return FileFilterUtils.trueFileFilter();
        }

    }
    
    protected void assertReportContains(String pattern) {
        String report = out.toString();
        if (!report.contains(pattern)) {
            System.out.println(report);
            fail("Report should contains: " + pattern);
        }
    }

}