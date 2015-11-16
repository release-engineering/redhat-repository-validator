package com.redhat.repository.validator.filter.internal;

import com.redhat.repository.validator.filter.BomDependencyNotFoundExceptionFilter;
import com.redhat.repository.validator.filter.DependencyNotFoundExceptionFilter;

public class BomDependencyNotFoundFilterParser extends DependencyNotFoundFilterParser {

    @Override
    protected Class<? extends DependencyNotFoundExceptionFilter> getBeanType() {
        return BomDependencyNotFoundExceptionFilter.class;
    }

}
