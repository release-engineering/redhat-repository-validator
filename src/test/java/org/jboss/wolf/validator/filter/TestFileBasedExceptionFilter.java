package org.jboss.wolf.validator.filter;

import org.jboss.wolf.validator.impl.signature.JarSignatureVerificationException;
import org.junit.Test;

import java.io.File;

public class TestFileBasedExceptionFilter extends AbstractExceptionFilterTest {

    @Test
    public void shouldIgnoreMatchingFileAndMatchingException() {
        FilenameBasedExceptionFilter filter = new FilenameBasedExceptionFilter(JarSignatureVerificationException.class, "filename.*");
        Exception ex = new JarSignatureVerificationException(new File("filename"), "some message");
        assertExceptionIgnored(filter, ex, new File("filename"));
        // simple test to verify the regex: "filename.*" will also capture the string "filename2"
        ex = new JarSignatureVerificationException(new File("filename2"), "Some message");
        assertExceptionIgnored(filter, ex, new File("filename2"));
    }

    @Test
    public void shouldNotIgnoreOnlyMatchingFile() {
        FilenameBasedExceptionFilter filter = new FilenameBasedExceptionFilter(JarSignatureVerificationException.class, "filename.*");
        Exception ex = new RuntimeException("Some message");
        assertExceptionNotIgnored(filter, ex, new File("filename"));
    }

    @Test
    public void shouldNotIgnoreOnlyMatchingException() {
        FilenameBasedExceptionFilter filter = new FilenameBasedExceptionFilter(JarSignatureVerificationException.class, "filename.*");
        Exception ex = new JarSignatureVerificationException(new File("filename"), "Some message");
        assertExceptionNotIgnored(filter, ex, new File("foo"));
    }

}
