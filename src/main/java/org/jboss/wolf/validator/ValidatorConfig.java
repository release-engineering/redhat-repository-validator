package org.jboss.wolf.validator;

import static org.apache.commons.io.IOCase.INSENSITIVE;
import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.nameFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.notFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;
import static org.springframework.core.annotation.AnnotationAwareOrderComparator.sort;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.inject.Named;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.artifact.DefaultArtifactTypeRegistry;
import org.eclipse.aether.util.graph.manager.ClassicDependencyManager;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.JavaScopeDeriver;
import org.eclipse.aether.util.graph.transformer.JavaScopeSelector;
import org.eclipse.aether.util.graph.transformer.NearestVersionSelector;
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;
import org.jboss.wolf.validator.impl.bom.BomFilter;
import org.jboss.wolf.validator.impl.bom.BomFilterSimple;
import org.jboss.wolf.validator.internal.DepthOneOptionalDependencySelector;
import org.jboss.wolf.validator.internal.LocalRepositoryModelResolver;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

@Configuration
@ComponentScan(
        useDefaultFilters = false, 
        includeFilters = @Filter(value = Named.class))
public class ValidatorConfig {
    
    @Autowired
    private BeanFactory beanFactory;
    
    @Value("#{systemProperties['wolf-validatedRepository']?:'workspace/validated-repository'}")
    private String validatedRepository;

    @Value("#{systemProperties['wolf-localRepository']?:'workspace/local-repository'}")
    private String localRepository;

    @Value("#{systemProperties['wolf-remoteRepositories']?.split(';')}")
    private String[] remoteRepositories;
    
    @Bean
    @Primary
    public Validator validator(final Validator[] validators) {
        sort(validators);
        return new Validator() {
            @Override
            public void validate(ValidatorContext ctx) {
                for (Validator validator : validators) {
                    validator.validate(ctx);
                }
            }
        };
    }
    
    @Bean
    @Primary
    public Reporter reporter(final Reporter[] reporters) {
        sort(reporters);
        return new Reporter() {
            @Override
            public void report(ValidatorContext ctx) {
                for (Reporter reporter : reporters) {
                    reporter.report(ctx);
                }
            }
        };
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
    public BomFilter bomFilter() {
        return new BomFilterSimple();
    }

    @Bean
    public IOFileFilter defaultFilter() {
        return trueFileFilter();
    }

    @Bean
    public IOFileFilter dependenciesValidatorFilter() {
        return defaultFilter();
    }
    
    @Bean
    public IOFileFilter modelValidatorFilter() {
        return defaultFilter();
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
    public IOFileFilter bomAmbiguousVersionValidatorFilter() {
        return defaultFilter();
    }

    @Bean
    public IOFileFilter bomDependencyNotFoundValidatorFilter() {
        return defaultFilter();
    }

    @Bean
    public IOFileFilter bomUnmanagedVersionValidatorFilter() {
        return defaultFilter();
    }
    
    @Bean
    public IOFileFilter versionPatternValidatorFilter() {
        return defaultFilter();
    }
    
    @Bean
    public IOFileFilter versionAmbiguityValidatorFilter() {
        return defaultFilter();
    }

    @Bean
    public PrintStream defaultReporterStream() {
        return System.out;
    }
    
    @Bean
    public PrintStream dependencyNotFoundReporterStream() {
        return defaultReporterStream();
    }

    @Bean
    public PrintStream checksumReporterStream() {
        return defaultReporterStream();
    }

    @Bean
    public PrintStream unprocessedExceptionsReporterStream() {
        return defaultReporterStream();
    }

    @Bean
    public LocalRepository localRepository() {
        return new LocalRepository(localRepository);
    }

    @Bean
    public RepositorySystemSession repositorySystemSession(RepositorySystem repositorySystem) {
        // see MavenRepositorySystemUtils.newSession()

        DependencySelector selector = new AndDependencySelector(
                new ScopeDependencySelector("test", "provided"),
                new DepthOneOptionalDependencySelector(),
                new ExclusionDependencySelector());

        DependencyGraphTransformer transformer = new ConflictResolver(
                new NearestVersionSelector(), 
                new JavaScopeSelector(),
                new SimpleOptionalitySelector(), 
                new JavaScopeDeriver());
        
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepository()));
        session.setSystemProperties(System.getProperties());
        session.setConfigProperties(System.getProperties());
        session.setArtifactTypeRegistry(artifactTypeRegistry());
        session.setDependencyManager(new ClassicDependencyManager());
        session.setArtifactDescriptorPolicy(new SimpleArtifactDescriptorPolicy(true, true));
        session.setDependencySelector(selector);
        session.setDependencyGraphTransformer(transformer);

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
    public ModelBuildingRequest modelBuildingRequestTemplate(RepositorySystemSession repositorySystemSession, LocalRepositoryModelResolver localRepositoryModelResolver) {
        Properties userProperties = new Properties();
        userProperties.putAll(repositorySystemSession.getUserProperties());

        Properties systemProperties = new Properties();
        systemProperties.putAll(repositorySystemSession.getSystemProperties());

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setProcessPlugins(true);
        request.setLocationTracking(true);
        request.setModelResolver(localRepositoryModelResolver);
        request.setUserProperties(userProperties);
        request.setSystemProperties(systemProperties);

        return request;
    }

}