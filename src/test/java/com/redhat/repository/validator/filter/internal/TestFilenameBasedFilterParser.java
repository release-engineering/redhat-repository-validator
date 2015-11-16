package com.redhat.repository.validator.filter.internal;

import org.junit.Test;

import com.redhat.repository.validator.ExceptionFilter;
import com.redhat.repository.validator.filter.FilenameBasedExceptionFilter;
import com.redhat.repository.validator.impl.suspicious.SuspiciousFileException;

import java.util.List;

import static org.junit.Assert.*;

public class TestFilenameBasedFilterParser extends AbstractExceptionFilterParserTest {

    @Test
    public void shouldParseConfigWithFilenameRegexOnly() {
        initAppContext("/TestFilenameBasedFilterParser-regexOnly.xml");
        assertNumberOfBeansWithType(ExceptionFilter.class, 1);

        FilenameBasedExceptionFilter filter = getFirstMatchingBean(FilenameBasedExceptionFilter.class);
        assertFilter(filter, "regex-only-.*\\.jar", null);
    }

    @Test
    public void shouldParseConfigWithFilenameRegexAndExceptionType() {
        initAppContext("/TestFilenameBasedFilterParser-regexAndOneException.xml");
        assertNumberOfBeansWithType(ExceptionFilter.class, 1);

        FilenameBasedExceptionFilter filter = getFirstMatchingBean(FilenameBasedExceptionFilter.class);
        assertFilter(filter, "regex-and-exception.*\\.jar", SuspiciousFileException.class);
    }

    @Test
    public void shouldParseConfigWithFilenameRegexAndListOfExceptionTypes() {
        initAppContext("/TestFilenameBasedFilterParser-regexAndListOfExceptions.xml");
        assertNumberOfBeansWithType(ExceptionFilter.class, 2);

        List<FilenameBasedExceptionFilter> filters = getAllMatchingBeans(FilenameBasedExceptionFilter.class);
        assertContainsFilter(filters, "regex-and-list-of-exceptions-.*\\.jar", SuspiciousFileException.class);
        assertContainsFilter(filters, "regex-and-list-of-exceptions-.*\\.jar", TestingException.class);
    }

    @Test
    public void shouldParseConfigWithAllOptionsInOneFile() {
        initAppContext("/TestFilenameBasedFilterParser-allConfigOptions.xml");
        assertNumberOfBeansWithType(ExceptionFilter.class, 4);

        List<FilenameBasedExceptionFilter> filters = getAllMatchingBeans(FilenameBasedExceptionFilter.class);

        assertContainsFilter(filters, "regex-only-.*\\.jar", null);
        assertContainsFilter(filters, "regex-and-exception.*\\.jar", SuspiciousFileException.class);
        assertContainsFilter(filters, "regex-and-list-of-exceptions-.*\\.jar", SuspiciousFileException.class);
        assertContainsFilter(filters, "regex-and-list-of-exceptions-.*\\.jar", TestingException.class);
    }

    private void assertFilter(FilenameBasedExceptionFilter filter, String filenameRegex, Class<? extends Exception> exceptionType) {
        assertNotNull("Provided filter is null!", filter);
        FilenameBasedExceptionFilter expectedFilter = new FilenameBasedExceptionFilter(filenameRegex, exceptionType);
        assertEquals(filter, expectedFilter);
    }

    protected void assertContainsFilter(List<FilenameBasedExceptionFilter> filters, String filenameRegex, Class<? extends Exception> exceptionType) {
        FilenameBasedExceptionFilter expectedFilter = new FilenameBasedExceptionFilter(filenameRegex, exceptionType);
        for (FilenameBasedExceptionFilter filter : filters) {
            if (filter.equals(expectedFilter)) {
                return;
            }
        }
        fail("Filter with filename regex='" + filenameRegex + "' and exception type='" +
                exceptionType.getCanonicalName() + "' not found!");
    }

}
