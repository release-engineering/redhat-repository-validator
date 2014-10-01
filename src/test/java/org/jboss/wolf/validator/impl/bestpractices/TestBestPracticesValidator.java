package org.jboss.wolf.validator.impl.bestpractices;

import static org.jboss.wolf.validator.impl.TestUtil.pom;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Scm;
import org.jboss.wolf.validator.impl.AbstractTest;
import org.jboss.wolf.validator.impl.TestUtil.PomBuilder;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration
public class TestBestPracticesValidator extends AbstractTest {

    @Configuration
    public static class TestConfiguration {

        @Bean
        public IOFileFilter bestPracticesValidatorFilter() {
            return new TestFileFilter();
        }

    }

    @Test
    public void shouldFindBestPracticesViolation() {
        Repository repository = new Repository();
        repository.setId("repo-id");
        repository.setName("repo-name");
        repository.setUrl("repo-url");

        PomBuilder pomBuilder = pom();
        pomBuilder.model().addRepository(repository);
        pomBuilder.model().addPluginRepository(repository);
        pomBuilder.create(repoFooDir);

        validationExecutor.execute(ctx);
        assertExpectedException(BestPracticesException.class, "contains <repositories> configuration");
        assertExpectedException(BestPracticesException.class, "contains <pluginRepositories> configuration");
        assertExpectedException(BestPracticesException.class, "doesn't contain <name>");
        assertExpectedException(BestPracticesException.class, "doesn't contain <description>");
        assertExpectedException(BestPracticesException.class, "doesn't contain <url>");
        assertExpectedException(BestPracticesException.class, "doesn't contain <licenses>");
        assertExpectedException(BestPracticesException.class, "doesn't contain <developers>");
        assertExpectedException(BestPracticesException.class, "doesn't contain <scm>");
    }

    @Test
    public void shouldNotFindBestPracticesViolation() {
        License license = new License();
        license.setName("license-name");
        license.setUrl("license-url");

        Developer developer = new Developer();
        developer.setId("dev-id");
        developer.setName("dev-name");

        Scm scm = new Scm();
        scm.setUrl("scm-url");
        scm.setConnection("scm-connection");

        PomBuilder pomBuilder = pom();
        pomBuilder.model().setName("foo-name");
        pomBuilder.model().setDescription(""); // empty description is allowed, see WOLF-69
        pomBuilder.model().setUrl("foo-url");
        pomBuilder.model().addLicense(license);
        pomBuilder.model().addDeveloper(developer);
        pomBuilder.model().setScm(scm);
        pomBuilder.create(repoFooDir);

        validationExecutor.execute(ctx);
        assertSuccess();
    }

}