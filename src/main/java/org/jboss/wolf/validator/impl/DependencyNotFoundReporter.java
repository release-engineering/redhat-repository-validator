package org.jboss.wolf.validator.impl;

import static org.jboss.wolf.validator.internal.Utils.sortArtifacts;
import static org.jboss.wolf.validator.internal.Utils.sortDependencyNodes;

import java.io.PrintStream;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.filter.PatternInclusionsDependencyFilter;
import org.eclipse.aether.util.graph.visitor.PathRecordingDependencyVisitor;
import org.jboss.wolf.validator.Reporter;
import org.jboss.wolf.validator.ValidatorContext;
import org.springframework.core.annotation.Order;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

@Named
@Order(100)
public class DependencyNotFoundReporter implements Reporter {
    
    @Inject @Named("dependencyNotFoundReporterStream")
    private PrintStream out;

    @Override
    public final void report(ValidatorContext ctx) {
        ListMultimap<Artifact, DependencyNode> artifactNotFoundMap = ArrayListMultimap.create();
        collectMissingDependencies(ctx, artifactNotFoundMap);
        if (!artifactNotFoundMap.isEmpty()) {
            printHeader(artifactNotFoundMap);
            printMissingDependencies(artifactNotFoundMap);
            out.println();
            out.flush();
        }
    }

    protected void collectMissingDependencies(ValidatorContext ctx, ListMultimap<Artifact, DependencyNode> artifactNotFoundMap) {
        List<DependencyCollectionException> dependencyCollectionExceptions = ctx.getExceptions(DependencyCollectionException.class);
        for (DependencyCollectionException e : dependencyCollectionExceptions) {
            DependencyNode from = new DefaultDependencyNode(e.getResult().getRequest().getRoot());
            collectMissingDependencies(ctx, artifactNotFoundMap, e, from);
        }
    
        List<DependencyResolutionException> dependencyResolutionExceptions = ctx.getExceptions(DependencyResolutionException.class);
        for (DependencyResolutionException e : dependencyResolutionExceptions) {
            DependencyNode from = e.getResult().getRoot();
            collectMissingDependencies(ctx, artifactNotFoundMap, e, from);
        }
    }

    protected void collectMissingDependencies(ValidatorContext ctx, ListMultimap<Artifact, DependencyNode> artifactNotFoundMap, Exception e, DependencyNode from) {
        ArtifactResolutionException artifactResolutionException = findCause(e, ArtifactResolutionException.class);
        if (artifactResolutionException != null) {
            ctx.addProcessedException(e);
            for (ArtifactResult artifactResult : artifactResolutionException.getResults()) {
                if (!artifactResult.isResolved()) {
                    artifactNotFoundMap.put(artifactResult.getRequest().getArtifact(), from);
                }
            }
        }
    }

    protected void printHeader(ListMultimap<Artifact, DependencyNode> artifactNotFoundMap) {
        out.println("--- DEPENDENCY NOT FOUND REPORT ---");
        out.println("Found " + artifactNotFoundMap.keySet().size() + " missing dependencies.");
    }

    protected void printMissingDependencies(ListMultimap<Artifact, DependencyNode> artifactNotFoundMap) {
        for (Artifact artifact : sortArtifacts(artifactNotFoundMap.keySet())) {
            out.println("miss: " + artifact);
            List<DependencyNode> roots = sortDependencyNodes(artifactNotFoundMap.get(artifact));
            for (DependencyNode root : roots) {
                out.println("    from: " + root.getArtifact());
                PathRecordingDependencyVisitor pathVisitor = new PathRecordingDependencyVisitor(new PatternInclusionsDependencyFilter(artifact.toString()));
                root.accept(pathVisitor);
                List<List<DependencyNode>> paths = pathVisitor.getPaths();
                for (List<DependencyNode> path : paths) {
                    out.print("        path: ");
                    for (int i = 0; i < path.size(); i++) {
                        out.print(path.get(i).getArtifact());
                        if (i != path.size() - 1) {
                            out.print(" > ");
                        }
                    }
                    out.println();
                }
            }
        }
    }

    private <T extends Exception> T findCause(Throwable e, Class<T> clazz) {
        int index = ExceptionUtils.indexOfThrowable(e, ArtifactResolutionException.class);
        if (index != -1) {
            Throwable[] throwables = ExceptionUtils.getThrowables(e);
            return clazz.cast(throwables[index]);
        } else {
            return null;
        }
    }

}