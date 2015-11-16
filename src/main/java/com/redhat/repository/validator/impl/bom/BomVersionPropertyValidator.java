package com.redhat.repository.validator.impl.bom;

import static com.redhat.repository.validator.internal.Utils.gav;
import static com.redhat.repository.validator.internal.ValidatorSupport.listPomFiles;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.repository.validator.Validator;
import com.redhat.repository.validator.ValidatorContext;
import com.redhat.repository.validator.internal.ValidatorSupport;

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
            if (!ctx.getErrors(pomFile).isEmpty()) {
                logger.debug("skipping `{}`, because already contains exceptions", pomFile);
                continue;
            }
            
            ModelBuildingResult result = validatorSupport.buildModel(pomFile);
            Model rawModel = result.getRawModel();
            Model effectiveModel = result.getEffectiveModel();
            if (bomFilter.isBom(effectiveModel)) {
                validateBomDependenciesVersionProperty(ctx, pomFile, rawModel, effectiveModel);
            }
        }
    }

    private void validateBomDependenciesVersionProperty(ValidatorContext ctx, File pomFile, Model rawModel, Model effectiveModel) {
        String bomGav = gav(effectiveModel);
        
        List<String> bomDependenciesWithoutVersionProperty = new ArrayList<String>();
        if( rawModel.getDependencyManagement() != null && rawModel.getDependencyManagement().getDependencies() != null ) {
            for (Dependency bomDependency : rawModel.getDependencyManagement().getDependencies()) {
                if (!(bomDependency.getVersion() != null && bomDependency.getVersion().startsWith("${") && bomDependency.getVersion().endsWith("}"))) {
                    String bomDependencyGav = gav(bomDependency);
                    bomDependenciesWithoutVersionProperty.add(bomDependencyGav);
                }
            }
        }
        
        if( !bomDependenciesWithoutVersionProperty.isEmpty() ) {
            ctx.addError(this, pomFile, new BomVersionPropertyException(bomGav, bomDependenciesWithoutVersionProperty.toArray(new String[]{})));
        }
    }

}