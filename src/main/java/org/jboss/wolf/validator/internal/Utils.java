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

import org.eclipse.aether.artifact.Artifact;
import org.jboss.wolf.validator.ValidatorContext;

public class Utils {
    
    public static File relativize(ValidatorContext ctx, File file) {
        URI relativePath = ctx.getValidatedRepository().toURI().relativize(file.toURI());
        return new File(relativePath.toString());
    }
    
    public static List<Artifact> sortArtifacts(Collection<Artifact> artifacts) {
        ArrayList<Artifact> result = new ArrayList<Artifact>(artifacts);
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
    
    public static List<Exception> sortExceptions(Collection<Exception> exceptions) {
        Set<Exception> uniqueExceptions = new HashSet<Exception>(exceptions);
        ArrayList<Exception> sortedExceptions = new ArrayList<Exception>(uniqueExceptions);
        Collections.sort(sortedExceptions, new Comparator<Exception>() {
            @Override
            public int compare(Exception e1, Exception e2) {
                String name1 = e1.getClass().getSimpleName();
                String name2 = e2.getClass().getSimpleName();
                return name1.compareTo(name2);
            }
        });
        return sortedExceptions;
    }

}