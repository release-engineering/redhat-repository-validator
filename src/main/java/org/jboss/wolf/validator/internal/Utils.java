package org.jboss.wolf.validator.internal;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

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
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.util.filter.PatternInclusionsDependencyFilter;
import org.eclipse.aether.util.graph.visitor.PathRecordingDependencyVisitor;
import org.jboss.wolf.validator.ValidatorContext;

public class Utils {
    
    public static String gav(Model model) {
        return model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion();
    }
    
    public static String gav(Dependency dependency) {
        return dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion();
    }
    
    public static File relativize(ValidatorContext ctx, File file) {
        URI relativePath = ctx.getValidatedRepository().toURI().relativize(file.toURI());
        return new File(relativePath.toString());
    }

    public static File relativizeFile(File parentDir, File file) {
        URI relativePath = parentDir.toURI().relativize(file.toURI());
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
                    String msg1 = defaultIfNull(e1.getMessage(), "");
                    String msg2 = defaultIfNull(e2.getMessage(), "");
                    result = msg1.compareTo(msg2);
                }

                return result;
            }
        });
        return sortedExceptions;
    }

    public static List<File> sortFiles(Collection<File> files){
        List<File> sorted = new ArrayList<File>(files);
        Collections.sort(sorted, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return f1.getPath().compareTo(f2.getPath());
            }
        });
        return sorted;
    }

    public static <T extends Exception> T findCause(Throwable e, Class<T> clazz) {
        int index = ExceptionUtils.indexOfThrowable(e, clazz);
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

    public static List<Artifact> collectMissingArtifacts(ArtifactResolutionException e) {
        List<Artifact> missingArtifacts = new ArrayList<Artifact>();
        for (ArtifactResult artifactResult : e.getResults()) {
            if (artifactResult.isMissing()) {
                missingArtifacts.add(artifactResult.getRequest().getArtifact());
            }
        }
        return missingArtifacts;
    }

}