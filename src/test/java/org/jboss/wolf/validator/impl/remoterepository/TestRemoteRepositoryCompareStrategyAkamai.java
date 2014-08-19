package org.jboss.wolf.validator.impl.remoterepository;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

public class TestRemoteRepositoryCompareStrategyAkamai {
    
    @Test
    public void shouldParseHashFromHeaders() throws URISyntaxException, RemoteRepositoryCompareException {
        RemoteRepositoryCompareStrategyAkamai strategyAkamai = new RemoteRepositoryCompareStrategyAkamai() {
            @Override
            protected String getLocalArtifactHash(URI localArtifact) {
                return "7368fd4e4d4b437d895d6c650084b9b0";
            }
        };

        HttpClient httpClient = HttpClients.createDefault();
        URI localArtifact = new URI("file:/nothing");
        URI remoteArtifact = new URI("https://maven.repository.redhat.com/techpreview/all/org/hibernate/hibernate-core/4.2.0.Final-redhat-1/hibernate-core-4.2.0.Final-redhat-1.jar");

        strategyAkamai.compareArtifact(httpClient, localArtifact, remoteArtifact);
    }

}