package org.jboss.wolf.validator.filter.internal;

import org.jboss.wolf.validator.ExceptionFilter;
import org.jboss.wolf.validator.filter.FilenameBasedExceptionFilter;
import org.jboss.wolf.validator.impl.suspicious.SuspiciousFileException;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class TestFilenameBasedFilterParser extends AbstractExceptionFilterParserTest {

    @Test
    public void shouldParseConfigWithFilenameRegexOnly() {
        initAppContext("/TestFilenameBasedFilterParser-regexOnly.xml");
        assertNumberOfBeansWithType(ExceptionFilter.class, 1);

        FilenameBasedExceptionFilter filter = getFirstMatchingBean(FilenameBasedExceptionFilter.class);
        assertFilter(filter, "regex-only-.*\\.jar", null, null);
    }

    @Test
    public void shouldParseConfigWithFilenameRegexAndExceptionType() {
        initAppContext("/TestFilenameBasedFilterParser-regexAndOneException.xml");
        assertNumberOfBeansWithType(ExceptionFilter.class, 1);

        FilenameBasedExceptionFilter filter = getFirstMatchingBean(FilenameBasedExceptionFilter.class);
        assertFilter(filter, "regex-and-exception.*\\.jar", null, SuspiciousFileException.class);
    }

    @Test
    public void shouldParseConfigWithFilenameRegexAndListOfExceptionTypes() {
        initAppContext("/TestFilenameBasedFilterParser-regexAndListOfExceptions.xml");
        assertNumberOfBeansWithType(ExceptionFilter.class, 2);

        List<FilenameBasedExceptionFilter> filters = getAllMatchingBeans(FilenameBasedExceptionFilter.class);
        assertContainsFilter(filters, "regex-and-list-of-exceptions-.*\\.jar", null, SuspiciousFileException.class);
        assertContainsFilter(filters, "regex-and-list-of-exceptions-.*\\.jar", null, TestingException.class);
    }

    @Test
    public void shouldParseConfigWithAllOptionsInOneFile() {
        initAppContext("/TestFilenameBasedFilterParser-allConfigOptions.xml");
        assertNumberOfBeansWithType(ExceptionFilter.class, 10);

        List<FilenameBasedExceptionFilter> filters = getAllMatchingBeans(FilenameBasedExceptionFilter.class);

        assertContainsFilter(filters, "regex-only-.*\\.jar", null, null);
        assertContainsFilter(filters, "deprecated-regex-only-.*\\.jar", null, null);
        assertContainsFilter(filters, null, "/path/regex/only/.*\\.jar", null);
        assertContainsFilter(filters, "regex-and-exception.*\\.jar", null, SuspiciousFileException.class);
        assertContainsFilter(filters, "deprecated-regex-and-exception.*\\.jar", null, SuspiciousFileException.class);
        assertContainsFilter(filters, null, "/path/regex/and/exception/.*\\.jar", SuspiciousFileException.class);
        assertContainsFilter(filters, "regex-and-list-of-exceptions-.*\\.jar", null, SuspiciousFileException.class);
        assertContainsFilter(filters, "regex-and-list-of-exceptions-.*\\.jar", null, TestingException.class);
        assertContainsFilter(filters, null, "/path/regex/and/list/of/exceptions/.*\\.jar", SuspiciousFileException.class);
        assertContainsFilter(filters, null, "/path/regex/and/list/of/exceptions/.*\\.jar", TestingException.class);
    }

    private void assertFilter(FilenameBasedExceptionFilter filter, String filenameRegex, String filepathRegex, Class<? extends Exception> exceptionType) {
        assertNotNull("Provided filter is null!", filter);
        FilenameBasedExceptionFilter expectedFilter = new FilenameBasedExceptionFilter(filenameRegex, filepathRegex, exceptionType);
        assertEquals(filter, expectedFilter);
    }

    protected void assertContainsFilter(List<FilenameBasedExceptionFilter> filters, String filenameRegex, String filepathRegex, Class<? extends Exception> exceptionType) {
        FilenameBasedExceptionFilter expectedFilter = new FilenameBasedExceptionFilter(filenameRegex, filepathRegex, exceptionType);
        for (FilenameBasedExceptionFilter filter : filters) {
            if (filter.equals(expectedFilter)) {
                return;
            }
        }
        fail("Filter with filename name-regex='" + filenameRegex
				+ "' and path-regex='" + filepathRegex + "' and exception type='"
				+ (exceptionType != null ? exceptionType.getCanonicalName() : null) + "' not found!");
    }

}
