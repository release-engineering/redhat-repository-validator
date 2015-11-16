package com.redhat.repository.validator.internal;

import org.apache.maven.wagon.Wagon;
import org.eclipse.aether.transport.wagon.WagonConfigurator;

public class InternalWagonConfigurator implements WagonConfigurator {

    @Override
    public void configure(Wagon wagon, Object configuration) throws Exception {
        // noop
    }

}