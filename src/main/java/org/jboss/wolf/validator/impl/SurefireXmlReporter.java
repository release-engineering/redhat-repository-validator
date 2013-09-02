package org.jboss.wolf.validator.impl;

import static org.apache.maven.surefire.report.CategorizedReportEntry.reportEntry;
import static org.jboss.wolf.validator.internal.Utils.findCause;
import static org.jboss.wolf.validator.internal.Utils.findPathToDependency;
import static org.jboss.wolf.validator.internal.Utils.sortArtifacts;
import static org.jboss.wolf.validator.internal.Utils.sortDependencyNodes;
import static org.jboss.wolf.validator.internal.Utils.sortExceptions;

import java.io.File;
import java.util.List;
import java.util.ListIterator;

import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.surefire.report.ReportEntryType;
import org.apache.maven.plugin.surefire.report.StatelessXmlReporter;
import org.apache.maven.plugin.surefire.report.TestSetStats;
import org.apache.maven.plugin.surefire.report.WrappedReportEntry;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.SafeThrowable;
import org.apache.maven.surefire.report.StackTraceWriter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.jboss.wolf.validator.Reporter;
import org.jboss.wolf.validator.ValidatorContext;
import org.jboss.wolf.validator.impl.bom.BomDependencyNotFoundException;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

@Named
public class SurefireXmlReporter implements Reporter {
    
    /*
     * Some tools which process surefire reports (eg. Jenkins) 
     * show test suite name and test case name together, 
     * so prefix or postfix should increase readability.
     * 
     * test suite name = exception/report type
     * test case name = exception/report message
     */

    private static final String DEFAULT_REPORTS_DIRECTORY = "workspace/surefire-reports";
    private static final String DEFAULT_TEST_SUITE_POSTFIX = "";
    private static final String DEFAULT_TEST_CASE_PREFIX = "__";

    private final File reportsDirectory;
    private final String testSuitePostfix;
    private final String testCasePrefix;

    public SurefireXmlReporter() {
        this(new File(DEFAULT_REPORTS_DIRECTORY));
    }

    public SurefireXmlReporter(File reportsDirectory) {
        this(reportsDirectory, DEFAULT_TEST_SUITE_POSTFIX, DEFAULT_TEST_CASE_PREFIX);
    }

    public SurefireXmlReporter(File reportsDirectory, String testSuitePostfix, String testCasePrefix) {
        this.reportsDirectory = reportsDirectory;
        this.testSuitePostfix = testSuitePostfix;
        this.testCasePrefix = testCasePrefix;
    }

    @Override
    public void report(ValidatorContext ctx) {
        try {
            FileUtils.forceMkdir(reportsDirectory);
            FileUtils.cleanDirectory(reportsDirectory);

            List<Exception> exceptionList = sortExceptions(ctx.getExceptions());
            reportDependencyNotFound(exceptionList);
            reportBomDependencyNotFound(exceptionList);
            reportExceptions(exceptionList);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void reportDependencyNotFound(List<Exception> exceptionList) {
        ListMultimap<Artifact, DependencyNode> artifactNotFoundMap = collectDependencyNotFoundData(exceptionList);

        TestSetStats testSuite = new TestSetStats(false, false);
        for (Artifact artifact : sortArtifacts(artifactNotFoundMap.keySet())) {
            for (DependencyNode root : sortDependencyNodes(artifactNotFoundMap.get(artifact))) {
                String msg = "Miss " + artifact + " in " + root.getArtifact() + "(path " + findPathToDependency(artifact, root) + ")";
                testSuite.testError(testCase("DependencyNotFoundReport", msg));
            }
        }
        reportTestSuite("DependencyNotFoundReport", testSuite);
    }

    private void reportBomDependencyNotFound(List<Exception> exceptionList) {
        ListMultimap<Artifact, DependencyNode> artifactNotFoundMap = collectBomDependencyNotFoundData(exceptionList);

        TestSetStats testSuite = new TestSetStats(false, false);
        for (Artifact artifact : sortArtifacts(artifactNotFoundMap.keySet())) {
            for (DependencyNode root : sortDependencyNodes(artifactNotFoundMap.get(artifact))) {
                String msg = "Miss " + artifact + " in " + root.getArtifact() + "(path " + findPathToDependency(artifact, root) + ")";
                testSuite.testError(testCase("BomDependencyNotFoundReport", msg));
            }
        }
        reportTestSuite("BomDependencyNotFoundReport", testSuite);
    }

    private void reportExceptions(List<Exception> exceptionList) {
        Multimap<Class<? extends Exception>, Exception> exceptionMultimap = LinkedListMultimap.create();
        for (Exception exception : exceptionList) {
            exceptionMultimap.put(exception.getClass(), exception);
        }

        for (Class<? extends Exception> exceptionType : exceptionMultimap.keySet()) {
            TestSetStats testSuiteData = new TestSetStats(false, false);
            for (Exception exception : exceptionMultimap.get(exceptionType)) {
                testSuiteData.testError(testCase(exception));
            }
            reportTestSuite(exceptionType.getSimpleName(), testSuiteData);
        }
    }

    private ListMultimap<Artifact, DependencyNode> collectDependencyNotFoundData(List<Exception> exceptionList) {
        ListMultimap<Artifact, DependencyNode> artifactNotFoundMap = ArrayListMultimap.create();

        ListIterator<Exception> exceptionIterator = exceptionList.listIterator();
        while (exceptionIterator.hasNext()) {
            Exception e = exceptionIterator.next();
            if (e instanceof DependencyCollectionException) {
                DependencyNode from = new DefaultDependencyNode(((DependencyCollectionException) e).getResult().getRequest().getRoot());
                if (collectMissingDependencies(artifactNotFoundMap, e, from)) {
                    exceptionIterator.remove();
                }
            }
            if (e instanceof DependencyResolutionException) {
                DependencyNode from = ((DependencyResolutionException) e).getResult().getRoot();
                if (collectMissingDependencies(artifactNotFoundMap, e, from)) {
                    exceptionIterator.remove();
                }
            }
        }
        return artifactNotFoundMap;
    }

    private ListMultimap<Artifact, DependencyNode> collectBomDependencyNotFoundData(List<Exception> exceptionList) {
        ListMultimap<Artifact, DependencyNode> artifactNotFoundMap = ArrayListMultimap.create();

        ListIterator<Exception> exceptionIterator = exceptionList.listIterator();
        while (exceptionIterator.hasNext()) {
            Exception e = exceptionIterator.next();
            if (e instanceof BomDependencyNotFoundException) {
                DependencyNode from = ((BomDependencyNotFoundException) e).getDependencyResolutionException().getResult().getRoot();
                if (collectMissingDependencies(artifactNotFoundMap, e, from)) {
                    exceptionIterator.remove();
                }
            }
        }

        return artifactNotFoundMap;
    }

    private boolean collectMissingDependencies(ListMultimap<Artifact, DependencyNode> artifactNotFoundMap, Exception e, DependencyNode from) {
        ArtifactResolutionException artifactResolutionException = findCause(e, ArtifactResolutionException.class);
        if (artifactResolutionException != null) {
            for (ArtifactResult artifactResult : artifactResolutionException.getResults()) {
                if (!artifactResult.isResolved()) {
                    artifactNotFoundMap.put(artifactResult.getRequest().getArtifact(), from);
                }
            }
        }
        return artifactResolutionException != null;
    }

    private void reportTestSuite(String testSuiteName, TestSetStats testSuiteData) {
        if (testSuiteData.getErrors() > 0) {
            WrappedReportEntry testSuite = testSuite(testSuiteName);
            StatelessXmlReporter reporter = new StatelessXmlReporter(reportsDirectory, null, false);
            reporter.testSetCompleted(testSuite, testSuiteData);
        }
    }

    private WrappedReportEntry testSuite(String testSuiteName) {
        ReportEntry reportEntry = reportEntry(testSuiteName, testSuiteName + testSuitePostfix, null, null, 0, null);
        return wrap(reportEntry);
    }

    private WrappedReportEntry testCase(String type, String message) {
        ReportEntry reportEntry = reportEntry(type, testCasePrefix + message, null, new InternalStackTraceWriter(), 0, "");
        return wrap(reportEntry);
    }

    private WrappedReportEntry testCase(Exception exception) {
        ReportEntry reportEntry = reportEntry(exception.getClass().getSimpleName(), testCasePrefix + exception.getMessage(), null, new InternalStackTraceWriter(exception), 0, "");
        return wrap(reportEntry);
    }

    private WrappedReportEntry wrap(ReportEntry reportEntry) {
        return new WrappedReportEntry(reportEntry, ReportEntryType.error, 0, null, null);
    }

    private static class InternalStackTraceWriter implements StackTraceWriter {

        private static final Exception NONE_EXCEPTION = new Exception("");

        private final Exception exception;

        private InternalStackTraceWriter() {
            this(NONE_EXCEPTION);
        }

        private InternalStackTraceWriter(Exception exception) {
            this.exception = exception;
        }

        @Override
        public String writeTraceToString() {
            return "";
        }

        @Override
        public String writeTrimmedTraceToString() {
            return "";
        }

        @Override
        public String smartTrimmedStackTrace() {
            return "";
        }

        @Override
        public SafeThrowable getThrowable() {
            return new SafeThrowable(exception);
        }

    }

}