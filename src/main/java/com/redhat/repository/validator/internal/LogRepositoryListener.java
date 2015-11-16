package com.redhat.repository.validator.internal;

import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//see project shrinkwrap-resolver, class org.jboss.shrinkwrap.resolver.impl.maven.logging.LogRepositoryListener
public class LogRepositoryListener extends AbstractRepositoryListener {

    private static final Logger logger = LoggerFactory.getLogger(LogRepositoryListener.class);

    @Override
    public void artifactDeployed(RepositoryEvent event) {
        logger.debug("Deployed " + event.getArtifact() + " to " + event.getRepository());
    }

    @Override
    public void artifactDeploying(RepositoryEvent event) {
        logger.trace("Deploying " + event.getArtifact() + " to " + event.getRepository());
    }

    @Override
    public void artifactDescriptorInvalid(RepositoryEvent event) {
        logger.warn("Invalid artifact descriptor for " + event.getArtifact() + ": " + event.getException().getMessage());
    }

    @Override
    public void artifactDescriptorMissing(RepositoryEvent event) {
        logger.warn("Missing artifact descriptor for " + event.getArtifact());
    }

    @Override
    public void artifactInstalled(RepositoryEvent event) {
        logger.debug("Installed " + event.getArtifact() + " to " + event.getFile());
    }

    @Override
    public void artifactInstalling(RepositoryEvent event) {
        logger.trace("Installing " + event.getArtifact() + " to " + event.getFile());
    }

    @Override
    public void artifactResolved(RepositoryEvent event) {
        logger.debug("Resolved artifact " + event.getArtifact() + " from " + event.getRepository());
    }

    @Override
    public void artifactResolving(RepositoryEvent event) {
        logger.trace("Resolving artifact " + event.getArtifact());
    }

    @Override
    public void metadataDeployed(RepositoryEvent event) {
        logger.debug("Deployed " + event.getMetadata() + " to " + event.getRepository());
    }

    @Override
    public void metadataDeploying(RepositoryEvent event) {
        logger.trace("Deploying " + event.getMetadata() + " to " + event.getRepository());
    }

    @Override
    public void metadataInstalled(RepositoryEvent event) {
        logger.debug("Installed " + event.getMetadata() + " to " + event.getFile());
    }

    @Override
    public void metadataInstalling(RepositoryEvent event) {
        logger.trace("Installing " + event.getMetadata() + " to " + event.getFile());
    }

    @Override
    public void metadataInvalid(RepositoryEvent event) {
        logger.warn("Invalid metadata " + event.getMetadata());
    }

    @Override
    public void metadataResolved(RepositoryEvent event) {
        logger.debug("Resolved metadata " + event.getMetadata() + " from " + event.getRepository());
    }

    @Override
    public void metadataResolving(RepositoryEvent event) {
        logger.trace("Resolving metadata " + event.getMetadata() + " from " + event.getRepository());
    }

}