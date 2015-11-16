package com.redhat.repository.validator.impl.remoterepository;

import static com.redhat.repository.validator.internal.Utils.calculateChecksum;

import java.io.File;
import java.net.URI;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

public class ChecksumProviderAkamai implements ChecksumProvider {

    @Override
    public String getRemoteArtifactChecksum(URI remoteArtifact, HttpResponse httpResponse) {
        Header etagHeader = httpResponse.getFirstHeader("ETag");
        if (etagHeader != null) {
            String etagValue = etagHeader.getValue();
            if (etagValue != null) {
                int index = etagValue.indexOf(":");
                if (index != -1) {
                    return etagValue.substring(1, index);
                }
            }
        }
        throw new ChecksumProviderException("Remote repository returned unknown headers, it is not possible to parse artifact hash for " + remoteArtifact);
    }

    @Override
    public String getLocalArtifactChecksum(URI localArtifact) {
        return calculateChecksum(new File(localArtifact), "md5");
    }

}

/*

SAMPLE HTTP RESPONSE ...
 
Server : Apache
ETag : "7368fd4e4d4b437d895d6c650084b9b0:1372794403"
Last-Modified : Fri, 26 Apr 2013 15:24:38 GMT
Accept-Ranges : bytes
Content-Length : 4601483
Content-Type : text/plain
Date : Mon, 18 Aug 2014 11:45:26 GMT
Connection : keep-alive
 
*/