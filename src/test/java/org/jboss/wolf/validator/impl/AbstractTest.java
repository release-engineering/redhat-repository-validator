package org.jboss.wolf.validator.impl;

import static org.jboss.wolf.validator.impl.TestUtil.containsExceptionMessage;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorConfig;
import org.jboss.wolf.validator.ValidatorContext;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public abstract class AbstractTest {
    
    protected static final File reposDir = new File("target/repos");
    protected static final File repoLocalDir = new File(reposDir, "local-repo");
    protected static final File repoFooDir = new File(reposDir, "remote-repo-foo");
    protected static final File repoBarDir = new File(reposDir, "remote-repo-bar");
    protected static final File repoBazDir = new File(reposDir, "remote-repo-baz");
    protected static final File distributionDir = new File("target/distribution");
    
    protected static final RemoteRepository remoteRepoFoo = new RemoteRepository.Builder("foo", "default", repoFooDir.toURI().toString()).build();
    protected static final RemoteRepository remoteRepoBar = new RemoteRepository.Builder("bar", "default", repoBarDir.toURI().toString()).build();
    protected static final RemoteRepository remoteRepoBaz = new RemoteRepository.Builder("baz", "default", repoBazDir.toURI().toString()).build();
    protected static final RemoteRepository remoteRepoCentral = new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/").build();
    protected static final List<RemoteRepository> remoteRepos = Arrays.asList(remoteRepoFoo, remoteRepoBar, remoteRepoBaz, remoteRepoCentral);
    
    protected static IOFileFilter fileFilter;

    static {
        System.setProperty("wolf-distribution", distributionDir.getPath());
    }

    @Inject
    protected Validator validator;
    @Inject
    protected ValidatorContext ctx;

    @Before
    public final void init() {
        initRepositories();
        fileFilter = null;
    }

    private void initRepositories() {
        try {
            FileUtils.deleteDirectory(reposDir);
            FileUtils.deleteDirectory(distributionDir);
            FileUtils.forceMkdir(reposDir);
            FileUtils.forceMkdir(repoLocalDir);
            FileUtils.forceMkdir(repoFooDir);
            FileUtils.forceMkdir(repoBarDir);
            FileUtils.forceMkdir(repoBazDir);
            FileUtils.forceMkdir(distributionDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    protected void assertSuccess() {
        if (!ctx.isSuccess()) {
            logExceptions(ctx);
        }
        assertTrue(ctx.isSuccess());
    }
    
    protected void assertLocalRepoContains(Model model) {
        String dir = model.getGroupId().replace('.', '/') + "/" + model.getArtifactId() + "/" + model.getVersion() + "/";
        String pomFile = dir + model.getArtifactId() + "-" + model.getVersion() + ".pom";
        assertLocalRepoContains(pomFile);

        if (!model.getPackaging().equals("pom")) {
            String archiveFile = dir + model.getArtifactId() + "-" + model.getVersion() + "." + model.getPackaging();
            assertLocalRepoContains(archiveFile);
        }
    }
    
    protected void assertLocalRepoContains(String filePath) {
        File f = new File(repoLocalDir, filePath);
        assertTrue("Local repository should contains file: " + filePath, f.exists());
    }
    
    protected void assertExpectedException(Class<? extends Exception> exceptionType, String exceptionMessage) {
        for (Exception e : ctx.getExceptions()) {
            if (containsExceptionMessage(e, exceptionType, exceptionMessage)) {
                return;
            }
        }
        logExceptions(ctx);
        fail("Expected exception " + exceptionType.getSimpleName() + " with message : " + exceptionMessage);
    }
    
    public static void logExceptions(ValidatorContext ctx) {
        List<Exception> exceptions = ctx.getExceptions();
        for (Exception exception : exceptions) {
            logException(exception, 0);
        }
    }
    
    public static void logException(Throwable e, int depth) {
        String log = "";
        log += StringUtils.repeat(" ", depth*4);
        log += e.getClass().getSimpleName();
        log += " ";
        log += e.getMessage();
        System.out.println(log);
        
        if( e.getCause() != null ) {
            logException(e.getCause(), depth+1);
        }
    }

    @Configuration
    @ImportResource("wolf-validator-app-context.xml")
    public static class TestConfiguration extends ValidatorConfig {

        @Bean
        @Override
        public IOFileFilter defaultFilter() {
            return FileFilterUtils.falseFileFilter();
        }
        
        @Bean
        @Scope(SCOPE_PROTOTYPE)
        @Override
        public ValidatorContext validatorContext() {
            return new ValidatorContext(repoFooDir, remoteRepos);
        }
        
        @Bean
        @Override
        public LocalRepository localRepository() {
            return new LocalRepository(repoLocalDir);
        }

    }

    public static class TestFileFilter implements IOFileFilter {

        @Override
        public boolean accept(File file) {
            return fileFilter != null ? fileFilter.accept(file) : true;
        }

        @Override
        public boolean accept(File dir, String name) {
            return fileFilter != null ? fileFilter.accept(dir, name) : true;
        }
    }
    
}