package org.jboss.wolf.validator.impl.bom;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.model.Model;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorContext;
import org.jboss.wolf.validator.internal.ValidatorSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class BomDependencyNotFoundValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(BomDependencyNotFoundValidator.class);

    @Inject @Named("bomDependencyNotFoundValidatorFilter")
    private IOFileFilter fileFilter;
    @Inject
    private RepositorySystem repositorySystem;
    @Inject
    private RepositorySystemSession repositorySystemSession;
    @Inject
    private ValidatorSupport validatorSupport;
    @Inject
    private BomFilter bomFilter;

    @Override
    public void validate(ValidatorContext ctx) {
        logger.debug("start...");
        List<Model> models = validatorSupport.resolveEffectiveModels(ctx, fileFilter);
        for (Model model : models) {
            if( bomFilter.isBom(model) ) {
                validateBomDependencies(ctx, model);
            }
        }
    }

    private void validateBomDependencies(ValidatorContext ctx, Model bom) {
        Artifact bomArtifact = new DefaultArtifact(bom.getGroupId(), bom.getArtifactId(), bom.getPackaging(), bom.getVersion());
        Dependency bomDependency = new Dependency(bomArtifact, JavaScopes.COMPILE);
        for (org.apache.maven.model.Dependency dependency : bom.getDependencyManagement().getDependencies()) {
            validateBomDependency(ctx, bom, bomDependency, validatorSupport.convert(dependency));
        }
    }

    private void validateBomDependency(ValidatorContext ctx, Model bom, Dependency bomDependency, Dependency dependency) {
        CollectRequest collectRequest = new CollectRequest(
                bomDependency,
                Collections.singletonList(dependency),
                ctx.getRemoteRepositories());

        DependencyRequest dependencyRequest = new DependencyRequest(
                collectRequest,
                DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE));

        try {
            repositorySystem.resolveDependencies(repositorySystemSession, dependencyRequest);
        } catch (DependencyResolutionException e) {
            Exception bomDependencyNotFoundException = new BomDependencyNotFoundException(bom.getPomFile(), e);
            ctx.addException(bom.getPomFile(), bomDependencyNotFoundException);
        }
    }

}