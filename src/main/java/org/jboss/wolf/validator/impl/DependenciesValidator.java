package org.jboss.wolf.validator.impl;

import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;
import static org.jboss.wolf.validator.impl.Util.listPomFiles;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.inject.Inject;

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
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependenciesValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(DependenciesValidator.class);
    
    @Inject
    private IOFileFilter fileFilter;
    @Inject
    private ModelReader modelReader;
    @Inject
    private RepositorySystem repositorySystem;
    @Inject
    private RepositorySystemSession repositorySystemSession;

    @Override
    public void validate(ValidatorContext ctx) {
        logger.debug("start...");
        Collection<File> pomFiles = listPomFiles(ctx.getValidatedRepoDir(), fileFilter);
        for (File pomFile : pomFiles) {
            logger.debug("validate: {}", pomFile);
            validate(ctx, pomFile);
        }
    }

    private void validate(ValidatorContext ctx, File pomFile) {
        Artifact pomArtifact = parsePomArtifact(ctx.getValidatedRepoDir(), pomFile);
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
            ctx.addException(pomFile, e);
            return null;
        }
    }

    private boolean resolvePom(ValidatorContext ctx, File pomFile, Artifact pomArtifact) {
        ArtifactRequest pomRequest = new ArtifactRequest();
        pomRequest.setArtifact(pomArtifact);
        pomRequest.setRepositories(ctx.getRemoteRepos());
        try {
            repositorySystem.resolveArtifact(repositorySystemSession, pomRequest);
        } catch (ArtifactResolutionException e) {
            ctx.addException(pomFile, e);
            return false;
        }
        return true;
    }

    private boolean resolveArchive(ValidatorContext ctx, File pomFile, Artifact pomArtifact, Model model) {
        if (!model.getPackaging().equals("pom")) {
            
            ArtifactTypeRegistry artifactTypeRegistry = repositorySystemSession.getArtifactTypeRegistry();
            ArtifactType artifactType = artifactTypeRegistry.get(model.getPackaging());
            
            Artifact archiveArtifact = new DefaultArtifact(
                    pomArtifact.getGroupId(),
                    pomArtifact.getArtifactId(),
                    artifactType.getClassifier(),
                    artifactType.getExtension(),
                    pomArtifact.getVersion());
            
            ArtifactRequest archiveRequest = new ArtifactRequest();
            archiveRequest.setArtifact(archiveArtifact);
            archiveRequest.setRepositories(ctx.getRemoteRepos());
            
            try {
                repositorySystem.resolveArtifact(repositorySystemSession, archiveRequest);
            } catch (ArtifactResolutionException e) {
                ctx.addException(pomFile, e);
                return false;
            }
        }
        return true;
    }

    private boolean resolveDependencies(ValidatorContext ctx, File pomFile, Artifact pomArtifact) {
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(pomArtifact, JavaScopes.COMPILE));
        collectRequest.setRepositories(ctx.getRemoteRepos());
        
        DependencyFilter dependencyFilter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);
        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, dependencyFilter);

        try {
            repositorySystem.collectDependencies(repositorySystemSession, collectRequest);
        } catch (DependencyCollectionException e) {
            ctx.addException(pomFile, e);
            return false;
        }
        try {
            repositorySystem.resolveDependencies(repositorySystemSession, dependencyRequest);
        } catch (DependencyResolutionException e) {
            ctx.addException(pomFile, e);
            return false;
        }

        return true;
    }

}