package org.jboss.wolf.validator.internal;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.util.filter.PatternInclusionsDependencyFilter;
import org.eclipse.aether.util.graph.visitor.PathRecordingDependencyVisitor;
import org.jboss.wolf.validator.ValidatorContext;

public class Utils {
    
    public static File relativize(ValidatorContext ctx, File file) {
        URI relativePath = ctx.getValidatedRepository().toURI().relativize(file.toURI());
        return new File(relativePath.toString());
    }
    
    public static List<Artifact> sortArtifacts(Collection<Artifact> artifacts) {
        List<Artifact> result = new ArrayList<Artifact>(artifacts);
        Collections.sort(result, new Comparator<Artifact>() {
            @Override
            public int compare(Artifact a1, Artifact a2) {
                String gav1 = a1.toString();
                String gav2 = a2.toString();
                return gav1.compareTo(gav2);
            }
        });
        return result;
    }
    
    public static List<DependencyNode> sortDependencyNodes(Collection<DependencyNode> nodes) {
        List<DependencyNode> result = new ArrayList<DependencyNode>(nodes);
        Collections.sort(result, new Comparator<DependencyNode>() {
            @Override
            public int compare(DependencyNode n1, DependencyNode n2) {
                String gav1 = n1.getArtifact().toString();
                String gav2 = n2.getArtifact().toString();
                return gav1.compareTo(gav2);
            }
        });
        return result;
    }
    
    public static List<Exception> sortExceptions(Collection<Exception> exceptions) {
        Set<Exception> uniqueExceptions = new HashSet<Exception>(exceptions);
        ArrayList<Exception> sortedExceptions = new ArrayList<Exception>(uniqueExceptions);
        Collections.sort(sortedExceptions, new Comparator<Exception>() {
            @Override
            public int compare(Exception e1, Exception e2) {
                String name1 = e1.getClass().getSimpleName();
                String name2 = e2.getClass().getSimpleName();
                int result = name1.compareTo(name2);

                if (result == 0) {
                    String msg1 = e1.getMessage();
                    String msg2 = e2.getMessage();
                    result = msg1.compareTo(msg2);
                }

                return result;
            }
        });
        return sortedExceptions;
    }
    
    public static <T extends Exception> T findCause(Throwable e, Class<T> clazz) {
        int index = ExceptionUtils.indexOfThrowable(e, ArtifactResolutionException.class);
        if (index != -1) {
            Throwable[] throwables = ExceptionUtils.getThrowables(e);
            return clazz.cast(throwables[index]);
        } else {
            return null;
        }
    }
    
    public static String findPathToDependency(Artifact artifact, DependencyNode root) {
        StringBuilder pathBuilder = new StringBuilder();
        PathRecordingDependencyVisitor pathVisitor = new PathRecordingDependencyVisitor(new PatternInclusionsDependencyFilter(artifact.toString()));
        root.accept(pathVisitor);
        List<List<DependencyNode>> paths = pathVisitor.getPaths();
        for (List<DependencyNode> path : paths) {
            for (int i = 0; i < path.size(); i++) {
                pathBuilder.append(path.get(i).getArtifact());
                if (i != path.size() - 1) {
                    pathBuilder.append(" > ");
                }
            }
        }
        return pathBuilder.toString();
    }

}