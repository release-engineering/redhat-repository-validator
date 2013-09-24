package org.jboss.wolf.validator;

import static org.apache.commons.io.filefilter.FileFilterUtils.*;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;
import static org.springframework.core.annotation.AnnotationAwareOrderComparator.sort;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.inject.Named;

import org.apache.commons.io.FileUtils;
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
import org.jboss.wolf.validator.internal.DepthOneOptionalDependencySelector;
import org.jboss.wolf.validator.internal.LocalRepositoryModelResolver;
import org.jboss.wolf.validator.internal.LogOutputStream;
import org.jboss.wolf.validator.internal.LogRepositoryListener;
import org.jboss.wolf.validator.internal.LogTransferListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(Validator.class);

    @Autowired
    private BeanFactory beanFactory;

    @Value("#{systemProperties['wolf-reportFile']?:'workspace/report.txt'}")
    private String reportFileName;

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
                    logger.debug("starting {}", validator.getClass().getSimpleName());
                    try {
                        validator.validate(ctx);
                    } catch (RuntimeException e) {
                        logger.error("validator " + validator.getClass().getSimpleName() + " ended with unexpected exception!", e);
                        ctx.addException(ctx.getValidatedRepository(), e);
                    }
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
                    try {
                        reporter.report(ctx);
                    } catch (RuntimeException e) {
                        logger.error("reporter " + reporter.getClass().getSimpleName() + " ended with unexpected exception!", e);
                    }
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
        return new ValidatorContext(new File(validatedRepository), effectiveRemoteRepositories());
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
                "jbosseula.txt"};
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
    public PrintStream defaultReporterStream() {
        try {
            File reportFile = new File(reportFileName);
            FileUtils.forceMkdir(reportFile.getParentFile());
            FileUtils.touch(reportFile);
            FileOutputStream fileOutputStream = new FileOutputStream(reportFile);
            LogOutputStream logOutputStream = new LogOutputStream(Reporter.class.getSimpleName(), fileOutputStream);
            PrintStream printStream = new PrintStream(logOutputStream);
            return printStream;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public PrintStream dependencyNotFoundReporterStream() {
        return defaultReporterStream();
    }

    @Bean
    public PrintStream bomDependencyNotFoundReporterStream() {
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