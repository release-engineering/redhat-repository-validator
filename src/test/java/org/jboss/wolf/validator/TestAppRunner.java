package org.jboss.wolf.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
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

public class TestAppRunner {

    private AppRunner appRunner;
    private PrintStream systemOutOriginal;
    private PrintStream systemOutDelegate;
    private StringBuffer systemOutBuffer;

    @Before
    public void init() {
        appRunner = new AppRunner();
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
        appRunner.run("-h");
        assertOutputContains("print help and exit");
    }

    @Test
    public void shouldPrintHelp2() {
        appRunner.run("--help");
        assertOutputContains("print help and exit");
    }

    @Test
    public void shouldPrintUnrecognizedOption() {
        appRunner.run("--foo", "--bar");
        assertOutputContains("Unrecognized option");
    }

    @Test(expected = BeanDefinitionStoreException.class)
    public void shouldPrintUnhandledException1() {
        appRunner.run("-c", "file-does-not-exist.xml");
        assertOutputContains("FileNotFoundException: file-does-not-exist.xml");
    }

    @Test(expected = BeanDefinitionStoreException.class)
    public void shouldPrintUnhandledException2() {
        appRunner.run("--config", "file-does-not-exist.xml");
        assertOutputContains("FileNotFoundException: file-does-not-exist.xml");
    }
    
    @Test
    public void shouldUseDefaultValidatedRepository() {
        appRunner = new AssertValidatedRepositoryRunner("workspace/validated-repository");
        appRunner.run();
    }
    
    @Test
    public void shouldUseValidatedRepositoryFromArguments() {
        appRunner = new AssertValidatedRepositoryRunner("foo-repo");
        appRunner.run("-vr", "foo-repo");
    }
    
    @Test
    public void shouldUseDefaultLocalRepository() {
        appRunner = new AssertLocalRepositoryRunner("workspace/local-repository");
        appRunner.run();
    }
    
    @Test
    public void shouldUseLocalRepositoryFromArguments() {
        appRunner = new AssertLocalRepositoryRunner("foo-local-repo");
        appRunner.run("-lr", "foo-local-repo");
    }
    
    @Test
    public void shouldUseLocalRepositoryFromConfiguration() {
        appRunner = new AssertLocalRepositoryRunner("/foo-local-repo");
        appRunner.run("-c", getClass().getResource("/TestAppRunner-localRepository.xml").getFile());
    }
    
    @Test
    public void shouldUseDefaultRemoteRepositories() {
        appRunner = new AssertRemoteRepositoryRunner("http://repo1.maven.org/maven2/");
        appRunner.run();
    }
    
    @Test
    public void shouldUseRemoteRepositoriesFromArguments() {
        appRunner = new AssertRemoteRepositoryRunner("file://foo", "file://bar", "http://repo1.maven.org/maven2/");
        appRunner.run("-rr", "file://foo", "-rr", "file://bar");
    }
    
    @Test
    public void shouldUseRemoteRepositoriesFromConfiguration() {
        appRunner = new AssertRemoteRepositoryRunner("file://foo", "http://bar.com", "http://repo1.maven.org/maven2/");
        appRunner.run("--config", getClass().getResource("/TestAppRunner-remoteRepositories.xml").getFile());
    }
    
    @Test
    public void shouldUseStubValidatorExclusively() {
        appRunner = new AppRunner() {
            @Override
            protected void runValidation() {
                Validator[] validators = validationExecutor.getValidators();
                assertEquals(validators.length, 1);
                assertTrue(validators[0] instanceof StubValidator);
            };
        };
        appRunner.run("--config", getClass().getResource("/TestAppRunner-stubValidatorExclusively.xml").getFile());
    }
    
    @Test
    public void shouldUseStubValidatorAdditionally() {
        appRunner = new AppRunner() {
            @Override
            protected void runValidation() {
                String[] validatorNames = appCtx.getBeanNamesForType(Validator.class);
                assertTrue(ArrayUtils.contains(validatorNames, "stubValidator"));

                Validator stubValidator = (Validator)appCtx.getBean("stubValidator");
                assertTrue(Arrays.asList(validationExecutor.getValidators()).contains(stubValidator));
            };
        };
        appRunner.run("--config", getClass().getResource("/TestAppRunner-stubValidatorAdditionally.xml").getFile());
    }
    
    @Test
    public void shouldSurviveUnexpectedExceptionInValidator() {
        appRunner = new AppRunner() {
            @Override
            protected void runValidation() {
                try {
                    FileUtils.forceMkdir(context.getValidatedRepository());
                } catch (IOException e) {
                    // noop
                }
                
                validationExecutor.execute(context);
                
                List<Exception> exceptions = context.getExceptions(context.getValidatedRepository());
                assertEquals(exceptions.size(), 1);
                assertEquals(exceptions.get(0).getMessage(), "stubValidator");
            };
        };
        appRunner.run("--config", getClass().getResource("/TestAppRunner-stubValidatorAdditionally.xml").getFile());
    }
    
    @Test
    public void shouldReportToFileByDefault() {
        appRunner = new AssertReportFileRunner(new File("workspace/report.txt"));
        appRunner.run();
    }

    @Test
    public void shouldRedirectDefaultReportToCustomFile1() throws IOException {
        appRunner = new AssertReportFileRunner(new File("workspace/foo.txt"));
        appRunner.run("--report", "workspace/foo.txt");
    }

    @Test
    public void shouldRedirectDefaultReportToCustomFile2() throws IOException {
        appRunner = new AssertReportFileRunner(new File("workspace/bar.txt"));
        appRunner.run("--config", getClass().getResource("/TestAppRunner-defaultReporterStream.xml").getFile());
    }

    @Test
    public void shouldNotFailWhenNoExceptionFiltersDefined() {
        appRunner = new AppRunner() {
            @Override
            protected void runValidation() {
                // make sure no filters are defined
                assertTrue(exceptionFilters == null);
                context.applyExceptionFilters(exceptionFilters);
            }
        };
        appRunner.run();
    }

    private void assertOutputContains(String s) {
        String systemOut = systemOutBuffer.toString();
        if (!systemOut.contains(s)) {
            fail("System output should contains " + s + ", but has content:\n" + systemOut);
        }
    }
    
    private static class AssertValidatedRepositoryRunner extends AppRunner {
        
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
    
    private static class AssertLocalRepositoryRunner extends AppRunner {
        
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
    
    private static class AssertRemoteRepositoryRunner extends AppRunner {

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
    
    private static class AssertReportFileRunner extends AppRunner {
        
        private final File reportFile;
        
        public AssertReportFileRunner(File reportFile) {
            this.reportFile = reportFile;
        }

        @Override
        protected void runValidation() {
            context.addException(new File("foo"), new ChecksumNotExistException(new File("foo"), "SHA-1"));
            reportingExecutor.execute(context);
            
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