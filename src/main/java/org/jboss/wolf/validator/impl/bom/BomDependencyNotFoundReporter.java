package org.jboss.wolf.validator.impl.bom;

import java.io.PrintStream;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.jboss.wolf.validator.ValidatorContext;
import org.jboss.wolf.validator.impl.DependencyNotFoundReporter;
import org.springframework.core.annotation.Order;

import com.google.common.collect.ListMultimap;

@Named
@Order(200)
public class BomDependencyNotFoundReporter extends DependencyNotFoundReporter {

    @Inject @Named("bomDependencyNotFoundReporterStream")
    private PrintStream out;

    @Override
    protected void collectMissingDependencies(ValidatorContext ctx, ListMultimap<Artifact, DependencyNode> artifactNotFoundMap) {
        List<BomDependencyNotFoundException> bomDependencyNotFoundExceptions = ctx.getExceptions(BomDependencyNotFoundException.class);
        for (BomDependencyNotFoundException e : bomDependencyNotFoundExceptions) {
            DependencyNode from = e.getDependencyResolutionException().getResult().getRoot();
            collectMissingDependencies(artifactNotFoundMap, e, from);
            ctx.addProcessedException(e);
        }
    }
    
    @Override
    protected void printHeader(ListMultimap<Artifact, DependencyNode> artifactNotFoundMap) {
        out.println("--- BOM DEPENDENCY NOT FOUND REPORT ---");
        out.println("Found " + artifactNotFoundMap.keySet().size() + " missing BOM dependencies.");
    }

}