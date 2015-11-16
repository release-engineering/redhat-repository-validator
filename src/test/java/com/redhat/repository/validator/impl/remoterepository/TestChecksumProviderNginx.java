package com.redhat.repository.validator.impl.remoterepository;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import com.redhat.repository.validator.impl.remoterepository.ChecksumProviderNginx;

public class TestChecksumProviderNginx {

    @Test
    public void shouldParseHashFromHeaders() throws Exception {
        ChecksumProviderNginx providerNginx = new ChecksumProviderNginx();
        
        URI remoteArtifact = new URI("http://repo1.maven.org/maven2/org/hibernate/hibernate-core/4.3.6.Final/hibernate-core-4.3.6.Final.pom");
        
        HttpClient httpClient = HttpClients.createDefault();
        HttpUriRequest httpRequest = RequestBuilder.head().setUri(remoteArtifact).build();
        HttpResponse httpResponse = httpClient.execute(httpRequest);

        String remoteChecksum = providerNginx.getRemoteArtifactChecksum(remoteArtifact, httpResponse);
        assertEquals("53c7583f-d11", remoteChecksum);
    }

}