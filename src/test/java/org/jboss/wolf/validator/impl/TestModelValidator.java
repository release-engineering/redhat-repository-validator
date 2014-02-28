package org.jboss.wolf.validator.impl;

import static org.jboss.wolf.validator.impl.TestUtil.pom;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.io.ModelParseException;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration
public class TestModelValidator extends AbstractTest {
    
    @Configuration
    public static class TestConfiguration {

        @Bean
        public IOFileFilter modelValidatorFilter() {
            return new TestFileFilter();
        }
        
        @Bean
        public IOFileFilter dependenciesValidatorFilter() {
            return new TestFileFilter();
        }

    }

    @Test
    public void shouldFindDamageNoGroupId() {
        pom().withDamage("<groupId>com.acme</groupId>", "").create(repoFooDir);
        validationExecutor.execute(ctx);
        assertExpectedException(ModelBuildingException.class, "'groupId' is missing");
    }

    @Test
    public void shouldFindDamageUnclosedGroupId() {
        pom().withDamage("</groupId>", "<groupId>").create(repoFooDir);
        validationExecutor.execute(ctx);
        assertExpectedException(ModelParseException.class, "TEXT must be immediately followed by END_TAG");
    }

    @Test
    public void shouldFindInvalidModelVersion() {
        pom().withDamage("<modelVersion>4.0.0", "<modelVersion>999").create(repoFooDir);
        validationExecutor.execute(ctx);
        assertExpectedException(ModelBuildingException.class, "'modelVersion' must be one of [4.0.0]");
    }

    @Test
    public void shouldFindInvalidPackagingForParent() {
        Model fooParent = pom().artifactId("foo-parent").create(repoFooDir);
        pom().artifactId("foo-api").parent(fooParent).create(repoFooDir);
        validationExecutor.execute(ctx);
        assertExpectedException(ModelBuildingException.class, "Invalid packaging for parent POM");
    }

}