package org.jboss.wolf.validator.impl;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.jboss.wolf.validator.ReportingExecutor;
import org.junit.Before;
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

    @Configuration
    public static class TestConfiguration {

        @Bean
        public IOFileFilter defaultFilter() {
            return FileFilterUtils.trueFileFilter();
        }

    }
    
    @Before
    public void deleteReports() throws IOException {
        FileUtils.forceMkdir(new File("workspace"));
        FileUtils.deleteQuietly(new File("workspace/report.txt"));
    }    
    
    protected void assertReportContains(String pattern) {
        String report = readReport();
        if (!report.contains(pattern)) {
            System.out.println(report);
            fail("Report should contains [" + pattern + "] but contains [" + report + "]");
        }
    }

    protected String readReport() {
        try {
            return FileUtils.readFileToString(new File("workspace/report.txt"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}