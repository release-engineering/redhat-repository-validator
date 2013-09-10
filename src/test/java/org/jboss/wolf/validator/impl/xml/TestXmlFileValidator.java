package org.jboss.wolf.validator.impl.xml;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.jboss.wolf.validator.impl.AbstractTest;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;
import java.io.IOException;

import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;

@ContextConfiguration
public class TestXmlFileValidator extends AbstractTest {
    @Configuration
    public static class TestConfiguration {
        @Bean
        public IOFileFilter xmlFileValidatorFilter() {
            return trueFileFilter();
        }
    }

    private final File validXmlFile = new File(repoFooDir, "settings-valid.xml");
    private final File notValidXmlFile = new File(repoFooDir, "settings-not-valid.xml");
    private final File sourceXmlFile = new File("target/test-classes/settings-valid.xml");

    @Test
    public void validXML_shouldSuccess() throws IOException {
        FileUtils.copyFile(sourceXmlFile, validXmlFile);

        validator.validate(ctx);
        assertSuccess();
    }

    @Test
    public void notValidXML_addBracket_shouldFail() throws IOException {
        FileUtils.copyFile(sourceXmlFile, notValidXmlFile);

        String xmlText = FileUtils.readFileToString(notValidXmlFile);
        xmlText = xmlText.replace("<profiles>", "  <<profiles>");
        FileUtils.writeStringToFile(notValidXmlFile, xmlText);

        validator.validate(ctx);
        assertExpectedException(XmlVerificationException.class, "Xml file settings-not-valid.xml has following errors (182, 6): The content of elements must consist of well-formed character data or markup.");
    }

    @Test
    public void notValidXML_delBracket_shouldFail() throws IOException {
        FileUtils.copyFile(sourceXmlFile, notValidXmlFile);

        String xmlText = FileUtils.readFileToString(notValidXmlFile);
        xmlText = xmlText.replace("<profiles>", "<profiles");
        FileUtils.writeStringToFile(notValidXmlFile, xmlText);

        validator.validate(ctx);
        assertExpectedException(XmlVerificationException.class, "Xml file settings-not-valid.xml has following errors (183, 5): Element type \"profiles\" must be followed by either attribute specifications, \">\" or \"/>\".");
    }

}
