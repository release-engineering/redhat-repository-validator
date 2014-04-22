package org.jboss.wolf.validator.impl;

import static org.apache.commons.lang3.SystemUtils.LINE_SEPARATOR;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
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
import org.apache.commons.lang3.exception.ExceptionUtils;
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
        ListMultimap<Artifact, DependencyNode> artifactNotFoundMap = collectDependencyNotFoundData(exceptionList, DependencyNotFoundException.class);
        reportDependencyNotFound("DependencyNotFoundReport", artifactNotFoundMap);
    }

    private void reportBomDependencyNotFound(List<Exception> exceptionList) {
        ListMultimap<Artifact, DependencyNode> artifactNotFoundMap = collectDependencyNotFoundData(exceptionList, BomDependencyNotFoundException.class);
        reportDependencyNotFound("BomDependencyNotFoundReport", artifactNotFoundMap);
    }

    private void reportDependencyNotFound(String type, ListMultimap<Artifact, DependencyNode> artifactNotFoundMap) {
        TestSetStats testSuite = new TestSetStats(false, false);
        for (Artifact artifact : sortArtifacts(artifactNotFoundMap.keySet())) {
            List<DependencyNode> roots = sortDependencyNodes(artifactNotFoundMap.get(artifact));
            if (roots.size() == 1) {
                String msg = "Miss " + artifact + " in " + roots.get(0).getArtifact() + " (path " + findPathToDependency(artifact, roots.get(0)) + ")";
                testSuite.testError(testCase(type, msg, null));
            } else {
                String msg = "Miss " + artifact + " in " + roots.size() + " artifacts ...";
                StringBuilder dsc = new StringBuilder();
                dsc.append("Miss " + artifact + " in ...");
                dsc.append(LINE_SEPARATOR).append(LINE_SEPARATOR);
                for (DependencyNode root : roots) {
                    dsc.append(root.getArtifact() + " (path " + findPathToDependency(artifact, root) + ")");
                    dsc.append(LINE_SEPARATOR).append(LINE_SEPARATOR);
                }
                testSuite.testError(testCase(type, msg, dsc.toString()));
            }
        }
        reportTestSuite(type, testSuite);
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

    private ListMultimap<Artifact, DependencyNode> collectDependencyNotFoundData(List<Exception> exceptionList, Class<? extends DependencyNotFoundException> exceptionType) {
        ListMultimap<Artifact, DependencyNode> artifactNotFoundMap = ArrayListMultimap.create();

        ListIterator<Exception> exceptionIterator = exceptionList.listIterator();
        while (exceptionIterator.hasNext()) {
            Exception e = exceptionIterator.next();
            if (e.getClass().equals(exceptionType)) {
                DependencyNode from = exceptionType.cast(e).getDependencyNode();
                artifactNotFoundMap.put(exceptionType.cast(e).getMissingArtifact(), from);
                exceptionIterator.remove();
            }
        }
        return artifactNotFoundMap;
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

    private WrappedReportEntry testCase(String type, String message, String description) {
        ReportEntry reportEntry = reportEntry(type, testCasePrefix + message, null, new InternalStackTraceWriter(description), 0, "");
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

        private final String description;
        private final Exception exception;

        private InternalStackTraceWriter(String description) {
            this(description, NONE_EXCEPTION);
        }

        private InternalStackTraceWriter(Exception exception) {
            this(null, exception);
        }

        private InternalStackTraceWriter(String description, Exception exception) {
            this.description = description;
            this.exception = exception;
        }

        private String stackTrace() {
            if (description != null) {
                return description;
            } else {
                return exception.getMessage() != null ? "" : ExceptionUtils.getStackTrace(exception);
            }
        }

        @Override
        public String writeTraceToString() {
            return stackTrace();
        }

        @Override
        public String writeTrimmedTraceToString() {
            return stackTrace();
        }

        @Override
        public String smartTrimmedStackTrace() {
            return stackTrace();
        }

        @Override
        public SafeThrowable getThrowable() {
            return new InternalSafeThrowable(exception);
        }

    }

    private static class InternalSafeThrowable extends SafeThrowable {

        public InternalSafeThrowable(Throwable target) {
            super(target);
        }

        @Override
        public String getMessage() {
            return defaultIfNull(super.getMessage(), "");
        }

        @Override
        public String getLocalizedMessage() {
            return defaultIfNull(super.getLocalizedMessage(), "");
        }

    }

}