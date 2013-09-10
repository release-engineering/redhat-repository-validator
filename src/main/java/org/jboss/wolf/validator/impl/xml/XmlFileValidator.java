package org.jboss.wolf.validator.impl.xml;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

import static org.apache.commons.io.filefilter.FileFilterUtils.*;
import static org.apache.commons.io.filefilter.FileFilterUtils.notFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.suffixFileFilter;
import static org.jboss.wolf.validator.internal.Utils.relativize;

@Named
public class XmlFileValidator implements Validator {

    @Inject @Named("xmlFileValidatorFilter")
    private IOFileFilter fileFilter;
    private static final Logger logger = LoggerFactory.getLogger(XmlFileValidator.class);

    @Override
    public void validate(ValidatorContext ctx) {
        Collection<File> xmlFiles = FileUtils.listFiles(ctx.getValidatedRepository(), and(fileFilter, excludePomFilesFilter(), includeXmlFileFilter()), trueFileFilter());
        for (File xmlFile : xmlFiles) {
            logger.trace("validating {}", relativize(ctx, xmlFile));
            validateFile(ctx, xmlFile);
        }

    }

    private void validateFile(ValidatorContext ctx, File xmlFile) {
        try {
            Source xmlSource = new StreamSource(xmlFile);
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema();
            javax.xml.validation.Validator validator = schema.newValidator();
            validator.validate(xmlSource);
        } catch (SAXParseException e) {
            ctx.addException(xmlFile, new XmlVerificationException(relativize(ctx, xmlFile), formatException(e)));
        } catch (SAXException e) {
            ctx.addException(xmlFile, new XmlVerificationException(relativize(ctx, xmlFile), e));
        } catch (IOException e) {
            ctx.addException(xmlFile, new XmlVerificationException(relativize(ctx, xmlFile), e));
        }
    }

    public IOFileFilter excludePomFilesFilter() {
        return notFileFilter(nameFileFilter("pom.xml"));
    }

    public IOFileFilter includeXmlFileFilter() {
        return suffixFileFilter(".xml");
    }

    private String formatException(SAXParseException x) {
        return String.format("(%d, %d): %s", x.getLineNumber(), x.getColumnNumber(), x.getMessage());
    }
}
