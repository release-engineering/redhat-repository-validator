package org.jboss.wolf.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.jboss.wolf.validator.impl.checksum.ChecksumNotExistException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.core.annotation.Order;

public class TestValidatorRunner {

    private ValidatorRunner validatorRunner;
    private PrintStream systemOutOriginal;
    private PrintStream systemOutDelegate;
    private StringBuffer systemOutBuffer;

    @Before
    public void init() {
        validatorRunner = new ValidatorRunner();
        systemOutOriginal = System.out;
        systemOutBuffer = new StringBuffer();
        systemOutDelegate = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                systemOutOriginal.write(b);
                systemOutBuffer.append((char) b);
            }
        });
        System.setOut(systemOutDelegate);
    }

    @After
    public void dispose() {
        System.setOut(systemOutOriginal);
        System.clearProperty("wolf-reportFile");
        System.clearProperty("wolf-validatedRepository");
        System.clearProperty("wolf-localRepository");
        System.clearProperty("wolf-remoteRepositories");
    }

    @Test
    public void shouldPrintHelp1() {
        validatorRunner.run("-h");
        assertOutputContaints("print help and exit");
    }

    @Test
    public void shouldPrintHelp2() {
        validatorRunner.run("--help");
        assertOutputContaints("print help and exit");
    }

    @Test
    public void shouldPrintUnrecognizedOption() {
        validatorRunner.run("--foo", "--bar");
        assertOutputContaints("Unrecognized option");
    }

    @Test(expected = BeanDefinitionStoreException.class)
    public void shouldPrintUnhandledException1() {
        validatorRunner.run("-c", "file-does-not-exist.xml");
        assertOutputContaints("FileNotFoundException: file-does-not-exist.xml");
    }

    @Test(expected = BeanDefinitionStoreException.class)
    public void shouldPrintUnhandledException2() {
        validatorRunner.run("--config", "file-does-not-exist.xml");
        assertOutputContaints("FileNotFoundException: file-does-not-exist.xml");
    }
    
    @Test
    public void shouldUseDefaultValidatedRepository() {
        validatorRunner = new AssertValidatedRepositoryRunner("workspace/validated-repository");
        validatorRunner.run();
    }
    
    @Test
    public void shouldUseValidatedRepositoryFromArguments() {
        validatorRunner = new AssertValidatedRepositoryRunner("foo-repo");
        validatorRunner.run("-vr", "foo-repo");
    }
    
    @Test
    public void shouldUseDefaultLocalRepository() {
        validatorRunner = new AssertLocalRepositoryRunner("workspace/local-repository");
        validatorRunner.run();
    }
    
    @Test
    public void shouldUseLocalRepositoryFromArguments() {
        validatorRunner = new AssertLocalRepositoryRunner("foo-local-repo");
        validatorRunner.run("-lr", "foo-local-repo");
    }
    
    @Test
    public void shouldUseLocalRepositoryFromConfiguration() {
        validatorRunner = new AssertLocalRepositoryRunner("/foo-local-repo");
        validatorRunner.run("-c", getClass().getResource("/TestValidatorRunner-localRepository.xml").getFile());
    }
    
    @Test
    public void shouldUseDefaultRemoteRepositories() {
        validatorRunner = new AssertRemoteRepositoryRunner("http://repo1.maven.org/maven2/");
        validatorRunner.run();
    }
    
    @Test
    public void shouldUseRemoteRepositoriesFromArguments() {
        validatorRunner = new AssertRemoteRepositoryRunner("file://foo", "file://bar", "http://repo1.maven.org/maven2/");
        validatorRunner.run("-rr", "file://foo", "-rr", "file://bar");
    }
    
    @Test
    public void shouldUseRemoteRepositoriesFromConfiguration() {
        validatorRunner = new AssertRemoteRepositoryRunner("file://foo", "http://bar.com", "http://repo1.maven.org/maven2/");
        validatorRunner.run("--config", getClass().getResource("/TestValidatorRunner-remoteRepositories.xml").getFile());
    }
    
    @Test
    public void shouldUseStubValidatorExclusively() {
        validatorRunner = new ValidatorRunner() {
            @Override
            protected void runValidation() {
                assertTrue(validator instanceof StubValidator);
            };
        };
        validatorRunner.run("--config", getClass().getResource("/TestValidatorRunner-stubValidatorExclusively.xml").getFile());
    }
    
    @Test
    public void shouldUseStubValidatorAdditionally() {
        validatorRunner = new ValidatorRunner() {
            @Override
            protected void runValidation() {
                String[] validatorNames = appCtx.getBeanNamesForType(Validator.class);
                assertTrue(ArrayUtils.contains(validatorNames, "stubValidator"));
            };
        };
        validatorRunner.run("--config", getClass().getResource("/TestValidatorRunner-stubValidatorAdditionally.xml").getFile());
    }
    
    @Test
    public void shouldSurviveUnexpectedExceptionInValidator() {
        validatorRunner = new ValidatorRunner() {
            @Override
            protected void runValidation() {
                try {
                    FileUtils.forceMkdir(context.getValidatedRepository());
                } catch (IOException e) {
                    // noop
                }
                
                validator.validate(context);
                
                List<Exception> exceptions = context.getExceptions(context.getValidatedRepository());
                assertEquals(exceptions.size(), 1);
                assertEquals(exceptions.get(0).getMessage(), "stubValidator");
            };
        };
        validatorRunner.run("--config", getClass().getResource("/TestValidatorRunner-stubValidatorAdditionally.xml").getFile());
    }
    
    @Test
    public void shouldReportToFileByDefault() {
        validatorRunner = new AssertReportFileRunner(new File("workspace/report.txt"));
        validatorRunner.run();
    }

    @Test
    public void shouldRedirectDefaultReportToCustomFile1() throws IOException {
        validatorRunner = new AssertReportFileRunner(new File("workspace/foo.txt"));
        validatorRunner.run("--report", "workspace/foo.txt");
    }

    @Test
    public void shouldRedirectDefaultReportToCustomFile2() throws IOException {
        validatorRunner = new AssertReportFileRunner(new File("workspace/bar.txt"));
        validatorRunner.run("--config", getClass().getResource("/TestValidatorRunner-defaultReporterStream.xml").getFile());
    }
    
    private void assertOutputContaints(String s) {
        String systemOut = systemOutBuffer.toString();
        if (!systemOut.contains(s)) {
            fail("System output should contains " + s + ", but has content:\n" + systemOut);
        }
    }
    
    private static class AssertValidatedRepositoryRunner extends ValidatorRunner {
        
        private final String expectedValidatedRepository;

        private AssertValidatedRepositoryRunner(String expectedValidatedRepository) {
            this.expectedValidatedRepository = expectedValidatedRepository;
        }
        
        @Override
        protected void runValidation() {
            ValidatorContext validatorContext = appCtx.getBean(ValidatorContext.class);
            assertEquals(expectedValidatedRepository, validatorContext.getValidatedRepository().getPath());
        }
        
    }
    
    private static class AssertLocalRepositoryRunner extends ValidatorRunner {
        
        private final String expectedLocalRepository;

        private AssertLocalRepositoryRunner(String expectedLocalRepository) {
            this.expectedLocalRepository = expectedLocalRepository;
        }
        
        @Override
        protected void runValidation() {
            LocalRepository localRepository = appCtx.getBean(LocalRepository.class);
            RepositorySystemSession repositorySystemSession = appCtx.getBean(RepositorySystemSession.class);
            assertEquals(expectedLocalRepository, localRepository.getBasedir().getPath());
            assertEquals(repositorySystemSession.getLocalRepository().getBasedir().getAbsolutePath(), localRepository.getBasedir().getAbsolutePath());
        }        
        
    }
    
    private static class AssertRemoteRepositoryRunner extends ValidatorRunner {

        private final String[] expectedRemoteRepositories;

        private AssertRemoteRepositoryRunner(String... expectedRemoteRepositories) {
            this.expectedRemoteRepositories = expectedRemoteRepositories;
        }

        @Override
        protected void runValidation() {
            ValidatorContext validatorContext = appCtx.getBean(ValidatorContext.class);
            File validatedRepository = validatorContext.getValidatedRepository();
            List<RemoteRepository> remoteRepositories = validatorContext.getRemoteRepositories();
            
            assertEquals(validatedRepository.toURI().toString(), remoteRepositories.get(0).getUrl());
            assertEquals(expectedRemoteRepositories.length+1, remoteRepositories.size());
            for (int i = 1; i < expectedRemoteRepositories.length; i++) {
                assertEquals(expectedRemoteRepositories[i-1], remoteRepositories.get(i).getUrl());
            }
        }

    }
    
    private static class AssertReportFileRunner extends ValidatorRunner {
        
        private final File reportFile;
        
        public AssertReportFileRunner(File reportFile) {
            this.reportFile = reportFile;
        }

        @Override
        protected void runValidation() {
            context.addException(new File("foo"), new ChecksumNotExistException(new File("foo"), "SHA-1"));
            reporter.report(context);
            
            try {
                assertTrue(reportFile.exists());
                assertTrue(reportFile.isFile());
                
                String reportContent = FileUtils.readFileToString(reportFile);
                assertTrue(reportContent.contains("ChecksumNotExistException (total count 1)"));
                assertTrue(reportContent.contains("Checksum SHA-1 for file foo not exist"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        
    }
    
    @Order(1)
    public static class StubValidator implements Validator {

        @Override
        public void validate(ValidatorContext ctx) {
            throw new RuntimeException("stubValidator");
        }

    }

}