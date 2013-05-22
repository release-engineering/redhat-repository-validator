package org.jboss.wolf.validator.impl.aether;

import java.io.File;

import javax.inject.Inject;

import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;

public class LocalRepositoryModelResolver implements ModelResolver {

    @Inject
    private RepositorySystemSession repositorySystemSession;

    @Override
    public ModelSource resolveModel(String groupId, String artifactId, String version) throws UnresolvableModelException {
        LocalRepository localRepo = repositorySystemSession.getLocalRepository();
        File localRepoDir = localRepo.getBasedir();

        StringBuilder pomPath = new StringBuilder();
        pomPath.append(groupId.replace('.', File.separatorChar));
        pomPath.append(File.separatorChar);
        pomPath.append(artifactId);
        pomPath.append(File.separatorChar);
        pomPath.append(version);
        pomPath.append(File.separatorChar);
        pomPath.append(artifactId);
        pomPath.append('-');
        pomPath.append(version);
        pomPath.append(".pom");

        File pomFile = new File(localRepoDir, pomPath.toString());
        if (pomFile.exists()) {
            return new FileModelSource(pomFile);
        } else {
            throw new UnresolvableModelException("POM does not exist in local repository: " + pomFile, groupId, artifactId, version);
        }
    }

    @Override
    public ModelResolver newCopy() {
        return this;
    }

    @Override
    public void addRepository(Repository repository) throws InvalidRepositoryException {
        throw new UnsupportedOperationException();
    }

}