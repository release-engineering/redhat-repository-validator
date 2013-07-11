package org.jboss.wolf.validator.reporter;

import static org.jboss.wolf.validator.impl.TestUtil.pom;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class TestSimpleChecksumReporter extends AbstractReporterTest {

    private final File fooDir = new File(repoFooDir, "com/acme/foo/1.0");

    @Test
    public void shouldReportChechsum() throws IOException {
        pom().artifactId("foo").create(repoFooDir);

        FileUtils.deleteQuietly(new File(fooDir, "foo-1.0.jar.sha1"));
        FileUtils.deleteQuietly(new File(fooDir, "foo-1.0.jar.md5"));
        FileUtils.writeStringToFile(new File(fooDir, "foo-1.0.pom.sha1"), "checksum");
        FileUtils.writeStringToFile(new File(fooDir, "foo-1.0.pom.md5"), "checksum");

        validator.validate(ctx);
        reporter.report(ctx);

        assertReportContains("Found 2 missing checksums");
        assertReportContains("com/acme/foo/1.0/foo-1.0.jar not exist MD5");
        assertReportContains("com/acme/foo/1.0/foo-1.0.jar not exist SHA-1");
        assertReportContains("Found 2 not match checksums");
        assertReportContains("com/acme/foo/1.0/foo-1.0.pom not match MD5");
        assertReportContains("com/acme/foo/1.0/foo-1.0.pom not match SHA-1");
    }

}