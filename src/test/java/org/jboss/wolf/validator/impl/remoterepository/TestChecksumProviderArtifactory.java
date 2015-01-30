package org.jboss.wolf.validator.impl.remoterepository;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

public class TestChecksumProviderArtifactory {
    
    @Test
    public void shouldParseHashFromHeaders() throws Exception {
        ChecksumProviderArtifactory providerArtifactory = new ChecksumProviderArtifactory();
        
        URI remoteArtifact = new URI("http://repo.jfrog.org/artifactory/libs-releases-local/org/apache/wicket/wicket/1.3.0-beta2/wicket-1.3.0-beta2.jar");
        
        HttpClient httpClient = HttpClients.createDefault();
        HttpUriRequest httpRequest = RequestBuilder.head().setUri(remoteArtifact).build();
        HttpResponse httpResponse = httpClient.execute(httpRequest);

        String remoteChecksum = providerArtifactory.getRemoteArtifactChecksum(remoteArtifact, httpResponse);
        assertEquals("9362fc3d9d47eedf3e9cda1ebd304f69ca70e7af", remoteChecksum);
    }

}