package org.jboss.wolf.validator.impl;

import static org.apache.commons.lang3.SystemUtils.LINE_SEPARATOR;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.maven.surefire.report.CategorizedReportEntry.reportEntry;
import static org.jboss.wolf.validator.internal.Utils.findPathToDependency;
import static org.jboss.wolf.validator.internal.Utils.sortArtifacts;
import static org.jboss.wolf.validator.internal.Utils.sortDependencyNodes;
import static org.jboss.wolf.validator.internal.Utils.sortExceptions;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.inject.Named;

import com.google.common.collect.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.surefire.report.ReportEntryType;
import org.apache.maven.plugin.surefire.report.StatelessXmlReporter;
import org.apache.maven.plugin.surefire.report.TestSetStats;
import org.apache.maven.plugin.surefire.report.WrappedReportEntry;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.SafeThrowable;
import org.apache.maven.surefire.report.StackTraceWriter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.jboss.wolf.validator.Reporter;
import org.jboss.wolf.validator.ValidatorContext;
import org.jboss.wolf.validator.impl.bom.BomAmbiguousVersionException;
import org.jboss.wolf.validator.impl.bom.BomDependencyNotFoundException;

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

            List<Exception> exceptions = sortExceptions(ctx.getExceptions());
            List<Exception> filteredExceptions = sortExceptions(ctx.getIgnoredExceptions());
            // in case no exceptions were reported, create dummy successful test suite,
            // fixes https://github.com/thradec/wolf-validator/issues/12
            if (exceptions.isEmpty() && filteredExceptions.isEmpty()) {
                TestSetStats testSuite = new TestSetStats(false, false);
                testSuite.testSucceeded(testCase("NoErrorsOrWarningsFound", ReportEntryType.success,
                        "No errors or warnings have been found during the validation.", null));
                reportTestSuite("NoErrorsOrWarningsFound", testSuite);
            }
            reportMissingDependencies("DependencyNotFoundReport", DependencyNotFoundException.class, exceptions, filteredExceptions);
            reportMissingDependencies("BomDependencyNotFoundReport", BomDependencyNotFoundException.class, exceptions, filteredExceptions);
            List<BomAmbiguousVersionException> bomAmbiguousVersionsExs = ctx.getExceptions(BomAmbiguousVersionException.class);
            reportAmbiguousDependencyVersionInBoms(bomAmbiguousVersionsExs);
            exceptions.removeAll(bomAmbiguousVersionsExs);
            reportExceptions(exceptions, filteredExceptions);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void reportMissingDependencies(String type, Class<? extends DependencyNotFoundException> exceptionType,
            List<Exception> exceptions, List<Exception> filteredExceptions) {
        ListMultimap<Artifact, DependencyNode> artifactNotFoundMap = collectDependencyNotFoundData(exceptions, exceptionType);
        ListMultimap<Artifact, DependencyNode> filteredArtifactNotFoundMap = collectDependencyNotFoundData(filteredExceptions, exceptionType);
        TestSetStats testSuite = new TestSetStats(false, false);
        reportMissingDependencies(type, artifactNotFoundMap, ReportEntryType.error, testSuite);
        reportMissingDependencies(type, filteredArtifactNotFoundMap, ReportEntryType.skipped, testSuite);
        reportTestSuite(type, testSuite);
    }

    private void reportMissingDependencies(String type, ListMultimap<Artifact, DependencyNode> artifactNotFoundMap,
            ReportEntryType reportEntryType, TestSetStats testSuite) {
        for (Artifact artifact : sortArtifacts(artifactNotFoundMap.keySet())) {
            List<DependencyNode> roots = sortDependencyNodes(artifactNotFoundMap.get(artifact));
            if (roots.size() == 1) {
                String msg = "Miss " + artifact + " in " + roots.get(0).getArtifact() + " (path " + findPathToDependency(artifact, roots.get(0)) + ")";
                reportTestCase(type, reportEntryType, msg, null, testSuite);
            } else {
                String msg = "Miss " + artifact + " in " + roots.size() + " artifacts ...";
                StringBuilder dsc = new StringBuilder();
                dsc.append("Miss " + artifact + " in ...");
                dsc.append(LINE_SEPARATOR).append(LINE_SEPARATOR);
                for (DependencyNode root : roots) {
                    dsc.append(root.getArtifact() + " (path " + findPathToDependency(artifact, root) + ")");
                    dsc.append(LINE_SEPARATOR).append(LINE_SEPARATOR);
                }
                reportTestCase(type, reportEntryType, msg, dsc.toString(), testSuite);
            }
        }
    }

    private void reportAmbiguousDependencyVersionInBoms(List<BomAmbiguousVersionException> exceptions) {
        TestSetStats testSuite = new TestSetStats(false, false);
        // the list may contain duplicates, because the exception is inserted for each BOM in the repo
        // e.g. if there are there boms declaring different version for some artifact, there will be three
        // identical exceptions in the list
        Set<BomAmbiguousVersionException> uniqueExceptions = new HashSet<BomAmbiguousVersionException>(exceptions);
        for (BomAmbiguousVersionException ex : uniqueExceptions) {
            String description = ex.getMessage() + "\n\n" + formatAmbiguousDependencies(ex.getAmbiguousDependencies());
            reportTestCase("BomAmbiguousVersionReport", ReportEntryType.error, ex.getMessage(), description, testSuite);
        }
        reportTestSuite("BomAmbiguousVersionReport", testSuite);
    }

    private String formatAmbiguousDependencies(List<Pair<Dependency, File>> ambiguousDependencies) {
        StringBuilder msgSb = new StringBuilder();
        for (Pair<Dependency, File> pair : ambiguousDependencies) {
            msgSb.append("BOM " + pair.getValue() + " defines version " + pair.getKey().getVersion());
            msgSb.append(LINE_SEPARATOR);
            msgSb.append(LINE_SEPARATOR);
        }
        return msgSb.toString();
    }

    private void reportExceptions(List<Exception> exceptionList, List<Exception> ignoredExceptions) {
        Multimap<Class<? extends Exception>, Exception> exceptionMultimap = LinkedListMultimap.create();
        for (Exception exception : exceptionList) {
            exceptionMultimap.put(exception.getClass(), exception);
        }
        for (Exception exception : ignoredExceptions) {
            exceptionMultimap.put(exception.getClass(), exception);
        }

        for (Class<? extends Exception> exceptionType : exceptionMultimap.keySet()) {
            TestSetStats testSuiteData = new TestSetStats(false, false);
            for (Exception exception : exceptionMultimap.get(exceptionType)) {
                if (ignoredExceptions.contains(exception)) {
                    testSuiteData.testSkipped(testCase(exception, ReportEntryType.skipped));
                } else {
                    testSuiteData.testError(testCase(exception, ReportEntryType.error));
                }
            }
            reportTestSuite(exceptionType.getSimpleName(), testSuiteData);
        }
    }


    private ListMultimap<Artifact, DependencyNode> collectDependencyNotFoundData(List<Exception> exceptionList,
            Class<? extends DependencyNotFoundException> exceptionType) {
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

    private void reportTestCase(String type, ReportEntryType reportEntryType, String message, String description, TestSetStats testSuite) {
        if (reportEntryType == ReportEntryType.error) {
            testSuite.testError(testCase(type, ReportEntryType.error, message, description == null ? null : description.toString()));
        } else {
            testSuite.testSkipped(testCase(type, ReportEntryType.skipped, message, description == null ? null : description.toString()));
        }
    }

    private void reportTestSuite(String testSuiteName, TestSetStats testSuiteData) {
        // report entries here are expected to be either "error", "skipped" or "success"
        if (testSuiteData.getReportEntries().size() > 0) {
            ReportEntryType reportEntryType;
            if (testSuiteData.getErrors() > 0) {
                reportEntryType = ReportEntryType.error;
            } else if (testSuiteData.getSkipped() > 0) {
                reportEntryType = ReportEntryType.skipped;
            } else {
                reportEntryType = ReportEntryType.success;
            }
            WrappedReportEntry testSuite = testSuite(testSuiteName, reportEntryType);
            StatelessXmlReporter reporter = new StatelessXmlReporter(reportsDirectory, null, false);
            reporter.testSetCompleted(testSuite, testSuiteData);
        }
    }

    private WrappedReportEntry testSuite(String testSuiteName, ReportEntryType reportEntryType) {
        ReportEntry reportEntry = reportEntry(testSuiteName, testSuiteName + testSuitePostfix, null, null, 0, null);
        return wrap(reportEntry, reportEntryType);
    }

    private WrappedReportEntry testCase(String type, ReportEntryType reportEntryType, String message, String description) {
        ReportEntry reportEntry = reportEntry(type, testCasePrefix + message, null, new InternalStackTraceWriter(description), 0, "");
        return wrap(reportEntry, reportEntryType);
    }

    private WrappedReportEntry testCase(Exception exception, ReportEntryType reportEntryType) {
        ReportEntry reportEntry = reportEntry(exception.getClass().getSimpleName(), testCasePrefix + exception.getMessage(), null, new InternalStackTraceWriter(exception), 0, "");
        return wrap(reportEntry, reportEntryType);
    }

    private WrappedReportEntry wrap(ReportEntry reportEntry, ReportEntryType reportEntryType) {
        return new WrappedReportEntry(reportEntry, reportEntryType, 0, null, null);
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