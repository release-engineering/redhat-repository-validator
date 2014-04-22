package org.jboss.wolf.validator.filter.internal;

import org.jboss.wolf.validator.filter.BomDependencyNotFoundExceptionFilter;
import org.jboss.wolf.validator.filter.DependencyNotFoundExceptionFilter;

public class BomDependencyNotFoundFilterParser extends DependencyNotFoundFilterParser {

    @Override
    protected Class<? extends DependencyNotFoundExceptionFilter> getBeanType() {
        return BomDependencyNotFoundExceptionFilter.class;
    }

}
