package org.jboss.wolf.validator.impl.aether;

import org.apache.maven.wagon.Wagon;
import org.eclipse.aether.connector.wagon.WagonConfigurator;

public class InternalWagonConfigurator implements WagonConfigurator {

    @Override
    public void configure(Wagon wagon, Object configuration) throws Exception {
        // noop
    }

}