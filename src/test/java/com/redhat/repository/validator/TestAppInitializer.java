package com.redhat.repository.validator;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.redhat.repository.validator.AppConfig;
import com.redhat.repository.validator.AppInitializer;
import com.redhat.repository.validator.ValidatorContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class TestAppInitializer {

    @Configuration
    @ImportResource("redhat-repository-validator-app-context.xml")
    public static class TestConfiguration extends AppConfig {
    }

    @Inject
    private ValidatorContext ctx;
    @Inject
    private AppInitializer initializer;
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    private final File workspace = new File("workspace");
    private final File localRepository = new File(workspace, "local-repository");
    private final File validatedRepository = new File(workspace, "validated-repository");

    @Before
    public void init() throws IOException {
        FileUtils.deleteQuietly(workspace);
        FileUtils.forceMkdir(localRepository);
        FileUtils.forceMkdir(validatedRepository);
        FileUtils.touch(new File(validatedRepository, "empty.txt"));
    }
    
    @After
    public void cleanup() throws IOException {
        FileUtils.deleteQuietly(workspace);
    }

    @Test
    public void shouldSuccess() {
        initializer.initialize(ctx);
    }

    @Test
    public void shouldCreateLocalRepositoryWhenNotExist() throws IOException {
        FileUtils.forceDelete(localRepository);

        initializer.initialize(ctx);

        assertTrue(localRepository.exists());
        assertTrue(localRepository.isDirectory());
    }

    @Test
    public void shouldFailWhenLocalRepositoryIsFile() throws IOException {
        FileUtils.forceDelete(localRepository);
        FileUtils.touch(localRepository);

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("Local repository workspace/local-repository isn't directory");

        initializer.initialize(ctx);
    }

    @Test
    public void shouldFailWhenValidatedRepositoryNotExist() throws IOException {
        FileUtils.forceDelete(validatedRepository);

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("Validated repository workspace/validated-repository doesn't exist");

        initializer.initialize(ctx);
    }

    @Test
    public void shouldFailWhenValidatedRepositoryIsFile() throws IOException {
        FileUtils.forceDelete(validatedRepository);
        FileUtils.touch(validatedRepository);

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("Validated repository workspace/validated-repository isn't directory");

        initializer.initialize(ctx);
    }

    @Test
    public void shouldFailWhenValidatedRepositoryIsEmpty() throws IOException {
        FileUtils.forceDelete(validatedRepository);
        FileUtils.forceMkdir(validatedRepository);

        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("Validated repository workspace/validated-repository is empty");

        initializer.initialize(ctx);
    }

}