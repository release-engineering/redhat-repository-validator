package org.jboss.wolf.validator;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.util.artifact.DefaultArtifactTypeRegistry;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.jboss.wolf.validator.impl.DelegatingValidator;
import org.jboss.wolf.validator.impl.DependenciesValidator;
import org.jboss.wolf.validator.impl.ModelValidator;
import org.jboss.wolf.validator.impl.aether.DepthOneOptionalDependencySelector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;

@Configuration
@ImportResource("aether-config.xml")
public class ValidatorConfig {

    @Bean
    public Validator dependenciesValidator() {
        return new DependenciesValidator();
    }

    @Bean
    public Validator modelValidator() {
        return new ModelValidator();
    }

    @Bean
    @Primary
    public Validator validator() {
        return new DelegatingValidator(
                dependenciesValidator(), 
                modelValidator());
    }

    @Bean
    public IOFileFilter fileFilter() {
        return FileFilterUtils.trueFileFilter();
    }
    
    @Bean
    public LocalRepository localRepository() {
        return new LocalRepository("target/repos/local-repo");
    }

    @Bean
    public RepositorySystemSession repositorySystemSession(RepositorySystem repositorySystem) {
        DependencySelector dependencySelector = new AndDependencySelector(
                new ScopeDependencySelector("test", "provided"),
                new DepthOneOptionalDependencySelector(),
                new ExclusionDependencySelector());

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(); // MavenRepositorySystemUtils.newSession()
        session.setDependencySelector(dependencySelector);
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepository()));
        session.setSystemProperties(System.getProperties());
        session.setConfigProperties(System.getProperties());
        session.setArtifactTypeRegistry(artifactTypeRegistry());
        return session;
    }
    
    @Bean
    public ArtifactTypeRegistry artifactTypeRegistry() {
        DefaultArtifactTypeRegistry registry = new DefaultArtifactTypeRegistry();
        registry.add(new DefaultArtifactType("pom"));
        registry.add(new DefaultArtifactType("maven-plugin", "jar", "", "java"));
        registry.add(new DefaultArtifactType("jar", "jar", "", "java"));
        registry.add(new DefaultArtifactType("ejb", "jar", "", "java"));
        registry.add(new DefaultArtifactType("ejb-client", "jar", "client", "java"));
        registry.add(new DefaultArtifactType("test-jar", "jar", "tests", "java"));
        registry.add(new DefaultArtifactType("javadoc", "jar", "javadoc", "java"));
        registry.add(new DefaultArtifactType("java-source", "jar", "sources", "java", false, false));
        registry.add(new DefaultArtifactType("war", "war", "", "java", false, true));
        registry.add(new DefaultArtifactType("ear", "ear", "", "java", false, true));
        registry.add(new DefaultArtifactType("rar", "rar", "", "java", false, true));
        registry.add(new DefaultArtifactType("par", "par", "", "java", false, true));
        registry.add(new DefaultArtifactType("bundle", "jar", "", "java"));
        return registry;
    }    

}