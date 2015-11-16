package com.redhat.repository.validator.impl.remoterepository;

import java.io.File;
import java.net.URI;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

public class ChecksumProviderNginx implements ChecksumProvider {

    @Override
    public String getRemoteArtifactChecksum(URI remoteArtifact, HttpResponse httpResponse) {
        Header etagHeader = httpResponse.getFirstHeader("ETag");
        if (etagHeader != null) {
            String etagValue = etagHeader.getValue();
            if( etagValue.startsWith("\"") && etagValue.endsWith("\"") ) {
                etagValue = etagValue.substring(1, etagValue.length()-1);
            }
            return etagValue;
        }
        throw new ChecksumProviderException("Remote repository returned unknown headers, it is not possible to parse artifact hash for " + remoteArtifact);
    }

    @Override
    public String getLocalArtifactChecksum(URI localArtifact) {
        File file = new File(localArtifact);
        return String.format("%1$x-%2$x", file.lastModified() / 1000, file.length());
    }
    
}

/*

nginx etag format is combination of last modified time in seconds plus content length, both formated in hexadecimal, 
see http://hustoknow.blogspot.cz/2014/11/how-nginx-computes-etag-header-for-files.html 

SAMPLE HTTP RESPONSE ...
 
Server: nginx
Content-Type: text/xml
Content-Length: 3345
Last-Modified: Thu, 17 Jul 2014 04:59:43 GMT
ETag: "53c7583f-d11"
Accept-Ranges: bytes
Accept-Ranges: bytes
Via: 1.1 varnish
Accept-Ranges: bytes
Date: Fri, 30 Jan 2015 07:16:20 GMT
Via: 1.1 varnish
Connection: keep-alive
X-Served-By: cache-iad2135-IAD, cache-ams4131-AMS
X-Cache: MISS, MISS
X-Cache-Hits: 0, 0
X-Timer: S1422602180.757671,VS0,VE103
 
*/