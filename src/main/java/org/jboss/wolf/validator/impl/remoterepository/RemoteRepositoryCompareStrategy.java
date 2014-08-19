package org.jboss.wolf.validator.impl.remoterepository;

import java.net.URI;

import org.apache.http.client.HttpClient;

public interface RemoteRepositoryCompareStrategy {
    
    void compareArtifact(HttpClient httpClient, URI localArtifact, URI remoteArtifact) throws RemoteRepositoryCompareException;

}