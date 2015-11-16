package com.redhat.repository.validator.impl.remoterepository;

import java.net.URI;

import org.apache.http.HttpResponse;

public interface ChecksumProvider {

    String getRemoteArtifactChecksum(URI remoteArtifact, HttpResponse httpResponse);

    String getLocalArtifactChecksum(URI localArtifact);

}