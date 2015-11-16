package com.redhat.repository.validator.filter;

import com.redhat.repository.validator.ExceptionFilter;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class AbstractExceptionFilterTest {

    protected void assertExceptionIgnored(ExceptionFilter filter, Exception ex, File file) {
        assertTrue("Exception should be ignored, but it wasn't!", filter.shouldIgnore(ex, file));
    }

    protected void assertExceptionNotIgnored(ExceptionFilter filter, Exception ex, File file) {
        assertFalse("Exception should _not_ be ignored, but it was!", filter.shouldIgnore(ex, file));
    }

}
