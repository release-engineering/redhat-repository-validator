package org.jboss.wolf.validator.impl.bom;

import static org.jboss.wolf.validator.internal.Utils.findCause;
import static org.jboss.wolf.validator.internal.Utils.relativize;

import java.util.Collections;
import java.util.Iterator;

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
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorContext;
import org.jboss.wolf.validator.internal.Utils;
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
        Iterator<Model> modelIterator = validatorSupport.effectiveModelIterator(ctx, fileFilter);
        while (modelIterator.hasNext()) {
            Model model = modelIterator.next();
            if (model != null) {
                if (bomFilter.isBom(model)) {
                    logger.trace("validating {}", relativize(ctx, model.getPomFile()));
                    validateBomDependencies(ctx, model);
                }
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
            DependencyNode dependencyNode = e.getResult().getRoot();
            Artifact validatedArtifact = new DefaultArtifact(bom.getGroupId(), bom.getArtifactId(), bom.getPackaging(),
                    bom.getVersion());
            for (Artifact missingArtifact : Utils.collectMissingArtifacts(findCause(e, ArtifactResolutionException.class))) {
                ctx.addError(this, bom.getPomFile(), new BomDependencyNotFoundException(e, missingArtifact, validatedArtifact,
                        dependencyNode));
            }
        }
    }

}