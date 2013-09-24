package org.jboss.wolf.validator.impl;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.eclipse.aether.repository.RemoteRepository;
import org.jboss.wolf.validator.ValidatorContext;
import org.jboss.wolf.validator.impl.checksum.ChecksumNotExistException;
import org.jboss.wolf.validator.impl.suspicious.SuspiciousFileException;
import org.junit.Test;

public class TestSurefireXmlReporter {

    @Test
    public void shouldCreateSurefireXmlReports() throws IOException {
        File validateRepository = new File("workspace/validated-repository");
        File fooFile = new File(validateRepository, "foo");
        File barFile = new File(validateRepository, "bar");

        ValidatorContext ctx = new ValidatorContext(validateRepository, null, Collections.<RemoteRepository> emptyList());
        ctx.addException(fooFile, new SuspiciousFileException(fooFile, "suspicious because foo"));
        ctx.addException(barFile, new SuspiciousFileException(barFile, "suspicious because bar"));
        ctx.addException(fooFile, new ChecksumNotExistException(fooFile, "sha1"));
        ctx.addException(barFile, new ChecksumNotExistException(barFile, "sha1"));

        SurefireXmlReporter reporter = new SurefireXmlReporter();
        reporter.report(ctx);

        File suspiciousFileExceptionReportFile = new File("workspace/surefire-reports/TEST-SuspiciousFileException.xml");
        File checksumNotExistExceptionReportFile = new File("workspace/surefire-reports/TEST-ChecksumNotExistException.xml");

        assertTrue(suspiciousFileExceptionReportFile.exists());
        assertTrue(suspiciousFileExceptionReportFile.isFile());

        assertTrue(checksumNotExistExceptionReportFile.exists());
        assertTrue(checksumNotExistExceptionReportFile.isFile());

        String suspiciousFileExceptionReport = FileUtils.readFileToString(suspiciousFileExceptionReportFile);
        String checksumNotExistExceptionReport = FileUtils.readFileToString(checksumNotExistExceptionReportFile);

        assertTrue(suspiciousFileExceptionReport.contains("<testsuite name=\"SuspiciousFileException\""));
        assertTrue(suspiciousFileExceptionReport.contains("<testcase name=\"__File workspace/validated-repository/foo is suspicious because foo\" classname=\"SuspiciousFileException\""));
        assertTrue(suspiciousFileExceptionReport.contains("<testcase name=\"__File workspace/validated-repository/bar is suspicious because bar\" classname=\"SuspiciousFileException\""));

        assertTrue(checksumNotExistExceptionReport.contains("<testsuite name=\"ChecksumNotExistException\""));
        assertTrue(checksumNotExistExceptionReport.contains("<testcase name=\"__Checksum sha1 for file workspace/validated-repository/foo not exist\" classname=\"ChecksumNotExistException\""));
        assertTrue(checksumNotExistExceptionReport.contains("<testcase name=\"__Checksum sha1 for file workspace/validated-repository/bar not exist\" classname=\"ChecksumNotExistException\""));
    }
    
    @Test
    public void shouldNotCrashWhenExceptionHasNullMessage() {
        File validateRepository = new File("workspace/validated-repository");
        File f1 = new File(validateRepository, "f1");
        File f2 = new File(validateRepository, "f2");
        
        ValidatorContext ctx = new ValidatorContext(validateRepository, null, Collections.<RemoteRepository> emptyList());
        ctx.addException(f1, new Exception((String)null));
        ctx.addException(f2, new Exception((String)null));
        
        SurefireXmlReporter reporter = new SurefireXmlReporter();
        reporter.report(ctx);
        
        File exceptionReportFile = new File("workspace/surefire-reports/TEST-Exception.xml");

        assertTrue(exceptionReportFile.exists());
        assertTrue(exceptionReportFile.isFile());
    }

}