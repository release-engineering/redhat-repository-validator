package com.redhat.repository.validator.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.model.Dependency;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.redhat.repository.validator.ExceptionFilter;
import com.redhat.repository.validator.ValidatorContext;
import com.redhat.repository.validator.filter.BomDependencyNotFoundExceptionFilter;
import com.redhat.repository.validator.filter.DependencyNotFoundExceptionFilter;
import com.redhat.repository.validator.filter.FilenameBasedExceptionFilter;
import com.redhat.repository.validator.impl.DependencyNotFoundException;
import com.redhat.repository.validator.impl.SurefireXmlReporter;
import com.redhat.repository.validator.impl.bom.BomAmbiguousVersionException;
import com.redhat.repository.validator.impl.bom.BomDependencyNotFoundException;
import com.redhat.repository.validator.impl.checksum.ChecksumNotExistException;
import com.redhat.repository.validator.impl.source.JarSourcesVerificationException;
import com.redhat.repository.validator.impl.suspicious.SuspiciousFileException;

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

        ValidatorContext ctx = new ValidatorContext(VALIDATED_REPO_DIR, null, Collections.<RemoteRepository>emptyList());
        ctx.addError(null, fooFile, new SuspiciousFileException(fooFile, "suspicious because foo"));
        ctx.addError(null, barFile, new SuspiciousFileException(barFile, "suspicious because bar"));
        ctx.addError(null, fooFile, new ChecksumNotExistException(fooFile, "sha1"));
        ctx.addError(null, barFile, new ChecksumNotExistException(barFile, "sha1"));

        SurefireXmlReporter reporter = new SurefireXmlReporter(REPORTS_DIR);
        reporter.report(ctx);

        File suspiciousFileExceptionReportFile = new File(REPORTS_DIR, "TEST-SuspiciousFileException.xml");
        File checksumNotExistExceptionReportFile = new File(REPORTS_DIR, "TEST-ChecksumNotExistException.xml");

        assertFileExists(suspiciousFileExceptionReportFile);

        assertFileExists(checksumNotExistExceptionReportFile);

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

        ValidatorContext ctx = new ValidatorContext(VALIDATED_REPO_DIR, null, Collections.<RemoteRepository>emptyList());
        ctx.addError(null, f1, new Exception((String) null));
        ctx.addError(null, f2, new Exception((String) null));

        SurefireXmlReporter reporter = new SurefireXmlReporter(REPORTS_DIR);
        reporter.report(ctx);

        File exceptionReportFile = new File(REPORTS_DIR, "TEST-Exception.xml");

        assertFileExists(exceptionReportFile);
    }

    @Test
    public void shouldSquashMultipleDependencyNotFoundExceptions() throws IOException {
        ValidatorContext ctx = new ValidatorContext(VALIDATED_REPO_DIR, null, Collections.<RemoteRepository>emptyList());
        File validatedFile1 = new File(VALIDATED_REPO_DIR, "validated1");
        Artifact validated1 = new DefaultArtifact("org", "validated1", "pom", "1.0");
        Artifact missing = new DefaultArtifact("org", "missing", "jar", "1.0");
        DependencyNode depRoot1 = new DefaultDependencyNode(validated1);
        DependencyNotFoundException ex1 = new DependencyNotFoundException(new Exception(), missing, validated1, depRoot1);
        ctx.addError(null, validatedFile1, ex1);

        // second exception without provided dependency node (default one should be created under the hood)
        File validatedFile2 = new File(VALIDATED_REPO_DIR, "validated2");
        Artifact validated2 = new DefaultArtifact("org", "validated2", "pom", "1.0");
        DependencyNotFoundException ex2 = new DependencyNotFoundException(new Exception(), missing, validated2);
        ctx.addError(null, validatedFile2, ex2);

        SurefireXmlReporter reporter = new SurefireXmlReporter(REPORTS_DIR);
        reporter.report(ctx);

        File depNotFoundReportFile = new File(REPORTS_DIR, "TEST-DependencyNotFoundReport.xml");
        assertFileExists(depNotFoundReportFile);

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
        ctx.addError(null, validatedFile1, ex1);

        File validatedFile2 = new File(VALIDATED_REPO_DIR, "validated2");
        Artifact validated2 = new DefaultArtifact("org", "validated2", "pom", "1.0");
        DependencyNode depRoot2 = new DefaultDependencyNode(validated2);
        BomDependencyNotFoundException ex2 = new BomDependencyNotFoundException(new Exception(), missing, validated2, depRoot2);
        ctx.addError(null, validatedFile2, ex2);

        SurefireXmlReporter reporter = new SurefireXmlReporter(REPORTS_DIR);
        reporter.report(ctx);

        File depNotFoundReportFile = new File(REPORTS_DIR, "TEST-BomDependencyNotFoundReport.xml");
        assertFileExists(depNotFoundReportFile);

        String depNotFoundReportContent = FileUtils.readFileToString(depNotFoundReportFile);
        assertTrue(depNotFoundReportContent.contains("Miss org:missing:jar:1.0 in ...\n\n" +
                        "org:validated1:pom:1.0 (path org:validated1:pom:1.0)\n\n" +
                        "org:validated2:pom:1.0 (path org:validated2:pom:1.0)"
        ));

        assertFalse(new File(REPORTS_DIR, "TEST-DependencyNotFoundException.xml").exists());
    }

    @Test
    public void shouldReportSimpleFilteredExceptionsAsSkipped() throws IOException {
        ExceptionFilter filter = new FilenameBasedExceptionFilter(".*validated", JarSourcesVerificationException.class);
        ValidatorContext ctx = new ValidatorContext(VALIDATED_REPO_DIR, null, Collections.<RemoteRepository>emptyList(), Collections.singletonList(filter));
        File validatedFile = new File(VALIDATED_REPO_DIR, "validated");
        Exception filteredException = new JarSourcesVerificationException(validatedFile);

        ctx.addError(null, validatedFile, filteredException);
        SurefireXmlReporter reporter = new SurefireXmlReporter(REPORTS_DIR);
        reporter.report(ctx);

        File reportFile = new File(REPORTS_DIR, "TEST-JarSourcesVerificationException.xml");
        assertFileExists(reportFile);

        String reportContent = FileUtils.readFileToString(reportFile);
        assertTrue(reportContent.contains("classname=\"JarSourcesVerificationException\" time=\"0\">\n    <skipped type=\"\"></skipped>"));
    }

    @Test
    public void shouldReportFilteredDependencyNotFoundExceptionAsSkipped() throws IOException {
        String missingArtifactRegex = "org:missing:jar:1.0";
        String validatedArtifactRegex = "org:validated:pom:1.0";
        ExceptionFilter filter = new DependencyNotFoundExceptionFilter(missingArtifactRegex, validatedArtifactRegex);
        ValidatorContext ctx = new ValidatorContext(VALIDATED_REPO_DIR, null, Collections.<RemoteRepository>emptyList(), Collections.singletonList(filter));

        Artifact validatedArtifact1 = new DefaultArtifact("org", "validated", "pom", "1.0");
        Artifact missing = new DefaultArtifact("org", "missing", "jar", "1.0");
        DependencyNode depRoot1 = new DefaultDependencyNode(validatedArtifact1);
        DependencyNotFoundException ex1 = new DependencyNotFoundException(new Exception(), missing, validatedArtifact1, depRoot1);
        ctx.addError(null, new File(""), ex1);

        Artifact validatedArtifact2 = new DefaultArtifact("org", "validated2-not-filtered", "pom", "1.1");
        DependencyNotFoundException ex2 = new DependencyNotFoundException(new Exception(), missing, validatedArtifact2);
        ctx.addError(null, new File(""), ex2);

        assertEquals("Number of filtered exceptions", 1, ctx.getIgnoredExceptions().size());
        assertEquals("Number of non-filtered exceptions", 1, ctx.getExceptions().size());

        SurefireXmlReporter reporter = new SurefireXmlReporter(REPORTS_DIR);
        reporter.report(ctx);

        File depNotFoundReportFile = new File(REPORTS_DIR, "TEST-DependencyNotFoundReport.xml");
        assertFileExists(depNotFoundReportFile);

        String depNotFoundReportContent = FileUtils.readFileToString(depNotFoundReportFile);
        assertTrue(depNotFoundReportContent.contains("<testcase name=\"__Miss org:missing:jar:1.0 in org:validated2-not-filtered:pom:1.1 \"" +
                " classname=\"DependencyNotFoundReport\" time=\"0\">\n    <error type=\"path org\">path org:validated2-not-filtered:pom:1.1</error>"));
        assertTrue(depNotFoundReportContent.contains("<testcase name=\"__Miss org:missing:jar:1.0 in org:validated:pom:1.0 \"" +
                " classname=\"DependencyNotFoundReport\" time=\"0\">\n    <skipped type=\"path org\">path org:validated:pom:1.0</skipped>"));
    }

    @Test
    public void shouldReportFilteredBomDependencyNotFoundExceptionAsSkipped() throws IOException {
        String missingArtifactRegex = "org:missing:jar:1.0";
        String validatedArtifactRegex = "org:validated:pom:1.0";
        ExceptionFilter filter = new BomDependencyNotFoundExceptionFilter(missingArtifactRegex, validatedArtifactRegex);
        ValidatorContext ctx = new ValidatorContext(VALIDATED_REPO_DIR, null, Collections.<RemoteRepository>emptyList(), Collections.singletonList(filter));

        Artifact validatedArtifact1 = new DefaultArtifact("org", "validated", "pom", "1.0");
        Artifact missingArtifact = new DefaultArtifact("org", "missing", "jar", "1.0");
        DependencyNode depRoot1 = new DefaultDependencyNode(validatedArtifact1);
        DependencyNotFoundException ex1 = new BomDependencyNotFoundException(new Exception(), missingArtifact, validatedArtifact1, depRoot1);
        ctx.addError(null, new File(""), ex1);

        Artifact validatedArtifact2 = new DefaultArtifact("org", "validated2-not-filtered", "pom", "1.1");
        DependencyNode depRoot2 = new DefaultDependencyNode(validatedArtifact2);
        DependencyNotFoundException ex2 = new BomDependencyNotFoundException(new Exception(), missingArtifact, validatedArtifact2, depRoot2);
        ctx.addError(null, new File(""), ex2);

        assertEquals("Number of filtered exceptions", 1, ctx.getIgnoredExceptions().size());
        assertEquals("Number of non-filtered exceptions", 1, ctx.getExceptions().size());

        SurefireXmlReporter reporter = new SurefireXmlReporter(REPORTS_DIR);
        reporter.report(ctx);

        File depNotFoundReportFile = new File(REPORTS_DIR, "TEST-BomDependencyNotFoundReport.xml");
        assertFileExists(depNotFoundReportFile);

        String depNotFoundReportContent = FileUtils.readFileToString(depNotFoundReportFile);
        assertTrue(depNotFoundReportContent.contains("<testcase name=\"__Miss org:missing:jar:1.0 in org:validated2-not-filtered:pom:1.1 \"" +
                " classname=\"BomDependencyNotFoundReport\" time=\"0\">\n    <error type=\"path org\">path org:validated2-not-filtered:pom:1.1</error>"));
        assertTrue(depNotFoundReportContent.contains("<testcase name=\"__Miss org:missing:jar:1.0 in org:validated:pom:1.0 \"" +
                " classname=\"BomDependencyNotFoundReport\" time=\"0\">\n    <skipped type=\"path org\">path org:validated:pom:1.0</skipped>"));
    }

    @Test
    public void shouldReportDependenciesForBomAmbiguousVersion() throws IOException {
        ValidatorContext ctx = new ValidatorContext(VALIDATED_REPO_DIR, null, Collections.<RemoteRepository>emptyList());
        List<Pair<Dependency, File>> ambiguousDependencies = new ArrayList<Pair<Dependency, File>>();

        Dependency dep1 = new Dependency();
        dep1.setVersion("1.0.Final");
        ambiguousDependencies.add(Pair.of(dep1, new File("org/some-bom-0.1.pom")));

        Dependency dep2 = new Dependency();
        dep2.setVersion("2.0.Final");
        ambiguousDependencies.add(Pair.of(dep2, new File("org/some-other-bom-0.2.pom")));

        BomAmbiguousVersionException ex = new BomAmbiguousVersionException("com.acme:acme-finance:jar", ambiguousDependencies);
        // report two identical exceptions, they should be "squashed" into single surefire test case entry
        ctx.addError(null, new File("some-file-in-repo"), ex);
        ctx.addError(null, new File("other-file-in-repo"), ex);

        SurefireXmlReporter reporter = new SurefireXmlReporter(REPORTS_DIR);
        reporter.report(ctx);
        File reportFile = new File(REPORTS_DIR, "TEST-BomAmbiguousVersionReport.xml");
        assertFileExists(reportFile);

        String reportFileContent = FileUtils.readFileToString(reportFile);
        assertTrue(reportFileContent.contains(
                "<testcase name=\"__BOMs contain ambiguous version for dependency com.acme:acme-finance:jar\""));
        assertTrue(reportFileContent.contains(
                "BOMs contain ambiguous version for dependency com.acme:acme-finance:jar\n\n" +
                        "BOM org/some-bom-0.1.pom defines version 1.0.Final\n\n" +
                        "BOM org/some-other-bom-0.2.pom defines version 2.0.Final"));
        // only single testcase should be created as only one unique exception was reported
        assertEquals(1, StringUtils.countMatches(reportFileContent, "<testcase"));
    }

    @Test
    public void shouldCreateDummySuccessfulTestReportWhenNoExceptionsReported() {
        ValidatorContext ctx = new ValidatorContext(VALIDATED_REPO_DIR, null, Collections.<RemoteRepository>emptyList());
        SurefireXmlReporter reporter = new SurefireXmlReporter(REPORTS_DIR);
        reporter.report(ctx);
        File reportFile = new File(REPORTS_DIR, "TEST-NoErrorsOrWarningsFound.xml");
        assertFileExists(reportFile);
    }

    private void assertFileExists(File file) {
        assertTrue("File " + file + " is expected to exist!", file.exists());
        assertTrue("File " + file + " is expected to be regular file!", file.isFile());
    }

}