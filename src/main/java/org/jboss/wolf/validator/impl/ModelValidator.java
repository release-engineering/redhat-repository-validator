package org.jboss.wolf.validator.impl;

import static org.jboss.wolf.validator.impl.Util.listPomFiles;

import java.io.File;
import java.util.Collection;
import java.util.Properties;

import javax.inject.Inject;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelSource;
import org.eclipse.aether.RepositorySystemSession;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorContext;
import org.jboss.wolf.validator.impl.aether.LocalRepositoryModelResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(ModelValidator.class);

    @Inject
    private IOFileFilter fileFilter;
    @Inject
    private ModelBuilder modelBuilder;
    @Inject
    private RepositorySystemSession repositorySystemSession;
    @Inject
    private LocalRepositoryModelResolver localRepositoryModelResolver; 

    @Override
    public void validate(ValidatorContext ctx) {
        logger.debug("start");
        Collection<File> pomFiles = listPomFiles(ctx.getValidatedRepoDir(), fileFilter);
        for (File pomFile : pomFiles) {
            if (!ctx.getExceptions(pomFile).isEmpty()) {
                logger.info("skipping `{}`, because already contains exceptions", pomFile);
                continue;
            }
            logger.debug("validate: {}", pomFile);
            validate(ctx, pomFile);
        }
    }

    private void validate(ValidatorContext ctx, File pomFile) {
        ModelSource modelSource = new FileModelSource(pomFile);
        
        Properties userProperties = new Properties();
        userProperties.putAll(repositorySystemSession.getUserProperties());
        
        Properties systemProperties = new Properties();
        systemProperties.putAll(repositorySystemSession.getSystemProperties());
        
        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelSource(modelSource);
        request.setProcessPlugins(true);
        request.setLocationTracking(true);
        request.setModelResolver(localRepositoryModelResolver);
        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0);
        request.setUserProperties(userProperties);
        request.setSystemProperties(systemProperties);

        try {
            modelBuilder.build(request);
        } catch (ModelBuildingException e) {
            ctx.addException(pomFile, e);
        }
    }

}