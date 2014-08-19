package org.jboss.wolf.validator.impl.remoterepository;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;

public abstract class RemoteRepositoryCompareStrategyAbstract implements RemoteRepositoryCompareStrategy {

    @Override
    public final void compareArtifact(HttpClient httpClient, URI localArtifact, URI remoteArtifact) throws RemoteRepositoryCompareException {
        try {
            HttpUriRequest httpRequest = RequestBuilder.head().setUri(remoteArtifact).build();
            HttpResponse httpResponse = httpClient.execute(httpRequest);
            int httpStatusCode = httpResponse.getStatusLine().getStatusCode();
            if (httpStatusCode == HttpStatus.SC_OK) {

                String remoteArtifactHash = getRemoteArtifactHash(remoteArtifact, httpResponse);
                if (remoteArtifactHash == null) {
                    throw new RemoteRepositoryCompareException("Remote repository returned unknown headers, it is not possible to parse artifact hash for " + remoteArtifact);
                }

                String localArtifactHash = getLocalArtifactHash(localArtifact);

                if (!equalsIgnoreCase(remoteArtifactHash, localArtifactHash)) {
                    throw new RemoteRepositoryCompareException("Remote repository contains different binary data for artifact " + remoteArtifact);
                }

            } else if (httpStatusCode == HttpStatus.SC_NOT_FOUND) {
                throw new RemoteRepositoryCompareException("Remote repository doesn't contains artifact " + remoteArtifact);
            } else {
                throw new RemoteRepositoryCompareException("Remote repository returned " + httpResponse.getStatusLine().toString() + " for artifact " + remoteArtifact);
            }
        } catch (IOException e) {
            throw new RemoteRepositoryCompareException("Remote repository request failed for artifact " + remoteArtifact, e);
        }
    }

    protected abstract String getRemoteArtifactHash(URI remoteArtifact, HttpResponse httpResponse);

    protected abstract String getLocalArtifactHash(URI localArtifact);

}