package org.jboss.wolf.validator.impl;

import static org.jboss.wolf.validator.internal.Utils.sortArtifacts;

import java.io.PrintStream;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyResolutionException;
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
    public void report(ValidatorContext ctx) {
        ListMultimap<Artifact, Artifact> artifactNotFoundMap = ArrayListMultimap.create();
        
        List<DependencyCollectionException> dependencyCollectionExceptions = ctx.getExceptions(DependencyCollectionException.class);
        for (DependencyCollectionException e : dependencyCollectionExceptions) {
            Artifact from = e.getResult().getRequest().getRoot().getArtifact();
            findMissingDependencies(artifactNotFoundMap, e, from);
            ctx.addProcessedException(e);
        }

        List<DependencyResolutionException> dependencyResolutionExceptions = ctx.getExceptions(DependencyResolutionException.class);
        for (DependencyResolutionException e : dependencyResolutionExceptions) {
            Artifact from = e.getResult().getRoot().getDependency().getArtifact();
            findMissingDependencies(artifactNotFoundMap, e, from);
            ctx.addProcessedException(e);
        }

        if( !artifactNotFoundMap.isEmpty() ) {
            out.println("--- DEPENDENCY NOT FOUND REPORT ---");
            out.println("Found " + artifactNotFoundMap.keySet().size() + " missing dependencies.");
            for (Artifact artifact : sortArtifacts(artifactNotFoundMap.keySet())) {
                out.println(artifact);
                List<Artifact> roots = sortArtifacts(artifactNotFoundMap.get(artifact));
                for (Artifact root : roots) {
                    out.println("    in: " + root);
                }
            }
            out.println();
            out.flush();
        }
    }

    private void findMissingDependencies(ListMultimap<Artifact, Artifact> artifactNotFoundMap, Exception e, Artifact from) {
        ArtifactResolutionException artifactResolutionException = findCause(e, ArtifactResolutionException.class);
        for (ArtifactResult artifactResult : artifactResolutionException.getResults()) {
            if (!artifactResult.isResolved()) {
                artifactNotFoundMap.put(artifactResult.getRequest().getArtifact(), from);
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