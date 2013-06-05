package org.jboss.wolf.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ApplicationContext;

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
    public void shouldOverrideLocalRepository() {
        URL configFile = getClass().getResource("/TestValidatorRunner-overrideLocalRepository.xml");

        validatorRunner = new ValidatorRunner() {
            @Override
            protected void runValidation(ApplicationContext ctx) {
                LocalRepository localRepository = ctx.getBean("localRepository", LocalRepository.class);
                RepositorySystemSession repositorySystemSession = ctx.getBean("repositorySystemSession", RepositorySystemSession.class);

                assertNotNull(localRepository);
                assertNotNull(repositorySystemSession);
                assertEquals("/foo-local-repo", localRepository.getBasedir().getPath());
                assertEquals(repositorySystemSession.getLocalRepository().getBasedir().getPath(), localRepository.getBasedir().getPath());
            }
        };
        validatorRunner.run("-c", configFile.getFile());
    }

    private void assertOutputContaints(String s) {
        String systemOut = systemOutBuffer.toString();
        if (!systemOut.contains(s)) {
            fail("System output should contains " + s + ", but has content:\n" + systemOut);
        }
    }

}