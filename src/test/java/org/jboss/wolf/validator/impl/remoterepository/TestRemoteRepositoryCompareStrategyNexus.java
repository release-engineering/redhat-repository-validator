package org.jboss.wolf.validator.impl.remoterepository;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

public class TestRemoteRepositoryCompareStrategyNexus {

    @Test
    public void shouldParseHashFromHeaders() throws URISyntaxException, RemoteRepositoryCompareException {
        RemoteRepositoryCompareStrategyNexus strategyNexus = new RemoteRepositoryCompareStrategyNexus() {
            @Override
            protected String getLocalArtifactHash(URI localArtifact) {
                return "e6f1e89880e645c66ef9c30d60a68f7e26f3152d";
            }
        };

        HttpClient httpClient = HttpClients.createDefault();
        URI localArtifact = new URI("file:/nothing");
        URI remoteArtifact = new URI("http://repository.jboss.org/nexus/content/groups/public/org/hibernate/hibernate-core/4.3.6.Final/hibernate-core-4.3.6.Final.jar");

        strategyNexus.compareArtifact(httpClient, localArtifact, remoteArtifact);
    }

}