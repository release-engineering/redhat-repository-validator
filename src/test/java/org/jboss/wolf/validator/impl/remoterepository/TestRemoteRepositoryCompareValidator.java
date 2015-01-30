package org.jboss.wolf.validator.impl.remoterepository;

import static org.jboss.wolf.validator.impl.TestUtil.pom;

import java.net.URISyntaxException;

import org.jboss.wolf.validator.impl.AbstractTest;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration
public class TestRemoteRepositoryCompareValidator extends AbstractTest {
    
    @Configuration
    public static class TestConfiguration {

        @Bean
        public RemoteRepositoryCompareValidator remoteRepositoryCompareValidator() {
            return new RemoteRepositoryCompareValidator("http://repository.jboss.org/nexus/content/groups/public/", new ChecksumProviderNexus());
        }
        
    }
    
    @Test
    public void shouldFindMissingArtifact() throws URISyntaxException, RemoteRepositoryCompareException {
        pom().artifactId("foo").create(repoFooDir);
        
        validationExecutor.execute(ctx);
        
        assertExpectedException(RemoteRepositoryCompareException.class, "Remote repository [http://repository.jboss.org/nexus/content/groups/public/] doesn't contains artifact http://repository.jboss.org/nexus/content/groups/public/com/acme/foo/1.0/foo-1.0.pom");
        assertExpectedException(RemoteRepositoryCompareException.class, "Remote repository [http://repository.jboss.org/nexus/content/groups/public/] doesn't contains artifact http://repository.jboss.org/nexus/content/groups/public/com/acme/foo/1.0/foo-1.0.jar");
    }
    
    @Test
    public void shouldFindDifferentArtifact() throws URISyntaxException, RemoteRepositoryCompareException {
        pom().groupId("org.hibernate").artifactId("hibernate-core").version("4.3.6.Final").create(repoFooDir);
        
        validationExecutor.execute(ctx);
        
        assertExpectedException(RemoteRepositoryCompareException.class, "Remote repository [http://repository.jboss.org/nexus/content/groups/public/] contains different binary data for artifact http://repository.jboss.org/nexus/content/groups/public/org/hibernate/hibernate-core/4.3.6.Final/hibernate-core-4.3.6.Final.pom");
        assertExpectedException(RemoteRepositoryCompareException.class, "Remote repository [http://repository.jboss.org/nexus/content/groups/public/] contains different binary data for artifact http://repository.jboss.org/nexus/content/groups/public/org/hibernate/hibernate-core/4.3.6.Final/hibernate-core-4.3.6.Final.jar");
    }    
    
}