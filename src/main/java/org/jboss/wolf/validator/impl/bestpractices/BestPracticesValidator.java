package org.jboss.wolf.validator.impl.bestpractices;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.jboss.wolf.validator.internal.Utils.relativize;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingResult;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorContext;
import org.jboss.wolf.validator.internal.ValidatorSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class BestPracticesValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(BestPracticesValidator.class);

    @Inject
    @Named("bestPracticesValidatorFilter")
    private IOFileFilter fileFilter;
    @Inject
    private ValidatorSupport validatorSupport;

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
        if (!isEmpty(rawModel.getRepositories())) {
            error(ctx, effectiveModel, "contains <repositories> configuration");
        }
        if (!isEmpty(rawModel.getPluginRepositories())) {
            error(ctx, effectiveModel, "contains <pluginRepositories> configuration");
        }
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
        if (isEmpty(effectiveModel.getDescription())) {
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

    private void error(ValidatorContext ctx, Model model, String msg) {
        ctx.addException(model.getPomFile(), new BestPracticesException("Error: artifact " + model.getId() + " " + msg));
    }

    private void warning(ValidatorContext ctx, Model model, String msg) {
        ctx.addException(model.getPomFile(), new BestPracticesException("Warning: artifact " + model.getId() + " " + msg));
    }

}