package org.jboss.wolf.validator.internal;

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.file.FileWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.eclipse.aether.connector.wagon.WagonProvider;

public class InternalWagonProvider implements WagonProvider {

    @Override
    public Wagon lookup(String roleHint) throws Exception {
        if ("http".equals(roleHint) || "https".equals(roleHint)) {
            return new LightweightHttpWagon();
        } else if ("file".equals(roleHint)) {
            return new FileWagon();
        }
        return null;
    }

    @Override
    public void release(Wagon wagon) {
        // noop
    }

}