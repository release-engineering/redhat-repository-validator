package org.jboss.wolf.validator.impl.bestpractices;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.jboss.wolf.validator.internal.Utils.relativize;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.ModelBuildingResult;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorContext;
import org.jboss.wolf.validator.internal.ValidatorSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// https://docs.sonatype.org/display/Repository/Central+Sync+Requirements
@Named
public class BestPracticesValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(BestPracticesValidator.class);

    @Inject
    @Named("bestPracticesValidatorFilter")
    private IOFileFilter fileFilter;
    @Inject
    private ValidatorSupport validatorSupport;
    
    private final String[] allowedRepositoriesUrl;
    private final String[] allowedPluginRepositoriesUrl;
    
    public BestPracticesValidator() {
        this(null, null);
    }
    
    public BestPracticesValidator(String[] allowedRepositoriesUrl, String[] allowedPluginRepositoriesUrl) {
        this.allowedRepositoriesUrl = allowedRepositoriesUrl;
        this.allowedPluginRepositoriesUrl = allowedPluginRepositoriesUrl;
    }

    @Override
    public void validate(ValidatorContext ctx) {
        Iterator<ModelBuildingResult> modelIterator = validatorSupport.modelIterator(ctx, fileFilter);
        while (modelIterator.hasNext()) {
            ModelBuildingResult modelResult = modelIterator.next();
            if (modelResult != null) {
                Model rawModel = modelResult.getRawModel();
                Model effectiveModel = modelResult.getEffectiveModel();
                if (rawModel != null && effectiveModel != null) {
                    logger.trace("validating {}", relativize(ctx, rawModel.getPomFile()));
                    validateBestPractices(ctx, rawModel, effectiveModel);
                }
            }
        }
    }

    private void validateBestPractices(ValidatorContext ctx, Model rawModel, Model effectiveModel) {
        validateRepositories(ctx, rawModel, effectiveModel);
        validatePluginRepositories(ctx, rawModel, effectiveModel);
        
        if (isEmpty(effectiveModel.getModelVersion())) {
            warning(ctx, effectiveModel, "doesn't contain <modelVersion>");
        }
        if (isEmpty(effectiveModel.getArtifactId())) {
            warning(ctx, effectiveModel, "doesn't contain <artifactId>");
        }
        if (isEmpty(effectiveModel.getGroupId())) {
            warning(ctx, effectiveModel, "doesn't contain <groupId>");
        }
        if (isEmpty(effectiveModel.getVersion())) {
            warning(ctx, effectiveModel, "doesn't contain <version>");
        }
        if (isEmpty(effectiveModel.getName())) {
            warning(ctx, effectiveModel, "doesn't contain <name>");
        }
        if (effectiveModel.getDescription() == null) { // empty description is allowed, see WOLF-69
            warning(ctx, effectiveModel, "doesn't contain <description>");
        }
        if (isEmpty(effectiveModel.getUrl())) {
            warning(ctx, effectiveModel, "doesn't contain <url>");
        }
        if (isEmpty(effectiveModel.getLicenses())) {
            warning(ctx, effectiveModel, "doesn't contain <licenses>");
        }
        if (isEmpty(effectiveModel.getDevelopers())) {
            warning(ctx, effectiveModel, "doesn't contain <developers>");
        }

        if (effectiveModel.getScm() == null) {
            warning(ctx, effectiveModel, "doesn't contain <scm>");
        } else {
            if (isEmpty(effectiveModel.getScm().getUrl())) {
                warning(ctx, effectiveModel, "doesn't contain <scm><url>");
            }
            if (isEmpty(effectiveModel.getScm().getConnection())) {
                warning(ctx, effectiveModel, "doesn't contain <scm><connection>");
            }
        }
    }

    private void validateRepositories(ValidatorContext ctx, Model rawModel, Model effectiveModel) {
        if (!isEmpty(rawModel.getRepositories())) {
            if (allowedRepositoriesUrl != null) {
                for (Repository r : effectiveModel.getRepositories()) {
                    if (r.getId() != null && r.getId().equals("central")) {
                        continue;
                    }
                    if (ArrayUtils.contains(allowedRepositoriesUrl, r.getUrl())) {
                        continue;
                    }
                    error(ctx, effectiveModel, "contains <repository> configuration with unallowed url " + r.getUrl());
                }
            }
            else {
                error(ctx, effectiveModel, "contains <repositories> configuration");
            }
        }
    }

    private void validatePluginRepositories(ValidatorContext ctx, Model rawModel, Model effectiveModel) {
        if (!isEmpty(rawModel.getPluginRepositories())) {
            if( allowedPluginRepositoriesUrl != null ) {
                for( Repository r : effectiveModel.getPluginRepositories()) {
                    if (r.getId() != null && r.getId().equals("central")) {
                        continue;
                    }
                    if (ArrayUtils.contains(allowedPluginRepositoriesUrl, r.getUrl())) {
                        continue;
                    }
                    error(ctx, effectiveModel, "contains <pluginRepository> configuration with unallowed url " + r.getUrl());
                }
            }
            else {
                error(ctx, effectiveModel, "contains <pluginRepositories> configuration");
            }
        }
    }

    private void error(ValidatorContext ctx, Model model, String msg) {
        ctx.addError(this, model.getPomFile(), new BestPracticesException("Error: artifact " + model.getId() + " " + msg));
    }

    private void warning(ValidatorContext ctx, Model model, String msg) {
        ctx.addError(this, model.getPomFile(), new BestPracticesException("Warning: artifact " + model.getId() + " " + msg));
    }

}