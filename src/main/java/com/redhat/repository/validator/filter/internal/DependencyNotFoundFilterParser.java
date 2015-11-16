package com.redhat.repository.validator.filter.internal;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import com.redhat.repository.validator.filter.DependencyNotFoundExceptionFilter;

import java.util.ArrayList;
import java.util.List;

public class DependencyNotFoundFilterParser extends AbstractExceptionFilterParser {

    @Override
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
        if (element.hasChildNodes()) {
            List<AbstractBeanDefinition> beansDefinitions = parseConfigWithChildElements(element, parserContext);
            registerBeanDefinitions(parserContext.getRegistry(), beansDefinitions);
        } else {
            AbstractBeanDefinition beanDefinition = parseConfigWithoutChildElements(element);
            registerBeanDefinitions(parserContext.getRegistry(), beanDefinition);
        }
        // all beans have been already registered, so nothing to return

        return null;
    }

    protected Class<? extends DependencyNotFoundExceptionFilter> getBeanType() {
        return DependencyNotFoundExceptionFilter.class;
    }

    /**
     * Parses the the XML config and primarily expects that there are some child element for the specified element.
     *
     * @param element top-level "filter" element
     * @param parserContext
     * @return list of bean definitions, each of type {@link com.redhat.repository.validator.ExceptionFilter}
     */
    private List<AbstractBeanDefinition> parseConfigWithChildElements(Element element, ParserContext parserContext) {
        List<AbstractBeanDefinition> beanDefinitions = new ArrayList<AbstractBeanDefinition>();
        if (element.hasAttribute("missing-artifact") && !element.hasAttribute("validated-artifact")) {
            // missing artifact + list of validated artifacts
            String missingArtifact = element.getAttribute("missing-artifact");
            List<String> validatedArtifacts = parseValidatedArtifacts(element, parserContext);
            for (String validatedArtifact : validatedArtifacts) {
                beanDefinitions.add(createBeanDef(missingArtifact, validatedArtifact));
            }
        } else if (!element.hasAttribute("missing-artifact") && element.hasAttribute("validated-artifact")) {
            // validated artifact + list of missing artifacts
            String validatedArtifact = element.getAttribute("validated-artifact");
            List<String> missingArtifacts = parseMissingArtifacts(element, parserContext);
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

    private List<String> parseValidatedArtifacts(Element element, ParserContext parserContext) {
        List<String> validatedArtifacts = new ArrayList<String>();
        List<Element> validatedArtifactRootElements = DomUtils.getChildElementsByTagName(element, "validated-artifacts");
        if (validatedArtifactRootElements.size() == 1) {
            Element rootValidatedArtifacts = validatedArtifactRootElements.get(0);
            if (rootValidatedArtifacts.hasAttribute("ref")) {
                String listId = rootValidatedArtifacts.getAttribute("ref");
                validatedArtifacts.addAll(parseListOfRegexs(parserContext.getRegistry().getBeanDefinition(listId)));
            }
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

    private List<String> parseMissingArtifacts(Element element, ParserContext parserContext) {
        List<String> missingArtifacts = new ArrayList<String>();
        List<Element> missingArtifactRootElements = DomUtils.getChildElementsByTagName(element, "missing-artifacts");
        if (missingArtifactRootElements.size() == 1) {
            Element rootValidatedArtifacts = missingArtifactRootElements.get(0);
            if (rootValidatedArtifacts.hasAttribute("ref")) {
                String listId = rootValidatedArtifacts.getAttribute("ref");
                missingArtifacts.addAll(parseListOfRegexs(parserContext.getRegistry().getBeanDefinition(listId)));
            }
            List<Element> missingArtifactsElements = DomUtils.getChildElementsByTagName(rootValidatedArtifacts, "missing-artifact");
            for (Element validatedArtifact : missingArtifactsElements) {
                missingArtifacts.add(validatedArtifact.getTextContent());
            }
        } else {
            throw new RuntimeException("Exactly one instance of 'missing-artifacts' element needs to be defined in case" +
                    "the filter is specifying list of missing artifacts!");
        }
        return missingArtifacts;
    }

    /**
     * Parses a list of regular expressions stored inside list bean, defined by the standard <util:list>.
     */
    private List<String> parseListOfRegexs(BeanDefinition listBean) {
        Object value = listBean.getPropertyValues().getPropertyValue("sourceList").getValue();
        List<String> regexs = new ArrayList<String>();
        if (value instanceof ManagedList) {
            @SuppressWarnings("rawtypes")
            ManagedList regexList = (ManagedList) value;
            for (int i = 0; i < regexList.size(); i++) {
                Object obj = regexList.get(i);
                if (obj instanceof TypedStringValue) {
                    regexs.add(((TypedStringValue) obj).getValue());
                } else {
                    throw new RuntimeException("Can't parse the list of regular expressions from bean " + listBean);
                }
            }
        } else {
            throw new RuntimeException("Can't parse the list of regular expressions from bean " + listBean);
        }
        return regexs;
    }

    /**
     * Parses the the XML config that has no child elements (the method will ignore such elements even if they are
     * present). The result is a single bean definition, either with both missing-artifact and validated-artifact
     * or only missing-artifact, based on what attributes are defined for the element.
     *
     * @param element top-level "filter" element
     * @return bean definition representing single exception filter
     */
    private AbstractBeanDefinition parseConfigWithoutChildElements(Element element) {
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
