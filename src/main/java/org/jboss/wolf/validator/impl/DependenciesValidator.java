package org.jboss.wolf.validator.impl;

import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;
import static org.jboss.wolf.validator.internal.Utils.relativize;
import static org.jboss.wolf.validator.internal.ValidatorSupport.listPomFiles;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelReader;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorContext;
import org.jboss.wolf.validator.internal.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

@Named
@Order(100)
public class DependenciesValidator implements Validator {
    
    private static final Logger logger = LoggerFactory.getLogger(DependenciesValidator.class);

    @Inject @Named("dependenciesValidatorFilter")
    private IOFileFilter fileFilter;
    @Inject
    private ModelReader modelReader;
    @Inject
    private RepositorySystem repositorySystem;
    @Inject
    private RepositorySystemSession repositorySystemSession;

    @Override
    public void validate(ValidatorContext ctx) {
        Collection<File> pomFiles = listPomFiles(ctx.getValidatedRepository(), fileFilter);
        for (File pomFile : pomFiles) {
            logger.trace("validating {}", relativize(ctx, pomFile));
            validate(ctx, pomFile);
        }
    }

    private void validate(ValidatorContext ctx, File pomFile) {
        Artifact pomArtifact = parsePomArtifact(ctx.getValidatedRepository(), pomFile);
        Model pomModel = parsePomModel(ctx, pomFile);
        if (pomModel == null) {
            return;
        }
        if (!resolvePom(ctx, pomFile, pomArtifact)) {
            return;
        }
        if (!resolveArchive(ctx, pomFile, pomArtifact, pomModel)) {
            return;
        }
        if (!resolveDependencies(ctx, pomFile, pomArtifact)) {
            return;
        }
    }

    private Artifact parsePomArtifact(File repoDir, File pomFile) {
        String pomPath = pomFile.getAbsolutePath();
        pomPath = removeStart(pomPath, repoDir.getAbsolutePath());
        pomPath = removeStart(pomPath, "/");
        pomPath = removeEnd(pomPath, pomFile.getName());
        pomPath = removeEnd(pomPath, "/");

        String version = substringAfterLast(pomPath, "/");
        pomPath = substringBeforeLast(pomPath, "/");
        String artifactId = substringAfterLast(pomPath, "/");
        pomPath = substringBeforeLast(pomPath, "/");
        String groupId = pomPath.replace("/", ".");

        return new DefaultArtifact(groupId, artifactId, "pom", version);
    }

    private Model parsePomModel(ValidatorContext ctx, File pomFile) {
        try {
            return modelReader.read(pomFile, null);
        } catch (IOException e) {
            ctx.addError(this, pomFile, e);
            return null;
        }
    }

    private boolean resolvePom(ValidatorContext ctx, File pomFile, Artifact pomArtifact) {
        ArtifactRequest pomRequest = new ArtifactRequest();
        pomRequest.setArtifact(pomArtifact);
        pomRequest.setRepositories(ctx.getRemoteRepositories());
        try {
            repositorySystem.resolveArtifact(repositorySystemSession, pomRequest);
        } catch (ArtifactResolutionException e) {
            collectAndReportMissingArtifacts(ctx, e, pomFile, pomArtifact, new DefaultDependencyNode(pomArtifact));
            return false;
        }
        return true;
    }

    private boolean resolveArchive(ValidatorContext ctx, File pomFile, Artifact pomArtifact, Model model) {
        if (!model.getPackaging().equals("pom")) {

            ArtifactTypeRegistry artifactTypeRegistry = repositorySystemSession.getArtifactTypeRegistry();
            ArtifactType artifactType = artifactTypeRegistry.get(model.getPackaging());
            
            if (artifactType == null) {
                ctx.addError(this, pomFile, new UnknownArtifactTypeException(model.getPackaging(), relativize(ctx, pomFile)));
                return false;
            }

            Artifact archiveArtifact = new DefaultArtifact(
                    pomArtifact.getGroupId(),
                    pomArtifact.getArtifactId(),
                    artifactType.getClassifier(),
                    artifactType.getExtension(),
                    pomArtifact.getVersion());

            ArtifactRequest archiveRequest = new ArtifactRequest();
            archiveRequest.setArtifact(archiveArtifact);
            archiveRequest.setRepositories(ctx.getRemoteRepositories());

            try {
                repositorySystem.resolveArtifact(repositorySystemSession, archiveRequest);
            } catch (ArtifactResolutionException e) {
                collectAndReportMissingArtifacts(ctx, e, pomFile, pomArtifact, new DefaultDependencyNode(pomArtifact));
                return false;
            }
        }
        return true;
    }

    private boolean resolveDependencies(ValidatorContext ctx, File pomFile, Artifact pomArtifact) {
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(pomArtifact, JavaScopes.COMPILE));
        collectRequest.setRepositories(ctx.getRemoteRepositories());

        DependencyFilter dependencyFilter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);
        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, dependencyFilter);

        try {
            repositorySystem.collectDependencies(repositorySystemSession, collectRequest);
        } catch (DependencyCollectionException e) {
            DependencyNode rootDepNode = new DefaultDependencyNode(e.getResult().getRequest().getRoot());
            collectAndReportMissingArtifacts(ctx, e, pomFile, pomArtifact, rootDepNode);
            return false;
        }

        try {
            repositorySystem.resolveDependencies(repositorySystemSession, dependencyRequest);
        } catch (DependencyResolutionException e) {
            DependencyNode rootDepNode = e.getResult().getRoot();
            collectAndReportMissingArtifacts(ctx, e, pomFile, pomArtifact, rootDepNode);
            return false;
        }

        return true;
    }

    private void collectAndReportMissingArtifacts(ValidatorContext ctx, Exception e, File pomFile,
                                                  Artifact validatedArtifact, DependencyNode rootDepNode) {
        ArtifactResolutionException artifactException;
        if (e instanceof ArtifactResolutionException) {
            artifactException = (ArtifactResolutionException) e;
        } else {
            artifactException = Utils.findCause(e, ArtifactResolutionException.class);
        }
        for (Artifact missingArtifact : Utils.collectMissingArtifacts(artifactException)) {
            ctx.addError(this,
                    pomFile, new DependencyNotFoundException(artifactException, missingArtifact, validatedArtifact, rootDepNode));
        }
    }

}