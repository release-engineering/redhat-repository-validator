package com.redhat.repository.validator.filter;

import org.junit.Test;

import com.redhat.repository.validator.filter.FilenameBasedExceptionFilter;
import com.redhat.repository.validator.impl.signature.JarSignatureVerificationException;

import java.io.File;

public class TestFilenameBasedExceptionFilter extends AbstractExceptionFilterTest {

    @Test
    public void shouldIgnoreMatchingFileAndMatchingException() {
        FilenameBasedExceptionFilter filter = new FilenameBasedExceptionFilter("filename.*", JarSignatureVerificationException.class);
        Exception ex = new JarSignatureVerificationException(new File("filename"), "some message");
        assertExceptionIgnored(filter, ex, new File("filename"));
        // simple test to verify the regex: "filename.*" will also capture the string "filename2"
        ex = new JarSignatureVerificationException(new File("filename2"), "Some message");
        assertExceptionIgnored(filter, ex, new File("filename2"));
    }

    @Test
    public void shouldNotIgnoreOnlyMatchingFile() {
        FilenameBasedExceptionFilter filter = new FilenameBasedExceptionFilter("filename.*", JarSignatureVerificationException.class);
        Exception ex = new RuntimeException("Some message");
        assertExceptionNotIgnored(filter, ex, new File("filename"));
    }

    @Test
    public void shouldNotIgnoreOnlyMatchingException() {
        FilenameBasedExceptionFilter filter = new FilenameBasedExceptionFilter("filename.*", JarSignatureVerificationException.class);
        Exception ex = new JarSignatureVerificationException(new File("filename"), "Some message");
        assertExceptionNotIgnored(filter, ex, new File("foo"));
    }

}
