package org.jboss.wolf.validator.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.jboss.wolf.validator.ValidatorContext;
import org.jboss.wolf.validator.impl.bom.BomDependencyNotFoundException;
import org.jboss.wolf.validator.impl.checksum.ChecksumNotExistException;
import org.jboss.wolf.validator.impl.suspicious.SuspiciousFileException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestSurefireXmlReporter {

    private static File TMP_TEST_DIR = new File("target/surefire-reporter-tests-tmp");
    private static File VALIDATED_REPO_DIR = new File(TMP_TEST_DIR, "workspace/validated-repository");
    private static File REPORTS_DIR = new File(TMP_TEST_DIR, "workspace/surefire-reports");

    @BeforeClass
    public static void createDirs() throws IOException {
        FileUtils.forceMkdir(VALIDATED_REPO_DIR);
        FileUtils.forceMkdir(REPORTS_DIR);
    }

    @Before
    public void cleanDirs() throws IOException {
        // make sure there are not some leftovers in the dirs that could cause tests to fail (or behave unexpectedly)
        FileUtils.cleanDirectory(VALIDATED_REPO_DIR);
        FileUtils.cleanDirectory(REPORTS_DIR);
    }

    @Test
    public void shouldCreateSurefireXmlReports() throws IOException {
        File fooFile = new File(VALIDATED_REPO_DIR, "foo");
        File barFile = new File(VALIDATED_REPO_DIR, "bar");

        ValidatorContext ctx = new ValidatorContext(VALIDATED_REPO_DIR, null, Collections.<RemoteRepository> emptyList());
        ctx.addException(fooFile, new SuspiciousFileException(fooFile, "suspicious because foo"));
        ctx.addException(barFile, new SuspiciousFileException(barFile, "suspicious because bar"));
        ctx.addException(fooFile, new ChecksumNotExistException(fooFile, "sha1"));
        ctx.addException(barFile, new ChecksumNotExistException(barFile, "sha1"));

        SurefireXmlReporter reporter = new SurefireXmlReporter(REPORTS_DIR);
        reporter.report(ctx);

        File suspiciousFileExceptionReportFile = new File(REPORTS_DIR, "TEST-SuspiciousFileException.xml");
        File checksumNotExistExceptionReportFile = new File(REPORTS_DIR, "TEST-ChecksumNotExistException.xml");

        assertTrue(suspiciousFileExceptionReportFile.exists());
        assertTrue(suspiciousFileExceptionReportFile.isFile());

        assertTrue(checksumNotExistExceptionReportFile.exists());
        assertTrue(checksumNotExistExceptionReportFile.isFile());

        String suspiciousFileExceptionReport = FileUtils.readFileToString(suspiciousFileExceptionReportFile);
        String checksumNotExistExceptionReport = FileUtils.readFileToString(checksumNotExistExceptionReportFile);

        assertTrue(suspiciousFileExceptionReport.contains("<testsuite name=\"SuspiciousFileException\""));
        assertTrue(suspiciousFileExceptionReport.contains(
                "<testcase name=\"__File " + TMP_TEST_DIR.getPath() + "/workspace/validated-repository/foo is suspicious because foo\" classname=\"SuspiciousFileException\""));
        assertTrue(suspiciousFileExceptionReport.contains(
                "<testcase name=\"__File " + TMP_TEST_DIR.getPath() + "/workspace/validated-repository/bar is suspicious because bar\" classname=\"SuspiciousFileException\""));

        assertTrue(checksumNotExistExceptionReport.contains("<testsuite name=\"ChecksumNotExistException\""));
        assertTrue(checksumNotExistExceptionReport.contains(
                "<testcase name=\"__Checksum sha1 for file " + TMP_TEST_DIR.getPath() + "/workspace/validated-repository/foo not exist\" classname=\"ChecksumNotExistException\""));
        assertTrue(checksumNotExistExceptionReport.contains(
                "<testcase name=\"__Checksum sha1 for file " + TMP_TEST_DIR.getPath() + "/workspace/validated-repository/bar not exist\" classname=\"ChecksumNotExistException\""));
    }

    @Test
    public void shouldNotCrashWhenExceptionHasNullMessage() {
        File f1 = new File(VALIDATED_REPO_DIR, "f1");
        File f2 = new File(VALIDATED_REPO_DIR, "f2");

        ValidatorContext ctx = new ValidatorContext(VALIDATED_REPO_DIR, null, Collections.<RemoteRepository> emptyList());
        ctx.addException(f1, new Exception((String)null));
        ctx.addException(f2, new Exception((String)null));

        SurefireXmlReporter reporter = new SurefireXmlReporter(REPORTS_DIR);
        reporter.report(ctx);

        File exceptionReportFile = new File(REPORTS_DIR, "TEST-Exception.xml");

        assertTrue(exceptionReportFile.exists());
        assertTrue(exceptionReportFile.isFile());
    }

    @Test
    public void shouldSquashMultipleDependencyNotFoundExceptions() throws IOException {
        ValidatorContext ctx = new ValidatorContext(VALIDATED_REPO_DIR, null, Collections.<RemoteRepository>emptyList());
        File validatedFile1 = new File(VALIDATED_REPO_DIR, "validated1");
        Artifact validated1 = new DefaultArtifact("org", "validated1", "pom", "1.0");
        Artifact missing = new DefaultArtifact("org", "missing", "jar", "1.0");
        DependencyNode depRoot1 = new DefaultDependencyNode(validated1);
        DependencyNotFoundException ex1 = new DependencyNotFoundException(new Exception(), missing, validated1, depRoot1);
        ctx.addException(validatedFile1, ex1);

        // second exception without provided dependency node (default one should be created under the hood)
        File validatedFile2 = new File(VALIDATED_REPO_DIR, "validated2");
        Artifact validated2 = new DefaultArtifact("org", "validated2", "pom", "1.0");
        DependencyNotFoundException ex2 = new DependencyNotFoundException(new Exception(), missing, validated2);
        ctx.addException(validatedFile2, ex2);

        SurefireXmlReporter reporter = new SurefireXmlReporter(REPORTS_DIR);
        reporter.report(ctx);

        File depNotFoundReportFile = new File(REPORTS_DIR, "TEST-DependencyNotFoundReport.xml");
        assertTrue(depNotFoundReportFile.exists());
        assertTrue(depNotFoundReportFile.isFile());

        String depNotFoundReportContent = FileUtils.readFileToString(depNotFoundReportFile);
        assertTrue(depNotFoundReportContent.contains("Miss org:missing:jar:1.0 in ...\n\n" +
                        "org:validated1:pom:1.0 (path org:validated1:pom:1.0)\n\n" +
                        "org:validated2:pom:1.0 (path org:validated2:pom:1.0)"
        ));

        assertFalse(new File(REPORTS_DIR, "TEST-DependencyNotFoundException.xml").exists());
    }

    @Test
    public void shouldSquashMultipleBomDependencyNotFoundExceptions() throws IOException {
        ValidatorContext ctx = new ValidatorContext(VALIDATED_REPO_DIR, null, Collections.<RemoteRepository>emptyList());
        File validatedFile1 = new File(VALIDATED_REPO_DIR, "validated1");
        Artifact validated1 = new DefaultArtifact("org", "validated1", "pom", "1.0");
        Artifact missing = new DefaultArtifact("org", "missing", "jar", "1.0");
        DependencyNode depRoot1 = new DefaultDependencyNode(validated1);
        BomDependencyNotFoundException ex1 = new BomDependencyNotFoundException(new Exception(), missing, validated1, depRoot1);
        ctx.addException(validatedFile1, ex1);

        File validatedFile2 = new File(VALIDATED_REPO_DIR, "validated2");
        Artifact validated2 = new DefaultArtifact("org", "validated2", "pom", "1.0");
        DependencyNode depRoot2 = new DefaultDependencyNode(validated2);
        BomDependencyNotFoundException ex2 = new BomDependencyNotFoundException(new Exception(), missing, validated2, depRoot2);
        ctx.addException(validatedFile2, ex2);

        SurefireXmlReporter reporter = new SurefireXmlReporter(REPORTS_DIR);
        reporter.report(ctx);

        File depNotFoundReportFile = new File(REPORTS_DIR, "TEST-BomDependencyNotFoundReport.xml");
        assertTrue(depNotFoundReportFile.exists());
        assertTrue(depNotFoundReportFile.isFile());

        String depNotFoundReportContent = FileUtils.readFileToString(depNotFoundReportFile);
        assertTrue(depNotFoundReportContent.contains("Miss org:missing:jar:1.0 in ...\n\n" +
                        "org:validated1:pom:1.0 (path org:validated1:pom:1.0)\n\n" +
                        "org:validated2:pom:1.0 (path org:validated2:pom:1.0)"
        ));

        assertFalse(new File(REPORTS_DIR, "TEST-DependencyNotFoundException.xml").exists());
    }

}