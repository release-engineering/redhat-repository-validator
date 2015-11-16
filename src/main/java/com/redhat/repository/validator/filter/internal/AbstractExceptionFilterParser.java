package com.redhat.repository.validator.filter.internal;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;

import java.util.Collection;

abstract public class AbstractExceptionFilterParser extends AbstractBeanDefinitionParser {
    private static int index = 1;

    protected void registerBeanDefinitions(BeanDefinitionRegistry registry, Collection<AbstractBeanDefinition> beanDefinitions) {
        registerBeanDefinitions(registry, beanDefinitions.toArray(new AbstractBeanDefinition[]{}));
    }

    protected void registerBeanDefinitions(BeanDefinitionRegistry registry, AbstractBeanDefinition... beanDefinitions) {
        for (AbstractBeanDefinition beanDefinition : beanDefinitions) {
            registry.registerBeanDefinition("exceptionFilter" + index++, beanDefinition);
        }
    }

}
