package org.jboss.wolf.validator.filter.internal;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

abstract public class AbstractExceptionFilterParserTest {
    protected ApplicationContext appCtx;

    protected void initAppContext(String appConfigClassPath) {
        Resource config = new ClassPathResource(appConfigClassPath);
        appCtx = new GenericXmlApplicationContext(config);

        AutowireCapableBeanFactory autowireCapableBeanFactory = appCtx.getAutowireCapableBeanFactory();
        autowireCapableBeanFactory.autowireBean(this);
    }

    /**
     * Returns first bean that matches the specified type.
     */
    protected <T> T getFirstMatchingBean(Class<T> beanType) {
        Map<String, T> beans = appCtx.getBeansOfType(beanType);
        if (!beans.isEmpty()) {
            return beans.values().iterator().next();
        } else {
            throw new AssertionError("Expected to bean with type '" + beanType.getCanonicalName() + "', but none found!");
        }
    }

    protected <T> List<T> getAllMatchingBeans(Class<T> beanType) {
        Map<String, T> beans = appCtx.getBeansOfType(beanType);
        return new ArrayList<T>(beans.values());
    }

    protected void assertNumberOfBeansWithType(Class beanType, int expectedNrOfBeans) {
        Map<String, Class> filters = appCtx.getBeansOfType(beanType);
        assertEquals("Number of created filters.", expectedNrOfBeans, filters.size());
    }


}
