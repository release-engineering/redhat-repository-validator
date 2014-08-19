package org.jboss.wolf.validator.impl.remoterepository;

import static org.jboss.wolf.validator.internal.Utils.calculateChecksum;

import java.io.File;
import java.net.URI;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

public class RemoteRepositoryCompareStrategyNexus extends RemoteRepositoryCompareStrategyAbstract {

    @Override
    protected String getRemoteArtifactHash(URI remoteArtifact, HttpResponse httpResponse) {
        Header etagHeader = httpResponse.getFirstHeader("ETag");
        if (etagHeader != null) {
            String etagValue = etagHeader.getValue();
            if (etagValue != null && etagValue.startsWith("\"{SHA1{") && etagValue.endsWith("}}\"")) {
                String sha1 = etagValue.substring(7, etagValue.length() - 3);
                return sha1;
            }
        }
        return null;
    }

    @Override
    protected String getLocalArtifactHash(URI localArtifact) {
        return calculateChecksum(new File(localArtifact), "sha1");
    }
    
}

/*
  
SAMPLE HTTP RESPONSE ...

Date : Mon, 18 Aug 2014 11:46:34 GMT
Server : Nexus/2.7.2-03
Accept-Ranges : bytes
ETag : "{SHA1{e6f1e89880e645c66ef9c30d60a68f7e26f3152d}}"
Content-Type : application/java-archive
Last-Modified : Thu, 17 Jul 2014 04:59:39 GMT
Content-Length : 5254140
X-Content-Type-Options : nosniff
Set-Cookie : rememberMe=deleteMe; Path=/nexus; Max-Age=0; Expires=Sun, 17-Aug-2014 11:46:34 GMT
Set-Cookie : LBROUTE=proxy02; path=/
Keep-Alive : timeout=10, max=128
Connection : Keep-Alive

*/