package org.jboss.wolf.validator.impl.bom;

import static org.jboss.wolf.validator.internal.Utils.gav;
import static org.jboss.wolf.validator.internal.ValidatorSupport.listPomFiles;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorContext;
import org.jboss.wolf.validator.internal.ValidatorSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;

@Named
public class BomUnmanagedVersionValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(BomUnmanagedVersionValidator.class);

    @Inject @Named("bomUnmanagedVersionValidatorFilter")
    private IOFileFilter fileFilter;
    @Inject
    private BomFilter bomFilter;
    @Inject
    private ValidatorSupport validatorSupport;

    @Override
    public void validate(ValidatorContext ctx) {
        Map<String, File> projectGavToFileMap = Maps.newHashMap();
        ListMultimap<String, String> dependencyGavToBomGavMap = ArrayListMultimap.create();
        
        collectData(ctx, projectGavToFileMap, dependencyGavToBomGavMap);
        findUnmanagedVersions(ctx, projectGavToFileMap, dependencyGavToBomGavMap);
    }

    private void collectData(ValidatorContext ctx, Map<String, File> projectGavToFileMap, ListMultimap<String, String> dependencyGavToBomGavMap) {
        Collection<File> pomFiles = listPomFiles(ctx.getValidatedRepository(), fileFilter);
        for (File pomFile : pomFiles) {
            if (!ctx.getErrors(pomFile).isEmpty()) {
                logger.debug("skipping `{}`, because already contains exceptions", pomFile);
                continue;
            }

            Model model = validatorSupport.buildModel(pomFile).getEffectiveModel();
            if (model.getPackaging().equals("pom")) {
                if (bomFilter.isBom(model)) {
                    String bomGav = gav(model);
                    for (Dependency bomDependency : model.getDependencyManagement().getDependencies()) {
                        String dependencyGav = gav(bomDependency);
                        dependencyGavToBomGavMap.put(dependencyGav, bomGav);
                    }
                }
            } else if( model.getPackaging().equals("maven-plugin") || model.getPackaging().equals("maven-archetype") ) {
                // skip, maven plugins and archetypes are not managed in boms
            } else {
                String projectGav = gav(model);
                projectGavToFileMap.put(projectGav, model.getPomFile());
            }
        }
    }

    private void findUnmanagedVersions(ValidatorContext ctx, Map<String, File> projectGavToFileMap, ListMultimap<String, String> dependencyGavToBomGavMap) {
        for (String projectGav : projectGavToFileMap.keySet()) {
            List<String> bomGavList = dependencyGavToBomGavMap.get(projectGav);
            if (bomGavList.isEmpty()) {
                ctx.addError(this, projectGavToFileMap.get(projectGav), new BomUnmanagedVersionException(projectGav));
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("project `{}` is managed in boms: `{}`", projectGav, dependencyGavToBomGavMap.get(projectGav));
                }
            }
        }
    }

}