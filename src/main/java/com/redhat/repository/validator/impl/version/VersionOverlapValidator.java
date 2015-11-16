package com.redhat.repository.validator.impl.version;

import static com.redhat.repository.validator.internal.Utils.relativize;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.model.Model;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.repository.validator.Validator;
import com.redhat.repository.validator.ValidatorContext;
import com.redhat.repository.validator.internal.ValidatorSupport;

@Named
public class VersionOverlapValidator implements Validator {
    
    private static final Logger logger = LoggerFactory.getLogger(VersionOverlapValidator.class);
    
    @Inject @Named("versionOverlapValidatorFilter")
    private IOFileFilter fileFilter;
    @Inject
    private ValidatorSupport validatorSupport;
    @Inject
    private RepositorySystem repositorySystem;
    @Inject
    private RepositorySystemSession repositorySystemSession;

    @Override
    public void validate(ValidatorContext ctx) {
        Iterator<Model> modelIterator = validatorSupport.effectiveModelIterator(ctx, fileFilter);
        while (modelIterator.hasNext()) {
            Model model = modelIterator.next();
            if (model != null) {
                logger.trace("validating {}", relativize(ctx, model.getPomFile()));
                validateVersionOverlap(ctx, model);
            }
        }
    }

    private void validateVersionOverlap(ValidatorContext ctx, Model model) {
        File tmpLocalRepository = createTempLocalRepository();
        RepositorySystemSession tmpSession = createTempSession(tmpLocalRepository);
        
        boolean first = true;
        for (RemoteRepository remoteRepository : ctx.getRemoteRepositories()) {
            if (first) {
                first = false;
                continue; // first remote repository is the validated repository, so we want to skip it
            }

            cleanTempLocalRepository(tmpLocalRepository);            
            
            ArtifactRequest request = new ArtifactRequest();
            request.setArtifact(new DefaultArtifact(model.getGroupId(), model.getArtifactId(), "pom", model.getVersion()));
            request.addRepository(remoteRepository);
            try {
                repositorySystem.resolveArtifact(tmpSession, request);
                ctx.addError(this, model.getPomFile(), new VersionOverlapException(model.getId(), remoteRepository));
            } catch (ArtifactResolutionException e) {
                // noop
            }
        }
        
        deleteTempLocalRepository(tmpLocalRepository);
    }

    private RepositorySystemSession createTempSession(File tmpLocalRepository) {
        DefaultRepositorySystemSession tempSession = new DefaultRepositorySystemSession(repositorySystemSession);
        LocalRepositoryManager tempLocalRepositoryManager = repositorySystem.newLocalRepositoryManager(tempSession, new LocalRepository(tmpLocalRepository));
        tempSession.setLocalRepositoryManager(tempLocalRepositoryManager);
        return tempSession;
    }

    private File createTempLocalRepository() {
        File tempLocalRepository = new File("workspace", "temp");
        try {
            FileUtils.forceMkdir(tempLocalRepository);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return tempLocalRepository;
    }

    private void cleanTempLocalRepository(File tempLocalRepository) {
        try {
            FileUtils.cleanDirectory(tempLocalRepository);
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
    }

    private void deleteTempLocalRepository(File tempLocalRepository) {
        try {
            FileUtils.deleteDirectory(tempLocalRepository);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}