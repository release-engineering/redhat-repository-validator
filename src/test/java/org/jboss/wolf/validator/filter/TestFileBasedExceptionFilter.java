package org.jboss.wolf.validator.filter;

import org.jboss.wolf.validator.impl.signature.JarSignatureVerificationException;
import org.junit.Test;

import java.io.File;

public class TestFileBasedExceptionFilter extends AbstractExceptionFilterTest {

    @Test
    public void shouldIgnoreMatchingFileAndMatchingException() {
        FileBasedExceptionFilter filter = new FileBasedExceptionFilter("filename.*", null, JarSignatureVerificationException.class ,".*signature found.*");
        Exception ex = new JarSignatureVerificationException(new File("filename"), "foo signature found bar");
        assertExceptionIgnored(filter, ex, new File("filename"));
        // simple test to verify the regex: "filename.*" will also capture the string "filename2"
        ex = new JarSignatureVerificationException(new File("filename2"), "signature found");
        assertExceptionIgnored(filter, ex, new File("filename2"));
    }

    @Test
    public void shouldIgnoreMatchingPathAndMatchingException() {
        FileBasedExceptionFilter filter = new FileBasedExceptionFilter(null, ".*/some/path/.*\\.jar", JarSignatureVerificationException.class, null);
        Exception ex = new JarSignatureVerificationException(new File("/some/path/x.jar"), "some message");
        assertExceptionIgnored(filter, ex, new File("/some/path/x.jar"));
        // simple test to verify the regex: ".*/some/path/.*\.jar" will also capture the string "another/some/path/anotherlib/anotherjar.jar"
        ex = new JarSignatureVerificationException(new File("another/some/path/anotherlib/anotherjar.jar"), "Some message");
        assertExceptionIgnored(filter, ex, new File("another/some/path/x.jar"));
    }

    @Test
    public void shouldNotIgnoreOnlyMatchingFile() {
        final String matchingFilename = "filename";
        final String matchingMessage = "some message";

        //does not match exception
        FileBasedExceptionFilter filter = new FileBasedExceptionFilter(matchingFilename, null, JarSignatureVerificationException.class, matchingMessage);
        Exception ex = new RuntimeException(matchingMessage);
        assertExceptionNotIgnored(filter, ex, new File(matchingFilename));

        //does not match exception message
        ex = new JarSignatureVerificationException(new File(matchingFilename), "other message");
        assertExceptionNotIgnored(filter, ex, new File(matchingFilename));

        //match for check previous tests does not match for their defined reason
        ex = new JarSignatureVerificationException(new File(matchingFilename), matchingMessage);
        assertExceptionNotIgnored(filter, ex, new File(matchingFilename));
    }

    @Test
    public void shouldNotIgnoreOnlyMatchingPath() {
        final String matchingPath = "/some/path/filename";
        final String matchingMessage = "some message";

        //does not match exception message
        FileBasedExceptionFilter filter = new FileBasedExceptionFilter(null, matchingPath, JarSignatureVerificationException.class, matchingMessage);
        Exception ex = new RuntimeException(matchingMessage);
        assertExceptionNotIgnored(filter, ex, new File(matchingPath));

        //does not match exception message
        ex = new JarSignatureVerificationException(new File(matchingPath), "other message");
        assertExceptionNotIgnored(filter, ex, new File(matchingPath));

        //match for check previous tests does not match for their defined reason
        ex = new JarSignatureVerificationException(new File(matchingPath), matchingMessage);
        assertExceptionNotIgnored(filter, ex, new File(matchingPath));
    }

    @Test
    public void shouldNotIgnoreOnlyMatchingExceptionNotMatchingFilename() {
        FileBasedExceptionFilter filter = new FileBasedExceptionFilter("filename.*", null, JarSignatureVerificationException.class, null);
        Exception ex = new JarSignatureVerificationException(new File("foo"), "Some message");
        assertExceptionNotIgnored(filter, ex, new File("foo"));
    }

    @Test
    public void shouldNotIgnoreOnlyMatchingExceptionNotMatchingFilepath() {
        FileBasedExceptionFilter filter = new FileBasedExceptionFilter(null, ".*/some/path/.*\\.jar", JarSignatureVerificationException.class, null);
        Exception ex = new JarSignatureVerificationException(new File("/some/path/x.jar"), "Some message");
        assertExceptionNotIgnored(filter, ex, new File("/some/no/matching/path/x.jar"));
    }

}
