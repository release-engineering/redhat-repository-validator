package com.redhat.repository.validator.impl.remoterepository;

import static com.redhat.repository.validator.impl.TestUtil.pom;

import java.net.URISyntaxException;

import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import com.redhat.repository.validator.impl.AbstractTest;
import com.redhat.repository.validator.impl.remoterepository.ChecksumProviderNexus;
import com.redhat.repository.validator.impl.remoterepository.ChecksumProviderNginx;
import com.redhat.repository.validator.impl.remoterepository.RemoteRepositoryCollisionException;
import com.redhat.repository.validator.impl.remoterepository.RemoteRepositoryCollisionValidator;
import com.redhat.repository.validator.impl.remoterepository.RemoteRepositoryCompareException;

@ContextConfiguration
public class TestRemoteRepositoryCollisionValidator extends AbstractTest {

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RemoteRepositoryCollisionValidator collisionValidatorMavenCentral() {
            return new RemoteRepositoryCollisionValidator("https://repo1.maven.org/maven2/", new ChecksumProviderNginx());
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
                "Remote repository [https://repo1.maven.org/maven2/] contains already artifact https://repo1.maven.org/maven2/org/hibernate/hibernate-core/4.3.6.Final/hibernate-core-4.3.6.Final.jar with different content");
        assertExpectedException(RemoteRepositoryCollisionException.class,
                "Remote repository [https://repo1.maven.org/maven2/] contains already artifact https://repo1.maven.org/maven2/org/hibernate/hibernate-core/4.3.6.Final/hibernate-core-4.3.6.Final.pom with different content");

        assertExpectedException(RemoteRepositoryCollisionException.class,
                "Remote repository [https://repository.jboss.org/nexus/content/groups/public-jboss/] contains already artifact https://repository.jboss.org/nexus/content/groups/public-jboss/org/hibernate/hibernate-core/4.3.6.Final/hibernate-core-4.3.6.Final.jar with different content");
        assertExpectedException(RemoteRepositoryCollisionException.class,
                "Remote repository [https://repository.jboss.org/nexus/content/groups/public-jboss/] contains already artifact https://repository.jboss.org/nexus/content/groups/public-jboss/org/hibernate/hibernate-core/4.3.6.Final/hibernate-core-4.3.6.Final.pom with different content");
    }

}