package org.jboss.wolf.validator.impl;

import static org.apache.commons.lang3.ObjectUtils.notEqual;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.jboss.wolf.validator.internal.Utils.findPathToDependency;
import static org.jboss.wolf.validator.internal.Utils.sortArtifacts;
import static org.jboss.wolf.validator.internal.Utils.sortDependencyNodes;
import static org.jboss.wolf.validator.internal.Utils.sortExceptions;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.jboss.wolf.validator.Reporter;
import org.jboss.wolf.validator.ValidatorContext;
import org.jboss.wolf.validator.impl.bom.BomDependencyNotFoundException;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

@Named
public class DefaultReporter implements Reporter {

    @Inject
    @Named("defaultReporterStream")
    private PrintStream out;

    @Override
    public void report(ValidatorContext ctx) {
        List<Exception> exceptions = new ArrayList<Exception>();
        List<DependencyNotFoundException> dependencyNotFoundExceptions = new ArrayList<DependencyNotFoundException>();
        List<BomDependencyNotFoundException> bomDependencyNotFoundExceptions = new ArrayList<BomDependencyNotFoundException>();

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

    private void reportBomDependencyNotFoundExceptions(List<BomDependencyNotFoundException> bomDependencyNotFoundExceptions) {
        ListMultimap<Artifact, DependencyNode> artifactNotFoundMap = collectMissingDependencies(bomDependencyNotFoundExceptions);
        if (!artifactNotFoundMap.isEmpty()) {
            out.println("--- BOM DEPENDENCY NOT FOUND REPORT ---");
            out.println("Found " + artifactNotFoundMap.keySet().size() + " missing BOM dependencies.");
            reportMissingDependencies(artifactNotFoundMap);
            out.println();
            out.flush();
        }
    }

    private void reportDependencyNotFoundExceptions(List<DependencyNotFoundException> dependencyNotFoundExceptions) {
        ListMultimap<Artifact, DependencyNode> artifactNotFoundMap = collectMissingDependencies(dependencyNotFoundExceptions);
        if (!artifactNotFoundMap.isEmpty()) {
            out.println("--- DEPENDENCY NOT FOUND REPORT ---");
            out.println("Found " + artifactNotFoundMap.keySet().size() + " missing dependencies.");
            reportMissingDependencies(artifactNotFoundMap);
            out.println();
            out.flush();
        }
    }

    private void reportMissingDependencies(ListMultimap<Artifact, DependencyNode> artifactNotFoundMap) {
        for (Artifact artifact : sortArtifacts(artifactNotFoundMap.keySet())) {
            out.println("miss: " + artifact);
            List<DependencyNode> roots = sortDependencyNodes(artifactNotFoundMap.get(artifact));
            for (DependencyNode root : roots) {
                out.println("    from: " + root.getArtifact());
                String path = findPathToDependency(artifact, root);
                String simplePath = root.getArtifact() + " > " + artifact;
                if (isNotEmpty(path) && notEqual(path, simplePath)) {
                    out.print("        path: ");
                    out.print(path);
                    out.println();
                }

            }
        }
    }

    private ListMultimap<Artifact, DependencyNode> collectMissingDependencies(
            List<? extends DependencyNotFoundException> dependencyNotFoundExceptions) {
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

        out.println("--- EXCEPTIONS REPORT ---");
        for (Class<? extends Exception> exceptionType : exceptionMultimap.keySet()) {
            out.println();
            out.println(exceptionType.getSimpleName() + " (total count " + exceptionMultimap.get(exceptionType).size() + ")");
            for (Exception exception : exceptionMultimap.get(exceptionType)) {
                reportException(exception, 0);
            }
        }
        out.println();
        out.flush();
    }

    private void reportException(Throwable e, int depth) {
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

        out.println(msg.toString());

        if (e.getCause() != null) {
            reportException(e.getCause(), depth + 1);
        }
    }

}