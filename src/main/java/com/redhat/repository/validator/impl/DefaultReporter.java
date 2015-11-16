package com.redhat.repository.validator.impl;

import static com.redhat.repository.validator.internal.Utils.findPathToDependency;
import static com.redhat.repository.validator.internal.Utils.sortArtifacts;
import static com.redhat.repository.validator.internal.Utils.sortDependencyNodes;
import static com.redhat.repository.validator.internal.Utils.sortExceptions;
import static org.apache.commons.lang3.ObjectUtils.notEqual;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.redhat.repository.validator.Reporter;
import com.redhat.repository.validator.ValidatorContext;
import com.redhat.repository.validator.impl.bom.BomDependencyNotFoundException;
import com.redhat.repository.validator.internal.LogOutputStream;

@Named
public class DefaultReporter implements Reporter {

    public static enum Mode {
        
        ONE_FILE_FOR_ALL,
        ONE_FILE_PER_EXCEPTION_TYPE
        
    }
    
    private final Mode mode;
    
    public DefaultReporter() {
        this(Mode.ONE_FILE_FOR_ALL);
    }

    public DefaultReporter(Mode mode) {
        this.mode = mode;
    }

    @Override
    public void report(ValidatorContext ctx) {
        List<BomDependencyNotFoundException> bomDependencyNotFoundExceptions = new ArrayList<BomDependencyNotFoundException>();
        List<DependencyNotFoundException> dependencyNotFoundExceptions = new ArrayList<DependencyNotFoundException>();
        List<Exception> exceptions = new ArrayList<Exception>();

        for (Exception e : ctx.getExceptions()) {
            if (e instanceof BomDependencyNotFoundException) {
                bomDependencyNotFoundExceptions.add((BomDependencyNotFoundException) e);
            } else if (e instanceof DependencyNotFoundException) {
                dependencyNotFoundExceptions.add((DependencyNotFoundException) e);
            } else {
                exceptions.add(e);
            }
        }

        reportDependencyNotFoundExceptions(dependencyNotFoundExceptions);
        reportBomDependencyNotFoundExceptions(bomDependencyNotFoundExceptions);
        reportExceptions(exceptions);
    }

    private void reportDependencyNotFoundExceptions(List<DependencyNotFoundException> dependencyNotFoundExceptions) {
        ListMultimap<Artifact, DependencyNode> artifactNotFoundMap = collectMissingDependencies(dependencyNotFoundExceptions);
        if (artifactNotFoundMap.isEmpty()) {
            return;
        }
        try (PrintStream ps = openStream(DependencyNotFoundException.class)) {
            reportMissingDependencies(ps, artifactNotFoundMap, false);
        }
    }

    private void reportBomDependencyNotFoundExceptions(List<BomDependencyNotFoundException> bomDependencyNotFoundExceptions) {
        ListMultimap<Artifact, DependencyNode> artifactNotFoundMap = collectMissingDependencies(bomDependencyNotFoundExceptions);
        if (artifactNotFoundMap.isEmpty()) {
            return;
        }
        try (PrintStream ps = openStream(BomDependencyNotFoundException.class)) {
            reportMissingDependencies(ps, artifactNotFoundMap, true);
        }
    }
    
    private void reportMissingDependencies(PrintStream ps, ListMultimap<Artifact, DependencyNode> artifactNotFoundMap, boolean isBom) {
        ps.print("--- ");
        ps.print(isBom ? BomDependencyNotFoundException.class.getSimpleName() : DependencyNotFoundException.class.getSimpleName());
        ps.print(" (found " + artifactNotFoundMap.keySet().size() + " missing dependencies)");
        ps.println(" ---");
        
        for (Artifact artifact : sortArtifacts(artifactNotFoundMap.keySet())) {
            ps.println("miss: " + artifact);
            List<DependencyNode> roots = sortDependencyNodes(artifactNotFoundMap.get(artifact));
            for (DependencyNode root : roots) {
                ps.println("    from: " + root.getArtifact());
                String path = findPathToDependency(artifact, root);
                String simplePath = root.getArtifact() + " > " + artifact;
                if (isNotEmpty(path) && notEqual(path, simplePath)) {
                    ps.print("        path: ");
                    ps.print(path);
                    ps.println();
                }
            }
        }
        ps.println();
        ps.flush();
    }

    private ListMultimap<Artifact, DependencyNode> collectMissingDependencies(List<? extends DependencyNotFoundException> dependencyNotFoundExceptions) {
        ListMultimap<Artifact, DependencyNode> artifactNotFoundMap = ArrayListMultimap.create();
        for (DependencyNotFoundException e : dependencyNotFoundExceptions) {
            DependencyNode dependencyNode = e.getDependencyNode();
            Artifact missingArtifact = e.getMissingArtifact();
            artifactNotFoundMap.put(missingArtifact, dependencyNode);
        }
        return artifactNotFoundMap;
    }

    private void reportExceptions(List<Exception> exceptions) {
        exceptions = sortExceptions(exceptions);
        if (exceptions.isEmpty()) {
            return;
        }

        Multimap<Class<? extends Exception>, Exception> exceptionMultimap = LinkedListMultimap.create();
        for (Exception exception : exceptions) {
            exceptionMultimap.put(exception.getClass(), exception);
        }

        for (Class<? extends Exception> exceptionType : exceptionMultimap.keySet()) {
            try (PrintStream ps = openStream(exceptionType)) {
                ps.println("--- " + exceptionType.getSimpleName() + " (total count " + exceptionMultimap.get(exceptionType).size() + ") ---");
                for (Exception exception : exceptionMultimap.get(exceptionType)) {
                    reportException(ps, exception, 0);
                }
                ps.println();
                ps.flush();
            }
        }
    }

    private void reportException(PrintStream ps, Throwable e, int depth) {
        StringBuilder msg = new StringBuilder();
        if (depth > 0) {
            msg.append(StringUtils.repeat(" ", depth * 4));
            msg.append(e.getClass().getSimpleName());
            msg.append(" ");
        }

        if (e.getMessage() != null) {
            msg.append(e.getMessage());
        }
        if (e.getMessage() == null && e.getCause() == null) {
            msg.append(ExceptionUtils.getStackTrace(e));
            msg.append(SystemUtils.LINE_SEPARATOR);
        }

        ps.println(msg.toString());

        if (e.getCause() != null) {
            reportException(ps, e.getCause(), depth + 1);
        }
    }
    
    private PrintStream openStream(Class<? extends Exception> exceptionType) {
        String reportFileName;
        switch(mode) {
            case ONE_FILE_FOR_ALL:
                reportFileName = "workspace/report.txt";
                break;
            case ONE_FILE_PER_EXCEPTION_TYPE:
                reportFileName = "workspace/report-"+exceptionType.getSimpleName()+".txt";
                break;
            default:
                throw new IllegalArgumentException();
        }
        
        try {
            File reportFile = new File(reportFileName);
            FileUtils.forceMkdir(reportFile.getParentFile());
            FileUtils.touch(reportFile);
            FileOutputStream fileOutputStream = new FileOutputStream(reportFile, mode == Mode.ONE_FILE_FOR_ALL);
            LogOutputStream logOutputStream = new LogOutputStream(Reporter.class.getSimpleName(), fileOutputStream);
            PrintStream printStream = new PrintStream(logOutputStream);
            return printStream;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}