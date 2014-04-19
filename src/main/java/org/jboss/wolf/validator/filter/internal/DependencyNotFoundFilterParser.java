package org.jboss.wolf.validator.filter.internal;

import org.jboss.wolf.validator.filter.DependencyNotFoundExceptionFilter;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DependencyNotFoundFilterParser extends AbstractExceptionFilterParser {

    private static int index = 1;

    @Override
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
        if (element.hasChildNodes()) {
            List<AbstractBeanDefinition> beansDefinitions = parseConfigWithChildren(element);
            registerBeanDefinitions(parserContext.getRegistry(), beansDefinitions);
        } else {
            AbstractBeanDefinition beanDefinition = parseConfigWithoutChildren(element);
            registerBeanDefinitions(parserContext.getRegistry(), beanDefinition);
        }
        // all beans have been alredy registered, so nothing to return
        return null;
    }

    protected Class<? extends DependencyNotFoundExceptionFilter> getBeanType() {
        return DependencyNotFoundExceptionFilter.class;
    }

    private void registerBeanDefinitions(BeanDefinitionRegistry registry, Collection<AbstractBeanDefinition> beanDefinitions) {
        registerBeanDefinitions(registry, beanDefinitions.toArray(new AbstractBeanDefinition[]{}));
    }

    private void registerBeanDefinitions(BeanDefinitionRegistry registry, AbstractBeanDefinition... beanDefinitions) {
        for (AbstractBeanDefinition beanDefinition : beanDefinitions) {
            registry.registerBeanDefinition("dependencyNotFoundFilter" + index++, beanDefinition);
        }
    }

    private List<AbstractBeanDefinition> parseConfigWithChildren(Element element) {
        List<AbstractBeanDefinition> beanDefinitions = new ArrayList<AbstractBeanDefinition>();
        if (element.hasAttribute("missing-artifact") && !element.hasAttribute("validated-artifact")) {
            // missing artifact + list of validated artifacts
            String missingArtifact = element.getAttribute("missing-artifact");
            List<String> validatedArtifacts = parseValidatedArtifacts(element);
            for (String validatedArtifact : validatedArtifacts) {
                beanDefinitions.add(createBeanDef(missingArtifact, validatedArtifact));
            }
        } else if (!element.hasAttribute("missing-artifact") && element.hasAttribute("validated-artifact")) {
            // validated artifact + list of missing artifacts
            String validatedArtifact = element.getAttribute("validated-artifact");
            List<String> missingArtifacts = parseMissingArtifacts(element);
            for (String missingArtifact : missingArtifacts) {
                beanDefinitions.add(createBeanDef(missingArtifact, validatedArtifact));
            }
        } else {
            // none or both attributes specified which is ambiguous
            throw new RuntimeException("The dependency-not-found filter definition with child elements need to have " +
                    "exactly one attribute defined ('missing-artifact' or 'validated-artifact'!");
        }
        return beanDefinitions;
    }

    private List<String> parseValidatedArtifacts(Element element) {
        List<String> validatedArtifacts = new ArrayList<String>();
        List<Element> validatedArtifactRootElements = DomUtils.getChildElementsByTagName(element, "validated-artifacts");
        if (validatedArtifactRootElements.size() == 1) {
            Element rootValidatedArtifacts = validatedArtifactRootElements.get(0);
            List<Element> validatedArtifactsElements = DomUtils.getChildElementsByTagName(rootValidatedArtifacts, "validated-artifact");
            for (Element validatedArtifact : validatedArtifactsElements) {
                validatedArtifacts.add(validatedArtifact.getTextContent());
            }
        } else {
            throw new RuntimeException("Exactly one instance of 'validated-artifacts' element needs to be defined in case" +
                    "the filter is specifying list of validated artifacts!");
        }
        return validatedArtifacts;
    }

    private List<String> parseMissingArtifacts(Element element) {
        List<String> validatedArtifacts = new ArrayList<String>();
        List<Element> validatedArtifactRootElements = DomUtils.getChildElementsByTagName(element, "missing-artifacts");
        if (validatedArtifactRootElements.size() == 1) {
            Element rootValidatedArtifacts = validatedArtifactRootElements.get(0);
            List<Element> validatedArtifactsElements = DomUtils.getChildElementsByTagName(rootValidatedArtifacts, "missing-artifact");
            for (Element validatedArtifact : validatedArtifactsElements) {
                validatedArtifacts.add(validatedArtifact.getTextContent());
            }
        } else {
            throw new RuntimeException("Exactly one instance of 'missing-artifacts' element needs to be defined in case" +
                    "the filter is specifying list of missing artifacts!");
        }
        return validatedArtifacts;
    }

    private AbstractBeanDefinition parseConfigWithoutChildren(Element element) {
        if (element.hasAttribute("missing-artifact")) {
            String missingArtifactRegex = element.getAttribute("missing-artifact");
            if (element.hasAttribute("validated-artifact")) {
                String validatedArtifactRegex = element.getAttribute("validated-artifact");
                return createBeanDef(missingArtifactRegex, validatedArtifactRegex);
            } else {
                return createBeanDef(missingArtifactRegex);
            }
        } else {
            throw new RuntimeException("The filter configuration without any child nodes needs to define at least " +
                    "attribute named 'missing-artifact'!");
        }
    }

    private AbstractBeanDefinition createBeanDef(String missingArtifactRegex) {
        return BeanDefinitionBuilder.
                rootBeanDefinition(getBeanType()).
                addConstructorArgValue(missingArtifactRegex).
                getBeanDefinition();
    }

    private AbstractBeanDefinition createBeanDef(String missingArtifactRegex, String validatedArtifactRegex) {
        return BeanDefinitionBuilder.
                rootBeanDefinition(getBeanType()).
                addConstructorArgValue(missingArtifactRegex).
                addConstructorArgValue(validatedArtifactRegex).
                getBeanDefinition();
    }

}
