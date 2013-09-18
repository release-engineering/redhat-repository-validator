package org.jboss.wolf.validator.internal;

import static org.apache.commons.io.FileUtils.iterateFiles;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.suffixFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.graph.Exclusion;
import org.jboss.wolf.validator.ValidatorContext;

@Named
public class ValidatorSupport {

    @Inject
    private ModelBuilder modelBuilder;
    @Inject
    private ModelBuildingRequest modelBuildingRequestTemplate;
    @Inject
    private ArtifactTypeRegistry artifactTypeRegistry;
    
    public static Collection<File> listPomFiles(File dir, IOFileFilter filter) {
        Collection<File> pomFiles = listFiles(dir, and(filter, suffixFileFilter(".pom")), trueFileFilter());
        return pomFiles;
    }
    
    public ModelBuildingResult buildModel(File pomFile) {
        ModelBuildingResult result = null;

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest(modelBuildingRequestTemplate);
        request.setPomFile(pomFile);
        request.setModelSource(new FileModelSource(pomFile));
        try {
            result = modelBuilder.build(request);
        } catch (ModelBuildingException e) {
            result = null;
        }
        
        return result;
    }
    
    public Iterator<Model> effectiveModelIterator(final ValidatorContext ctx, IOFileFilter filter) {
        final Iterator<File> fileIterator = iterateFiles(ctx.getValidatedRepository(), and(filter, suffixFileFilter(".pom")), trueFileFilter());
        final Iterator<Model> modelIterator = new Iterator<Model>() {
            
            @Override
            public boolean hasNext() {
                return fileIterator.hasNext();
            }
            
            @Override
            public Model next() {
                File file = fileIterator.next();
                return buildModel(file).getEffectiveModel();
            }
            
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
        
        return modelIterator;
    }
    
    // copy from DefaultArtifactDescriptorReader
    public org.eclipse.aether.graph.Dependency convert(org.apache.maven.model.Dependency dependency) {
        ArtifactType stereotype = artifactTypeRegistry.get(dependency.getType());
        if (stereotype == null) {
            stereotype = new DefaultArtifactType(dependency.getType());
        }

        Map<String, String> props = null;
        boolean system = dependency.getSystemPath() != null && dependency.getSystemPath().length() > 0;
        if (system) {
            props = Collections.singletonMap(ArtifactProperties.LOCAL_PATH, dependency.getSystemPath());
        }

        Artifact artifact = new DefaultArtifact(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getClassifier(),
                null,
                dependency.getVersion(),
                props,
                stereotype);

        List<Exclusion> exclusions = new ArrayList<Exclusion>(dependency.getExclusions().size());
        for (org.apache.maven.model.Exclusion exclusion : dependency.getExclusions()) {
            exclusions.add(new Exclusion(exclusion.getGroupId(), exclusion.getArtifactId(), "*", "*"));
        }

        org.eclipse.aether.graph.Dependency result = new org.eclipse.aether.graph.Dependency(
                artifact,
                dependency.getScope(),
                dependency.isOptional(),
                exclusions);

        return result;
    }    

}