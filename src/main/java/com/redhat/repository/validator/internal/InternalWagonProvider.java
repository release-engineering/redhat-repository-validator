package com.redhat.repository.validator.internal;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.file.FileWagon;
import org.apache.maven.wagon.providers.http.HttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagonAuthenticator;
import org.apache.maven.wagon.providers.http.LightweightHttpsWagon;
import org.eclipse.aether.transport.wagon.WagonProvider;

public class InternalWagonProvider implements WagonProvider {

    private final boolean useLightweightHttpWagon;

    public InternalWagonProvider() {
        this(false);
    }

    public InternalWagonProvider(boolean useLightweightHttpWagon) {
        this.useLightweightHttpWagon = useLightweightHttpWagon;
    }

    @Override
    public Wagon lookup(String roleHint) throws Exception {
        if ("http".equals(roleHint)) {
            if (useLightweightHttpWagon) {
                LightweightHttpWagon lightweightHttpWagon = new LightweightHttpWagon();
                FieldUtils.writeField(lightweightHttpWagon, "authenticator", new LightweightHttpWagonAuthenticator(), true);
                return lightweightHttpWagon;
            } else {
                return new HttpWagon();
            }
        } else if ("https".equals(roleHint)) {
            if (useLightweightHttpWagon) {
                LightweightHttpsWagon lightweightHttpsWagon = new LightweightHttpsWagon();
                FieldUtils.writeField(lightweightHttpsWagon, "authenticator", new LightweightHttpWagonAuthenticator(), true);
                return lightweightHttpsWagon;
            } else {
                return new HttpWagon();
            }
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