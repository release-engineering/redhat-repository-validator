package org.jboss.wolf.validator.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.jboss.wolf.validator.impl.checksum.ChecksumNotExistException;
import org.jboss.wolf.validator.impl.checksum.ChecksumNotMatchException;
import org.jboss.wolf.validator.impl.suspicious.SuspiciousFileException;
import org.junit.Test;

public class TestDefaultReporter extends AbstractReporterTest {

    private final File f1 = new File("f1");
    private final File f2 = new File("f2");
    private final File f3 = new File("f3");

    @Test
    public void shouldReportExceptionCount() {
        ctx.addError(null, f1, new SuspiciousFileException(f1, "foo"));
        ctx.addError(null, f2, new SuspiciousFileException(f2, "foo"));
        ctx.addError(null, f1, new ChecksumNotExistException(f1, "sha1"));
        ctx.addError(null, f2, new ChecksumNotExistException(f2, "sha1"));
        ctx.addError(null, f3, new ChecksumNotExistException(f3, "sha1"));
        
        reportingExecutor.execute(ctx);

        assertReportContains(
                  "--- ChecksumNotExistException (total count 3) ---\n"
                + "Checksum sha1 for file f1 not exist\n"
                + "Checksum sha1 for file f2 not exist\n"
                + "Checksum sha1 for file f3 not exist\n"
                + "\n"
                + "--- SuspiciousFileException (total count 2) ---\n"
                + "File f1 is foo\n"
                + "File f2 is foo");
    }

    @Test
    public void shouldReportOnlyDistinctException() {
        SuspiciousFileException suspiciousFileException = new SuspiciousFileException(f1, "foo");
        ctx.addError(null, f1, suspiciousFileException);
        ctx.addError(null, f2, suspiciousFileException);
        ctx.addError(null, f3, suspiciousFileException);

        reportingExecutor.execute(ctx);

        assertEquals(1, StringUtils.countMatches(readReport(), "File f1 is foo"));
    }

    @Test
    public void shouldSortExceptionsByType() {
        ctx.addError(null, f1, new ChecksumNotExistException(f1, "sha1"));
        ctx.addError(null, f2, new ChecksumNotExistException(f2, "sha1"));
        ctx.addError(null, f1, new ChecksumNotMatchException(f1, "sha1", "0", "1"));
        ctx.addError(null, f2, new ChecksumNotMatchException(f2, "sha1", "0", "1"));

        reportingExecutor.execute(ctx);

        String report = readReport();
        int index1 = report.indexOf("Checksum sha1 for file f1 not exist");
        int index2 = report.indexOf("Checksum sha1 for file f2 not exist");
        int index3 = report.indexOf("Checksum sha1 for file f1 not match");
        int index4 = report.indexOf("Checksum sha1 for file f2 not match");

        assertTrue(index1 < index2);
        assertTrue(index2 < index3);
        assertTrue(index3 < index4);
    }

    @Test
    public void shouldSortExceptionsByMessage() {
        ctx.addError(null, f3, new ChecksumNotExistException(f3, "sha1"));
        ctx.addError(null, f2, new ChecksumNotExistException(f2, "sha1"));
        ctx.addError(null, f1, new ChecksumNotExistException(f1, "sha1"));

        reportingExecutor.execute(ctx);

        String report = readReport();
        int index1 = report.indexOf("Checksum sha1 for file f1 not exist");
        int index2 = report.indexOf("Checksum sha1 for file f2 not exist");
        int index3 = report.indexOf("Checksum sha1 for file f3 not exist");

        assertTrue(index1 < index2);
        assertTrue(index2 < index3);
    }
    
    @Test
    public void shouldNotCrashWhenExceptionHasNullMessage() {
        ctx.addError(null, f1, new Exception((String)null));
        
        reportingExecutor.execute(ctx);
        
        assertReportContains(
                "--- Exception (total count 1) ---\n"
              + "java.lang.Exception\n"
              + "\tat org.jboss.wolf.validator.impl.TestDefaultReporter.shouldNotCrashWhenExceptionHasNullMessage(");
    }

}