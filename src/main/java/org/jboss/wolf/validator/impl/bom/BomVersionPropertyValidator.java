package org.jboss.wolf.validator.impl.bom;

import static org.jboss.wolf.validator.internal.Utils.gav;
import static org.jboss.wolf.validator.internal.ValidatorSupport.listPomFiles;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

@Named
public class BomVersionPropertyValidator implements Validator {
    
    private static final Logger logger = LoggerFactory.getLogger(BomVersionPropertyValidator.class);
    
    @Inject @Named("bomVersionPropertyValidatorFilter")
    private IOFileFilter fileFilter;
    @Inject
    private BomFilter bomFilter;
    @Inject
    private ValidatorSupport validatorSupport;

    @Override
    public void validate(ValidatorContext ctx) {
        Collection<File> pomFiles = listPomFiles(ctx.getValidatedRepository(), fileFilter);
        for (File pomFile : pomFiles) {
            if (!ctx.getExceptions(pomFile).isEmpty()) {
                logger.debug("skipping `{}`, because already contains exceptions", pomFile);
                continue;
            }
            
            Model model = validatorSupport.buildModel(pomFile).getRawModel();
            if (model.getPackaging().equals("pom")) {
                if (bomFilter.isBom(model)) {
                    validateBomDependenciesVersionProperty(ctx, pomFile, model);
                }
            }        
        }
    }

    private void validateBomDependenciesVersionProperty(ValidatorContext ctx, File pomFile, Model model) {
        String bomGav = gav(model);
        
        List<String> bomDependenciesWithoutVersionProperty = new ArrayList<String>();
        for (Dependency bomDependency : model.getDependencyManagement().getDependencies()) {
            if (!(bomDependency.getVersion() != null && bomDependency.getVersion().startsWith("${") && bomDependency.getVersion().endsWith("}"))) {
                String bomDependencyGav = gav(bomDependency);
                bomDependenciesWithoutVersionProperty.add(bomDependencyGav);
            }                        
        }
        
        if( !bomDependenciesWithoutVersionProperty.isEmpty() ) {
            ctx.addException(pomFile, new BomVersionPropertyException(bomGav, bomDependenciesWithoutVersionProperty.toArray(new String[]{})));
        }
    }

}