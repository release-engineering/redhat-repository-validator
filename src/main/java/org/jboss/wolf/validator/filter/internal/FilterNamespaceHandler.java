package org.jboss.wolf.validator.filter.internal;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class FilterNamespaceHandler extends NamespaceHandlerSupport {

    @Override
    public void init() {
        registerBeanDefinitionParser("bom-dependency-not-found", new BomDependencyNotFoundFilterParser());
        registerBeanDefinitionParser("dependency-not-found", new DependencyNotFoundFilterParser());
    }

}
