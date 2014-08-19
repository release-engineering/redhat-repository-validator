package org.jboss.wolf.validator.impl.remoterepository;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

public class TestRemoteRepositoryCompareStrategyArtifactory {
    
    @Test
    public void shouldParseHashFromHeaders() throws URISyntaxException, RemoteRepositoryCompareException {
        RemoteRepositoryCompareStrategyArtifactory strategyArtifactory = new RemoteRepositoryCompareStrategyArtifactory() {
            @Override
            protected String getLocalArtifactHash(URI localArtifact) {
                return "9362fc3d9d47eedf3e9cda1ebd304f69ca70e7af";
            }
        };

        HttpClient httpClient = HttpClients.createDefault();
        URI localArtifact = new URI("file:/nothing");
        URI remoteArtifact = new URI("http://repo.jfrog.org/artifactory/libs-releases-local/org/apache/wicket/wicket/1.3.0-beta2/wicket-1.3.0-beta2.jar");

        strategyArtifactory.compareArtifact(httpClient, localArtifact, remoteArtifact);
    }

}