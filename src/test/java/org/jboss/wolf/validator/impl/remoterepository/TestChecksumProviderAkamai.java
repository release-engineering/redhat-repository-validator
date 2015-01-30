package org.jboss.wolf.validator.impl.remoterepository;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

public class TestChecksumProviderAkamai {
    
    @Test
    public void shouldParseHashFromHeaders() throws Exception {
        ChecksumProviderAkamai providerAkamai = new ChecksumProviderAkamai();
        
        URI remoteArtifact = new URI("https://maven.repository.redhat.com/techpreview/all/org/hibernate/hibernate-core/4.2.0.Final-redhat-1/hibernate-core-4.2.0.Final-redhat-1.jar");
        
        HttpClient httpClient = HttpClients.createDefault();
        HttpUriRequest httpRequest = RequestBuilder.head().setUri(remoteArtifact).build();
        HttpResponse httpResponse = httpClient.execute(httpRequest);

        String remoteChecksum = providerAkamai.getRemoteArtifactChecksum(remoteArtifact, httpResponse);
        assertEquals("7368fd4e4d4b437d895d6c650084b9b0", remoteChecksum);
    }

}