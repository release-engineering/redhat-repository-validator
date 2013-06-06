package org.jboss.wolf.validator;

import static org.apache.commons.io.IOCase.INSENSITIVE;
import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.nameFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.notFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.artifact.DefaultArtifactTypeRegistry;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.jboss.wolf.validator.impl.BomAmbiguousVersionValidator;
import org.jboss.wolf.validator.impl.BomDependencyNotFoundValidator;
import org.jboss.wolf.validator.impl.BomFilter;
import org.jboss.wolf.validator.impl.BomFilterSimple;
import org.jboss.wolf.validator.impl.BomUnmanagedVersionValidator;
import org.jboss.wolf.validator.impl.ChecksumValidator;
import org.jboss.wolf.validator.impl.DelegatingValidator;
import org.jboss.wolf.validator.impl.DependenciesValidator;
import org.jboss.wolf.validator.impl.ModelValidator;
import org.jboss.wolf.validator.impl.ValidatorSupport;
import org.jboss.wolf.validator.impl.aether.DepthOneOptionalDependencySelector;
import org.jboss.wolf.validator.impl.aether.LocalRepositoryModelResolver;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

@Configuration
public class ValidatorConfig {
    
    @Autowired
    private BeanFactory beanFactory;
    
    @Value("#{systemProperties['validatedRepository']}")
    private String validatedRepository;

    @Value("#{systemProperties['localRepository']}")
    private String localRepository;

    @Value("#{systemProperties['remoteRepositories']?.split(';')}")
    private String[] remoteRepositories;

    @Bean
    @Primary
    public Validator validator() {
        return new DelegatingValidator(
                dependenciesValidator(),
                modelValidator(),
                checksumValidator(),
                bomDependencyNotFoundValidator(),
                bomAmbiguousVersionValidator(),
                bomUnmanagedVersionValidator());
    }

    @Bean
    public RemoteRepository centralRemoteRepository() {
        RemoteRepository remoteRepositoryCentral = new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/").build();
        return remoteRepositoryCentral;
    }

    @Bean
    public List<RemoteRepository> effectiveRemoteRepositories() {
        List<RemoteRepository> repositories = new ArrayList<RemoteRepository>();
        repositories.addAll(remoteRepositoriesFromArguments());
        repositories.addAll(remoteRepositoriesFromConfiguration());
        repositories.add(centralRemoteRepository());
        return Collections.unmodifiableList(repositories);
    }
    
    private List<RemoteRepository> remoteRepositoriesFromArguments() {
        List<RemoteRepository> repositories = new ArrayList<RemoteRepository>();
        if (remoteRepositories != null) {
            for (int i = 0; i < remoteRepositories.length; i++) {
                String remoteRepository = remoteRepositories[i];
                if (StringUtils.isNotEmpty(remoteRepository)) {
                    repositories.add(new RemoteRepository.Builder("remote" + i, "default", remoteRepository).build());
                }
            }
        }
        return repositories;
    }
    
    private List<RemoteRepository> remoteRepositoriesFromConfiguration() {
        List<RemoteRepository> repositories = new ArrayList<RemoteRepository>();
        if (beanFactory.containsBean("remoteRepositories")) {
            @SuppressWarnings("unchecked")
            List<RemoteRepository> customRemoteRepositories = beanFactory.getBean("remoteRepositories", List.class);
            repositories.addAll(customRemoteRepositories);
        }
        return repositories;
    }
    
    @Bean
    @Scope(SCOPE_PROTOTYPE)
    public ValidatorContext validatorContext() {
        return new ValidatorContext(new File(validatedRepository), effectiveRemoteRepositories());
    }

    @Bean
    public ValidatorSupport validatorSupport() {
        return new ValidatorSupport();
    }

    @Bean
    public BomFilter bomFilter() {
        return new BomFilterSimple();
    }

    @Bean
    public IOFileFilter defaultFilter() {
        return trueFileFilter();
    }

    @Bean
    public Validator dependenciesValidator() {
        return new DependenciesValidator();
    }

    @Bean
    public IOFileFilter dependenciesValidatorFilter() {
        return defaultFilter();
    }

    @Bean
    public Validator modelValidator() {
        return new ModelValidator();
    }

    @Bean
    public IOFileFilter modelValidatorFilter() {
        return defaultFilter();
    }

    @Bean
    public Validator checksumValidator() {
        return new ChecksumValidator();
    }

    @Bean
    public IOFileFilter checksumValidatorFilter() {
        return and(
                defaultFilter(), 
                notFileFilter(nameFileFilter("example-settings.xml", INSENSITIVE)),
                notFileFilter(nameFileFilter("readme.txt", INSENSITIVE)),
                notFileFilter(nameFileFilter("readme.md", INSENSITIVE)));
    }

    @Bean
    public Validator bomAmbiguousVersionValidator() {
        return new BomAmbiguousVersionValidator();
    }

    @Bean
    public IOFileFilter bomAmbiguousVersionValidatorFilter() {
        return defaultFilter();
    }

    @Bean
    public Validator bomDependencyNotFoundValidator() {
        return new BomDependencyNotFoundValidator();
    }

    @Bean
    public IOFileFilter bomDependencyNotFoundValidatorFilter() {
        return defaultFilter();
    }

    @Bean
    public Validator bomUnmanagedVersionValidator() {
        return new BomUnmanagedVersionValidator();
    }

    @Bean
    public IOFileFilter bomUnmanagedVersionValidatorFilter() {
        return defaultFilter();
    }

    @Bean
    public LocalRepository localRepository() {
        return new LocalRepository(localRepository);
    }

    @Bean
    public LocalRepositoryModelResolver localRepositoryModelResolver() {
        return new LocalRepositoryModelResolver();
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

    @Bean
    public ModelBuildingRequest modelBuildingRequestTemplate(RepositorySystemSession repositorySystemSession) {
        Properties userProperties = new Properties();
        userProperties.putAll(repositorySystemSession.getUserProperties());

        Properties systemProperties = new Properties();
        systemProperties.putAll(repositorySystemSession.getSystemProperties());

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setProcessPlugins(true);
        request.setLocationTracking(true);
        request.setModelResolver(localRepositoryModelResolver());
        request.setUserProperties(userProperties);
        request.setSystemProperties(systemProperties);

        return request;
    }

}