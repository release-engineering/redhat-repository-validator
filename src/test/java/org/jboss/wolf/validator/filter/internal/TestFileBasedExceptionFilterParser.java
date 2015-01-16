package org.jboss.wolf.validator.filter.internal;

import org.jboss.wolf.validator.ExceptionFilter;
import org.jboss.wolf.validator.filter.FileBasedExceptionFilter;
import org.jboss.wolf.validator.impl.suspicious.SuspiciousFileException;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class TestFileBasedExceptionFilterParser extends AbstractExceptionFilterParserTest {

    @Test
    public void shouldParseConfigWithAllOptionsInOneFile() {
        initAppContext("/TestFileBasedExceptionFilterParser-allConfigOptions.xml");
        assertNumberOfBeansWithType(ExceptionFilter.class, 8);

        List<FileBasedExceptionFilter> filters = getAllMatchingBeans(FileBasedExceptionFilter.class);

        assertContainsFilter(filters, "regex-only-.*\\.jar", null, null, null);
        assertContainsFilter(filters, null, "/path/regex/only/.*\\.jar", null, null);
        assertContainsFilter(filters, "regex-and-exception.*\\.jar", null, SuspiciousFileException.class, "msg-regex");
        assertContainsFilter(filters, null, "/path/regex/and/exception/.*\\.jar", SuspiciousFileException.class, "msg-regex");
        assertContainsFilter(filters, "regex-and-list-of-exceptions-.*\\.jar", null, SuspiciousFileException.class, null);
        assertContainsFilter(filters, "regex-and-list-of-exceptions-.*\\.jar", null, TestingException.class, "msg-regex");
        assertContainsFilter(filters, null, "/path/regex/and/list/of/exceptions/.*\\.jar", SuspiciousFileException.class, "msg-regex1");
        assertContainsFilter(filters, null, "/path/regex/and/list/of/exceptions/.*\\.jar", TestingException.class, "msg-regex2");
    }

    private void assertFilter(FileBasedExceptionFilter filter, String filenameRegex, String filepathRegex,
                              Class<? extends Exception> exceptionType, String exceptionMsgRegex) {
        assertNotNull("Provided filter is null!", filter);
        FileBasedExceptionFilter expectedFilter = new FileBasedExceptionFilter(filenameRegex, filepathRegex,
                exceptionType, exceptionMsgRegex);
        assertEquals(filter, expectedFilter);
    }

    protected void assertContainsFilter(List<FileBasedExceptionFilter> filters, String filenameRegex, String filepathRegex,
                                        Class<? extends Exception> exceptionType, String exceptionMsgRegex) {
        FileBasedExceptionFilter expectedFilter = new FileBasedExceptionFilter(filenameRegex, filepathRegex,
                exceptionType, exceptionMsgRegex);
        for (FileBasedExceptionFilter filter : filters) {
            if (filter.equals(expectedFilter)) {
                return;
            }
        }
        fail("Filter with file name-regex='" + filenameRegex
                + "' and path-regex='" + filepathRegex + "' and exception type='"
                + (exceptionType != null ? exceptionType.getCanonicalName() : null)
                + "' and exception-msg-regex=" + exceptionMsgRegex + " not found!");
    }

}
