package org.jboss.wolf.validator.filter.internal;

import org.jboss.wolf.validator.ExceptionFilter;
import org.jboss.wolf.validator.filter.BomDependencyNotFoundExceptionFilter;
import org.jboss.wolf.validator.filter.DependencyNotFoundExceptionFilter;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class TestBomDependencyNotFoundFilterParser extends AbstractExceptionFilterParserTest {

    @Test
    public void shouldParseFilterWithMissingArtifactOnly() {
        initAppContext("/TestBomDependencyNotFoundFilterParser-missingArtifactOnly.xml");

        assertNumberOfBeansWithType(ExceptionFilter.class, 1);

        BomDependencyNotFoundExceptionFilter filter = getFirstMatchingBean(BomDependencyNotFoundExceptionFilter.class);
        assertNotNull(filter);
        // verify the regex was not modified during the parsing
        assertEquals("org.drools:drools-core:jar:.*", filter.getMissingArtifactRegex());
        // the validated artifact regex should not be set
        assertNull(filter.getValidatedArtifactRegex());
    }

    @Test
    public void shouldParseFilterWithBothMissingAndValidatedArtifact() {
        initAppContext("/TestBomDependencyNotFoundFilterParser-bothMissingAndValidated.xml");

        assertNumberOfBeansWithType(ExceptionFilter.class, 1);

        BomDependencyNotFoundExceptionFilter filter = getFirstMatchingBean(BomDependencyNotFoundExceptionFilter.class);
        assertNotNull(filter);
        // verify the regexes were not modified during the parsing
        assertEquals("org.drools:drools-core:jar:.*", filter.getMissingArtifactRegex());
        assertEquals("org.kie:kie-parent:pom:.*", filter.getValidatedArtifactRegex());
    }

    @Test
    public void shouldParseConfigWithMissingArtifactAndListOfValidatedArtifacts() {
        initAppContext("/TestBomDependencyNotFoundFilterParser-missingArtifactWithListOfValidated.xml");

        assertNumberOfBeansWithType(ExceptionFilter.class, 2);

        List<BomDependencyNotFoundExceptionFilter> filters = getAllMatchingBeans(BomDependencyNotFoundExceptionFilter.class);
        assertFalse("No filters with specified type found!", filters.isEmpty());
        // verify the regexes were not modified during the parsing
        assertContainsFilter(filters, "org.drools:drools-core:jar:.*", "org.kie:kie-parent:pom:.*");
        assertContainsFilter(filters, "org.drools:drools-core:jar:.*", "org.acme:acme-parent:pom:.*");
    }

    @Test
    public void shouldParseConfigWithValidatedArtifactAndListOfMissingArtifacts() {
        initAppContext("/TestBomDependencyNotFoundFilterParser-validatedArtifactWithListOfMissing.xml");

        assertNumberOfBeansWithType(ExceptionFilter.class, 2);

        List<BomDependencyNotFoundExceptionFilter> filters = getAllMatchingBeans(BomDependencyNotFoundExceptionFilter.class);
        assertFalse("No filters with specified type found!", filters.isEmpty());
        // verify the regexes were not modified during the parsing
        assertContainsFilter(filters, "org.kie:kie-api:jar:.*", "org.kie:kie-parent:pom:.*");
        assertContainsFilter(filters, "org.kie:kie-internal:jar:.*", "org.kie:kie-parent:pom:.*");
    }

    @Test
    public void shouldParseConfigWithAllOptionsInOneFile() {
        initAppContext("/TestBomDependencyNotFoundFilterParser-allConfigOptions.xml");

        assertNumberOfBeansWithType(ExceptionFilter.class, 6);

        List<BomDependencyNotFoundExceptionFilter> filters = getAllMatchingBeans(BomDependencyNotFoundExceptionFilter.class);
        assertFalse("No filters with specified type found!", filters.isEmpty());
        // verify the regexes were not modified during the parsing
        assertContainsFilter(filters, "com.acme:missing-only:jar:.*", null);
        assertContainsFilter(filters, "com.acme:missing-and-validated:jar:.*", "com.acme:missing-and-validated-parent:pom:.*");
        assertContainsFilter(filters, "com.acme:validated-and-list-of-missing1:jar:.*", "com.acme:validated-and-list-of-missing-parent:pom:.*");
        assertContainsFilter(filters, "com.acme:validated-and-list-of-missing2:jar:.*", "com.acme:validated-and-list-of-missing-parent:pom:.*");
        assertContainsFilter(filters, "com.acme:missing-and-list-of-validated:jar:.*", "com.acme:missing-and-list-of-validated-parent1:pom:.*");
        assertContainsFilter(filters, "com.acme:missing-and-list-of-validated:jar:.*", "com.acme:missing-and-list-of-validated-parent2:pom:.*");
    }

    protected void assertContainsFilter(List<? extends DependencyNotFoundExceptionFilter> filters, String missingArtifactRegex, String validatedArtifactRegex) {
        for (DependencyNotFoundExceptionFilter filter : filters) {
            if (validatedArtifactRegex == null) {
                if (missingArtifactRegex.equals(filter.getMissingArtifactRegex()) &&
                        filter.getValidatedArtifactRegex() == null) {
                    return;
                }
            } else {
                if (missingArtifactRegex.equals(filter.getMissingArtifactRegex()) &&
                        validatedArtifactRegex.equals(filter.getValidatedArtifactRegex())) {
                    return;
                }
            }
        }
        fail("Filter with missing-artifact='" + missingArtifactRegex + "' and validated-artifact='" +
                validatedArtifactRegex + "' not found!");
    }

}
