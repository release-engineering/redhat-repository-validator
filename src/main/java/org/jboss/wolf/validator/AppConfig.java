package org.jboss.wolf.validator;

import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.falseFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.notFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.inject.Named;

import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.eclipse.aether.ConfigurationProperties;
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
import org.jboss.wolf.validator.impl.remoterepository.ChecksumProviderNexus;
import org.jboss.wolf.validator.impl.remoterepository.ChecksumProviderNginx;
import org.jboss.wolf.validator.impl.remoterepository.RemoteRepositoryCollisionValidator;
import org.jboss.wolf.validator.internal.DepthOneOptionalDependencySelector;
import org.jboss.wolf.validator.internal.LocalRepositoryModelResolver;
import org.jboss.wolf.validator.internal.LogRepositoryListener;
import org.jboss.wolf.validator.internal.LogTransferListener;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@ComponentScan(
        useDefaultFilters = false,
        includeFilters = @Filter(value = Named.class))
public class AppConfig {

    @Autowired
    private BeanFactory beanFactory;

    @Value("#{systemProperties['wolf-validatedRepository']?:'workspace/validated-repository'}")
    private String validatedRepository;
    
    @Value("#{systemProperties['wolf-validatedDistribution']?:'workspace/validated-distribution'}")
    private String validatedDistribution;

    @Value("#{systemProperties['wolf-localRepository']?:'workspace/local-repository'}")
    private String localRepository;

    @Value("#{systemProperties['wolf-remoteRepositories']?.split(';')}")
    private String[] remoteRepositories;
    
    @Autowired(required = false)
    private ExceptionFilter[] exceptionFilters;

    @Bean
    public ValidationExecutor validationExecutor(Validator[] validators) {
        return new ValidationExecutor(validators);
    }

    @Bean
    public ReportingExecutor reportingExecutor(Reporter[] reporters) {
        return new ReportingExecutor(reporters);
    }

    @Bean
    public RemoteRepository centralRemoteRepository() {
        RemoteRepository remoteRepositoryCentral = new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/").build();
        return remoteRepositoryCentral;
    }

    @Bean
    public List<RemoteRepository> effectiveRemoteRepositories() {
        RemoteRepository validatedRemoteRepository = new RemoteRepository.Builder("validated", "default", new File(validatedRepository).toURI().toString()).build();

        List<RemoteRepository> repositories = new ArrayList<RemoteRepository>();
        repositories.add(validatedRemoteRepository);
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
        return new ValidatorContext(
                  new File(validatedRepository),
                  new File(validatedDistribution),
                  effectiveRemoteRepositories(),
                  Arrays.asList(exceptionFilters != null ? exceptionFilters : new ExceptionFilter[]{}));
    }

    @Bean
    public BomFilter bomFilter() {
        return new BomFilterSimple();
    }

    @Bean
    public String[] expectedRootFiles() {
        return new String[]{
                "example-settings.xml",
                "readme.txt",
                "readme.md",
                "jbosseula.txt",
                ".maven-repository"};
    }

    @Bean
    public IOFileFilter expectedRootFilesFilter() {
        final String validatedRepositoryAbsolutePath = validatorContext().getValidatedRepository().getAbsolutePath();
        final String[] expectedRootFiles = expectedRootFiles();

        IOFileFilter expectedRootFileFilter = new AbstractFileFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (dir.getAbsolutePath().equals(validatedRepositoryAbsolutePath) &&
                        ArrayUtils.contains(expectedRootFiles, name.toLowerCase())) {
                    return true;
                }
                return false;
            }
        };

        return expectedRootFileFilter;
    }
    
    @Bean
    public RemoteRepositoryCollisionValidator collisionValidatorMavenCentral() {
        return new RemoteRepositoryCollisionValidator("http://repo1.maven.org/maven2/", new ChecksumProviderNginx(), collisionValidatorMavenCentralFilter(), 20);
    }
    
    @Bean
    public RemoteRepositoryCollisionValidator collisionValidatorJBossNexus() {
        return new RemoteRepositoryCollisionValidator("https://repository.jboss.org/nexus/content/groups/public-jboss/", new ChecksumProviderNexus(), collisionValidatorJBossNexusFilter(), 20);
    }
    
    @Bean
    public IOFileFilter collisionValidatorMavenCentralFilter() {
        return defaultFilter();
    }

    @Bean
    public IOFileFilter collisionValidatorJBossNexusFilter() {
        return defaultFilter();
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
    public IOFileFilter suspiciousFileValidatorFilter() {
        return and(defaultFilter(), notFileFilter(expectedRootFilesFilter()));
    }

    @Bean
    public IOFileFilter checksumValidatorFilter() {
        return and(defaultFilter(), notFileFilter(expectedRootFilesFilter()));
    }
    
    @Bean
    public IOFileFilter bestPracticesValidatorFilter() {
        return defaultFilter();
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
    public IOFileFilter bomVersionPropertyValidatorFilter() {
        return defaultFilter();
    }
    
    @Bean
    public IOFileFilter jarSignatureValidatorFilter() {
        return defaultFilter();
    }

    @Bean
    public IOFileFilter xmlFileValidatorFilter() {
        return defaultFilter();
    }

    @Bean
    public IOFileFilter jarSourcesValidatorFilter() {
        return defaultFilter();
    }

    @Bean
    public IOFileFilter distributionValidatorFilter() {
        return defaultFilter();
    }

    @Bean
    public IOFileFilter versionAmbiguityValidatorFilter() {
        return defaultFilter();
    }

    @Bean
    public IOFileFilter versionOverlapValidatorFilter() {
        return defaultFilter();
    }

    @Bean
    public IOFileFilter versionPatternValidatorFilter() {
        return defaultFilter();
    }
    
    @Bean
    public IOFileFilter osgiVersionValidatorFilter() {
        return falseFileFilter();
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
        session.setTransferListener(new LogTransferListener());
        session.setRepositoryListener(new LogRepositoryListener());

        if (!session.getConfigProperties().containsKey(ConfigurationProperties.REQUEST_TIMEOUT)) {
            session.setConfigProperty(ConfigurationProperties.REQUEST_TIMEOUT, 3 * 60 * 1000);
        }

        return session;
    }

    @Bean
    public ArtifactTypeRegistry artifactTypeRegistry() {
        DefaultArtifactTypeRegistry registry = new DefaultArtifactTypeRegistry();
        registry.add(new DefaultArtifactType("pom"));
        registry.add(new DefaultArtifactType("maven-plugin", "jar", "", "java"));
        registry.add(new DefaultArtifactType("maven-archetype", "jar", "", "java"));
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
        registry.add(new DefaultArtifactType("zip", "zip", "", ""));
        registry.add(new DefaultArtifactType("aar", "aar", "", "java"));
        registry.add(new DefaultArtifactType("apklib", "apklib", "", "java"));
        registry.add(new DefaultArtifactType("eclipse-plugin", "jar", "", "java"));
        registry.add(new DefaultArtifactType("eclipse-test-plugin", "jar", "", "java"));
        registry.add(new DefaultArtifactType("eclipse-feature", "jar", "", "java", false, false));
        registry.add(new DefaultArtifactType("eclipse-update-site", "zip", "", "java", false, false));
        registry.add(new DefaultArtifactType("eclipse-application", "zip", "", "java", false, false));
        registry.add(new DefaultArtifactType("eclipse-repository", "zip", "", "java", false, false));
        registry.add(new DefaultArtifactType("eclipse-target-definition", "target", "", "java", false, false));
        registry.add(new DefaultArtifactType("p2-installable-unit", "zip", "", "java", false, false));
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
