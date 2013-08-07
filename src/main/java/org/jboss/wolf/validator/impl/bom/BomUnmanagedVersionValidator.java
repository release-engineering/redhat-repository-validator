package org.jboss.wolf.validator.impl.bom;

import static org.jboss.wolf.validator.internal.ValidatorSupport.listPomFiles;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class BomUnmanagedVersionValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(BomUnmanagedVersionValidator.class);

    @Inject @Named("bomUnmanagedVersionValidatorFilter")
    private IOFileFilter fileFilter;
    @Inject
    private ModelBuilder modelBuilder;
    @Inject
    private ModelBuildingRequest modelBuildingRequestTemplate;
    @Inject
    private BomFilter bomFilter;

    @Override
    public void validate(ValidatorContext ctx) {
        List<Model> boms = new ArrayList<Model>();
        List<Model> projects = new ArrayList<Model>();

        collectBomsAndProjects(ctx, boms, projects);
        findUnmanagedVersions(ctx, boms, projects);
    }

    private void collectBomsAndProjects(ValidatorContext ctx, List<Model> boms, List<Model> projects) {
        Collection<File> pomFiles = listPomFiles(ctx.getValidatedRepository(), fileFilter);
        for (File pomFile : pomFiles) {
            if (!ctx.getExceptions(pomFile).isEmpty()) {
                logger.debug("skipping `{}`, because already contains exceptions", pomFile);
                continue;
            }

            Model model = buildModel(pomFile);
            if (model.getPackaging().equals("pom")) {
                if (bomFilter.isBom(model)) {
                    boms.add(model);
                }
            } else if( model.getPackaging().equals("maven-plugin") || model.getPackaging().equals("maven-archetype") ) {
                // skip, maven plugins and archetypes are not managed in boms
            } else {
                projects.add(model);
            }
        }
    }

    private void findUnmanagedVersions(ValidatorContext ctx, List<Model> boms, List<Model> projects) {
        for (Model project : projects) {
            Model bom = findBom(boms, project);
            if( bom == null ) {
                ctx.addException(project.getPomFile(), new BomUnmanagedVersionException(project));
            } else {
                logger.debug("project `{}` managed in bom `{}`", project, bom);
            }
        }
    }

    private Model findBom(List<Model> boms, Model project) {
        for (Model bom : boms) {
            for (Dependency bomDependency : bom.getDependencyManagement().getDependencies()) {
                if (ObjectUtils.equals(project.getGroupId(), bomDependency.getGroupId()) &&
                        ObjectUtils.equals(project.getArtifactId(), bomDependency.getArtifactId()) &&
                        ObjectUtils.equals(project.getVersion(), bomDependency.getVersion())) {
                    return bom;
                }
            }
        }
        return null;
    }

    private Model buildModel(File pomFile) {
        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest(modelBuildingRequestTemplate);
        request.setPomFile(pomFile);
        request.setModelSource(new FileModelSource(pomFile));
        try {
            ModelBuildingResult result = modelBuilder.build(request);
            return result.getEffectiveModel();
        } catch (ModelBuildingException e) {
            throw new RuntimeException(e);
        }
    }

}