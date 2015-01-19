package org.jboss.wolf.validator.filter.internal;

import com.google.common.collect.Lists;

import org.jboss.wolf.validator.filter.FilenameBasedExceptionFilter;
import org.jboss.wolf.validator.impl.DependencyNotFoundException;
import org.jboss.wolf.validator.impl.UnknownArtifactTypeException;
import org.jboss.wolf.validator.impl.bestpractices.BestPracticesException;
import org.jboss.wolf.validator.impl.bom.BomAmbiguousVersionException;
import org.jboss.wolf.validator.impl.bom.BomDependencyNotFoundException;
import org.jboss.wolf.validator.impl.bom.BomUnmanagedVersionException;
import org.jboss.wolf.validator.impl.bom.BomVersionPropertyException;
import org.jboss.wolf.validator.impl.checksum.ChecksumNotExistException;
import org.jboss.wolf.validator.impl.checksum.ChecksumNotMatchException;
import org.jboss.wolf.validator.impl.distribution.*;
import org.jboss.wolf.validator.impl.signature.JarSignatureVerificationException;
import org.jboss.wolf.validator.impl.signature.JarSignedException;
import org.jboss.wolf.validator.impl.signature.JarUnsignedException;
import org.jboss.wolf.validator.impl.source.JarSourcesVerificationException;
import org.jboss.wolf.validator.impl.suspicious.SuspiciousFileException;
import org.jboss.wolf.validator.impl.version.VersionAmbiguityException;
import org.jboss.wolf.validator.impl.version.VersionOverlapException;
import org.jboss.wolf.validator.impl.version.VersionPatternException;
import org.jboss.wolf.validator.impl.xml.XmlVerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

public class FilenameBasedExceptionFilterParser extends AbstractExceptionFilterParser {

    private static final Logger logger = LoggerFactory.getLogger(FilenameBasedExceptionFilterParser.class);

    /**
     * List of exceptions known to the validator in alphabetical order.
     * <p/>
     * Keep the order when adding new exceptions.
     */
    @SuppressWarnings("unchecked")
    public static final List<Class<? extends Exception>> KNOWN_EXCEPTIONS = Lists.newArrayList(
            BestPracticesException.class,
            BomAmbiguousVersionException.class,
            BomDependencyNotFoundException.class,
            BomUnmanagedVersionException.class,
            BomVersionPropertyException.class,
            ChecksumNotExistException.class,
            ChecksumNotMatchException.class,
            DependencyNotFoundException.class,
            DistributionCorruptedFileException.class,
            DistributionDuplicateFilesException.class,
            DistributionMisnomerFileException.class,
            DistributionMissingFileException.class,
            DistributionRedundantFileException.class,
            JarSignatureVerificationException.class,
            JarSignedException.class,
            JarUnsignedException.class,
            JarSourcesVerificationException.class,
            SuspiciousFileException.class,
            UnknownArtifactTypeException.class,
            VersionAmbiguityException.class,
            VersionOverlapException.class,
            VersionPatternException.class,
            XmlVerificationException.class
    );

    @Override
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
        logger.warn("Using DEPRECATED filter 'filename'. Please, use filter 'file' instead.");
        final BeanDefinitionRegistry registry = parserContext.getRegistry();
        // the regex attribute is enforced by the XSD
        String filenameRegex = element.getAttribute("regex");
        if (element.hasChildNodes()) {
            List<AbstractBeanDefinition> filterBeans = new ArrayList<AbstractBeanDefinition>();
            for (Class<? extends Exception> exceptionType : parseListOfExceptions(element)) {
                filterBeans.add(createBeanDef(filenameRegex, exceptionType));
            }
            registerBeanDefinitions(registry, filterBeans);
        } else {
            // no children artifacts, only attributes set
            if (element.hasAttribute("exception")) {
                Class<? extends Exception> exceptionType = determineExceptionTypeFromString(element.getAttribute("exception"));
                registerBeanDefinitions(registry, createBeanDef(filenameRegex, exceptionType));
            } else {
                registerBeanDefinitions(registry, createBeanDef(filenameRegex));
            }

        }
        return null;
    }

    private List<Class<? extends Exception>> parseListOfExceptions(Element element) {
        List<Class<? extends Exception>> exceptions = new ArrayList<Class<? extends Exception>>();
        List<Element> exceptionsRootElements = DomUtils.getChildElementsByTagName(element, "exceptions");
        if (exceptionsRootElements.size() == 1) {
            Element rootValidatedArtifacts = exceptionsRootElements.get(0);
            List<Element> exceptionElements = DomUtils.getChildElementsByTagName(rootValidatedArtifacts, "exception");
            for (Element exceptionElement : exceptionElements) {
                Class<? extends Exception> exceptionType = determineExceptionTypeFromString(exceptionElement.getTextContent());
                exceptions.add(exceptionType);
            }
        } else {
            throw new RuntimeException("Exactly one instance of 'exceptions' element needs to be defined in case" +
                    "the filter is specifying list exceptions to ignore for particular filename!");
        }
        return exceptions;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Class<? extends Exception> determineExceptionTypeFromString(String exceptionTypeString) {
        for (Class<? extends Exception> exceptionType : KNOWN_EXCEPTIONS) {
            if (exceptionType.getSimpleName().equals(exceptionTypeString)) {
                return exceptionType;
            }
        }
        // the exception is not known, so try to instantiate it
        Class clazz;
        try {
            clazz = Class.forName(exceptionTypeString);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Can't instantiate specified exception '" + exceptionTypeString + "'!", e);
        }
        // check if the instantiated class in indeed subclass of Exception
        if (Exception.class.isAssignableFrom(clazz)) {
            return clazz;
        } else {
            throw new RuntimeException("The specified exception class '" + clazz.getCanonicalName() + "' needs to" +
                    "extends the java.lang.Exception!");
        }
    }

    private AbstractBeanDefinition createBeanDef(String filenameRegex) {
        return BeanDefinitionBuilder.
                rootBeanDefinition(FilenameBasedExceptionFilter.class).
                addConstructorArgValue(filenameRegex).
                getBeanDefinition();
    }

    private AbstractBeanDefinition createBeanDef(String filenameRegex, Class<? extends Exception> exceptionType) {
        return BeanDefinitionBuilder.
                rootBeanDefinition(FilenameBasedExceptionFilter.class).
                addConstructorArgValue(filenameRegex).
                addConstructorArgValue(exceptionType).
                getBeanDefinition();
    }

}