package org.jboss.wolf.validator.impl.bestpractices;

import static org.jboss.wolf.validator.impl.TestUtil.pom;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Scm;
import org.jboss.wolf.validator.ValidationExecutor;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.impl.AbstractTest;
import org.jboss.wolf.validator.impl.TestUtil.PomBuilder;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration
public class TestBestPracticesValidatorAllowedRepositories extends AbstractTest {

    @Configuration
    public static class TestConfiguration {

        @Bean
        public ValidationExecutor validationExecutor(Validator[] validators) {
            return new ValidationExecutor(bestPracticesValidatorAllowedRepositories());
        }

        @Bean
        public BestPracticesValidator bestPracticesValidatorAllowedRepositories() {
            return new BestPracticesValidator(
                    new String[] { "repo-bar" }, 
                    new String[] { "repo-baz" });
        }

        @Bean
        public IOFileFilter bestPracticesValidatorFilter() {
            return new TestFileFilter();
        }

    }

    @Test
    public void shouldIgnoreAllowedRepositories() {
        PomBuilder pomBuilder = pomBuilder();
        pomBuilder.model().addRepository(repository("repo-bar"));
        pomBuilder.model().addPluginRepository(repository("repo-baz"));
        pomBuilder.create(repoFooDir);

        validationExecutor.execute(ctx);
        assertSuccess();
    }

    @Test
    public void shouldFindUnallowedRepositories() {
        PomBuilder pomBuilder = pomBuilder();
        pomBuilder.model().addRepository(repository("repo-aaa"));
        pomBuilder.model().addPluginRepository(repository("repo-bbb"));
        pomBuilder.create(repoFooDir);

        validationExecutor.execute(ctx);

        assertExpectedException(BestPracticesException.class, "contains <repository> configuration with unallowed url repo-aaa");
        assertExpectedException(BestPracticesException.class, "contains <pluginRepository> configuration with unallowed url repo-bbb");
    }

    private PomBuilder pomBuilder() {
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
        pomBuilder.model().setDescription("foo-description");
        pomBuilder.model().setUrl("foo-url");
        pomBuilder.model().addLicense(license);
        pomBuilder.model().addDeveloper(developer);
        pomBuilder.model().setScm(scm);
        return pomBuilder;
    }

    private Repository repository(String id) {
        Repository repo = new Repository();
        repo.setId(id);
        repo.setName(id);
        repo.setUrl(id);
        return repo;
    }

}