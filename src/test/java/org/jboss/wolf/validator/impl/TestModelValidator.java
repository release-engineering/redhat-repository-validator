package org.jboss.wolf.validator.impl;

import static org.jboss.wolf.validator.impl.TestUtil.pom;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.io.ModelParseException;
import org.junit.Test;

public class TestModelValidator extends AbstractTest {

    @Test
    public void shouldFindDamageNoGroupId() {
        pom().withDamage("<groupId>com.acme</groupId>", "").create(repoFooDir);
        validator.validate(ctx);
        assertExpectedException(ModelBuildingException.class, "'groupId' is missing");
    }

    @Test
    public void shouldFindDamageUnclosedGroupId() {
        pom().withDamage("</groupId>", "<groupId>").create(repoFooDir);
        validator.validate(ctx);
        assertExpectedException(ModelParseException.class, "TEXT must be immediately followed by END_TAG");
    }

    @Test
    public void shouldFindInvalidModelVersion() {
        pom().withDamage("<modelVersion>4.0.0", "<modelVersion>999").create(repoFooDir);
        validator.validate(ctx);
        assertExpectedException(ModelBuildingException.class, "'modelVersion' must be one of [4.0.0]");
    }

    @Test
    public void shouldFindInvalidPackagingForParent() {
        Model fooParent = pom().artifactId("foo-parent").create(repoFooDir);
        pom().artifactId("foo-api").parent(fooParent).create(repoFooDir);
        validator.validate(ctx);
        assertExpectedException(ModelBuildingException.class, "Invalid packaging for parent POM");
    }

}