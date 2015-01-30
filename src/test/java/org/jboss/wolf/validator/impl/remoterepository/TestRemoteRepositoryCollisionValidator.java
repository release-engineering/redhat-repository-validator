package org.jboss.wolf.validator.impl.remoterepository;

import static org.jboss.wolf.validator.impl.TestUtil.pom;

import java.net.URISyntaxException;

import org.jboss.wolf.validator.impl.AbstractTest;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration
public class TestRemoteRepositoryCollisionValidator extends AbstractTest {

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RemoteRepositoryCollisionValidator collisionValidatorMavenCentral() {
            return new RemoteRepositoryCollisionValidator("http://repo1.maven.org/maven2/", new ChecksumProviderNginx());
        }

        @Bean
        public RemoteRepositoryCollisionValidator collisionValidatorJBossNexus() {
            return new RemoteRepositoryCollisionValidator("https://repository.jboss.org/nexus/content/groups/public-jboss/", new ChecksumProviderNexus());
        }

    }

    @Test
    public void shouldSucceedForUnpublishedArtifact() throws Exception {
        pom().artifactId("foo").create(repoFooDir);

        validationExecutor.execute(ctx);

        assertSuccess();
    }

    @Test
    public void shouldFailForAlreadyPublishedArtifact() throws URISyntaxException, RemoteRepositoryCompareException {
        pom().groupId("org.hibernate").artifactId("hibernate-core").version("4.3.6.Final").create(repoFooDir);

        validationExecutor.execute(ctx);

        assertExpectedException(RemoteRepositoryCollisionException.class,
                "Remote repository [http://repo1.maven.org/maven2/] contains already artifact http://repo1.maven.org/maven2/org/hibernate/hibernate-core/4.3.6.Final/hibernate-core-4.3.6.Final.jar with different content");
        assertExpectedException(RemoteRepositoryCollisionException.class,
                "Remote repository [http://repo1.maven.org/maven2/] contains already artifact http://repo1.maven.org/maven2/org/hibernate/hibernate-core/4.3.6.Final/hibernate-core-4.3.6.Final.pom with different content");

        assertExpectedException(RemoteRepositoryCollisionException.class,
                "Remote repository [https://repository.jboss.org/nexus/content/groups/public-jboss/] contains already artifact https://repository.jboss.org/nexus/content/groups/public-jboss/org/hibernate/hibernate-core/4.3.6.Final/hibernate-core-4.3.6.Final.jar with different content");
        assertExpectedException(RemoteRepositoryCollisionException.class,
                "Remote repository [https://repository.jboss.org/nexus/content/groups/public-jboss/] contains already artifact https://repository.jboss.org/nexus/content/groups/public-jboss/org/hibernate/hibernate-core/4.3.6.Final/hibernate-core-4.3.6.Final.pom with different content");
    }

}