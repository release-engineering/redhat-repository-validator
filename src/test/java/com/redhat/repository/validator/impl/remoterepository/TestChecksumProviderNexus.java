package com.redhat.repository.validator.impl.remoterepository;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import com.redhat.repository.validator.impl.remoterepository.ChecksumProviderNexus;

public class TestChecksumProviderNexus {

    @Test
    public void shouldParseHashFromHeaders() throws Exception {
        ChecksumProviderNexus providerNexus = new ChecksumProviderNexus();
        
        URI remoteArtifact = new URI("http://repository.jboss.org/nexus/content/groups/public/org/hibernate/hibernate-core/4.3.6.Final/hibernate-core-4.3.6.Final.jar");
        
        HttpClient httpClient = HttpClients.createDefault();
        HttpUriRequest httpRequest = RequestBuilder.head().setUri(remoteArtifact).build();
        HttpResponse httpResponse = httpClient.execute(httpRequest);

        String remoteChecksum = providerNexus.getRemoteArtifactChecksum(remoteArtifact, httpResponse);
        assertEquals("e6f1e89880e645c66ef9c30d60a68f7e26f3152d", remoteChecksum);
    }

}