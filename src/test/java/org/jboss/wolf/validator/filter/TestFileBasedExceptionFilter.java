package org.jboss.wolf.validator.filter;

import org.jboss.wolf.validator.impl.signature.JarSignatureVerificationException;
import org.junit.Test;

import java.io.File;

public class TestFileBasedExceptionFilter extends AbstractExceptionFilterTest {

    @Test
    public void shouldIgnoreMatchingFileAndMatchingException() {
        FilenameBasedExceptionFilter filter = new FilenameBasedExceptionFilter("filename.*", null, JarSignatureVerificationException.class);
        Exception ex = new JarSignatureVerificationException(new File("filename"), "some message");
        assertExceptionIgnored(filter, ex, new File("filename"));
        // simple test to verify the regex: "filename.*" will also capture the string "filename2"
        ex = new JarSignatureVerificationException(new File("filename2"), "Some message");
        assertExceptionIgnored(filter, ex, new File("filename2"));
    }

    @Test
    public void shouldIgnoreMatchingPathAndMatchingException() {
        FilenameBasedExceptionFilter filter = new FilenameBasedExceptionFilter(null, ".*/some/path/.*\\.jar", JarSignatureVerificationException.class);
        Exception ex = new JarSignatureVerificationException(new File("/some/path/x.jar"), "some message");
        assertExceptionIgnored(filter, ex, new File("/some/path/x.jar"));
        // simple test to verify the regex: ".*/some/path/.*\.jar" will also capture the string "another/some/path/anotherlib/anotherjar.jar"
        ex = new JarSignatureVerificationException(new File("another/some/path/anotherlib/anotherjar.jar"), "Some message");
        assertExceptionIgnored(filter, ex, new File("another/some/path/x.jar"));
    }

    @Test
    public void shouldNotIgnoreOnlyMatchingFile() {
        FilenameBasedExceptionFilter filter = new FilenameBasedExceptionFilter("filename.*", null, JarSignatureVerificationException.class);
        Exception ex = new RuntimeException("Some message");
        assertExceptionNotIgnored(filter, ex, new File("filename"));
    }

    @Test
    public void shouldNotIgnoreOnlyMatchingPath() {
        FilenameBasedExceptionFilter filter = new FilenameBasedExceptionFilter(null, ".*/some/path/.*\\.jar", JarSignatureVerificationException.class);
        Exception ex = new RuntimeException("Some message");
        assertExceptionNotIgnored(filter, ex, new File("/some/path/x.jar"));
    }

    @Test
    public void shouldNotIgnoreOnlyMatchingExceptionNotMatchingFilename() {
        FilenameBasedExceptionFilter filter = new FilenameBasedExceptionFilter("filename.*", null, JarSignatureVerificationException.class);
        Exception ex = new JarSignatureVerificationException(new File("foo"), "Some message");
        assertExceptionNotIgnored(filter, ex, new File("foo"));
    }

    @Test
    public void shouldNotIgnoreOnlyMatchingExceptionNotMatchingFilepath() {
        FilenameBasedExceptionFilter filter = new FilenameBasedExceptionFilter(null, ".*/some/path/.*\\.jar", JarSignatureVerificationException.class);
        Exception ex = new JarSignatureVerificationException(new File("/some/path/x.jar"), "Some message");
        assertExceptionNotIgnored(filter, ex, new File("/some/no/matching/path/x.jar"));
    }

}
