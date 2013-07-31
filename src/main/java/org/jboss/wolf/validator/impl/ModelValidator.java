package org.jboss.wolf.validator.impl;

import static org.jboss.wolf.validator.internal.Utils.relativize;
import static org.jboss.wolf.validator.internal.ValidatorSupport.listPomFiles;

import java.io.File;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

@Named
@Order(200)
public class ModelValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(ModelValidator.class);

    @Inject @Named("modelValidatorFilter")
    private IOFileFilter fileFilter;
    @Inject
    private ModelBuilder modelBuilder;
    @Inject
    private ModelBuildingRequest modelBuildingRequestTemplate;

    @Override
    public void validate(ValidatorContext ctx) {
        Collection<File> pomFiles = listPomFiles(ctx.getValidatedRepository(), fileFilter);
        for (File pomFile : pomFiles) {
            logger.trace("validating {}", relativize(ctx, pomFile));
            if (!ctx.getExceptions(pomFile).isEmpty()) {
                logger.debug("skipping `{}`, because already contains exceptions", relativize(ctx, pomFile));
                continue;
            }
            validate(ctx, pomFile);
        }
    }

    private void validate(ValidatorContext ctx, File pomFile) {
        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest(modelBuildingRequestTemplate);
        request.setPomFile(pomFile);
        request.setModelSource(new FileModelSource(pomFile));
        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0);
        try {
            modelBuilder.build(request);
        } catch (ModelBuildingException e) {
            ctx.addException(pomFile, e);
        }
    }

}