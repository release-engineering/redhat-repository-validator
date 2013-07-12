package org.jboss.wolf.validator;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidatorInitializer {

    private static final Logger logger = LoggerFactory.getLogger(ValidatorInitializer.class);

    @Inject
    private LocalRepository localRepository;

    public void initialize(ValidatorContext ctx) {
        localRepositoryShouldExist();
        localRepositoryShouldBeEmpty();
        validatedRepositoryShouldExist(ctx);
        validatedRepositoryShouldNotBeEmpty(ctx);
        remoteRepositoriesShouldNotBeEmpty(ctx);
        logInformation(ctx);
    }

    private void localRepositoryShouldExist() {
        File dir = localRepository.getBasedir();
        if (dir.exists() && dir.isFile()) {
            throw new RuntimeException("Local repository " + dir + " isn't directory");
        }
        if (!dir.exists()) {
            logger.info("Local repository {} doesn't exist", dir);
            if (dir.mkdirs()) {
                logger.info("Local repository {} created", dir);
            } else {
                logger.error("Failed to create local repository " + dir);
                throw new RuntimeException("Failed to create local repository " + dir);
            }
        }
    }

    private void localRepositoryShouldBeEmpty() {
        File dir = localRepository.getBasedir();
        if (dir.list().length != 0) {
            logger.warn("Local repository should be empty, cleaning its content...");
            try {
                FileUtils.cleanDirectory(dir);
            } catch (IOException e) {
                logger.error("Failed to clean local repository");
                throw new RuntimeException("Failed to clean local repository", e);
            }
        }
    }

    private void validatedRepositoryShouldExist(ValidatorContext ctx) {
        File dir = ctx.getValidatedRepository();
        if (!dir.exists()) {
            logger.error("Validated repository " + dir + " doesn't exist");
            throw new RuntimeException("Validated repository " + dir + " doesn't exist");
        }
        if (!dir.isDirectory()) {
            logger.error("Validated repository " + dir + " isn't directory");
            throw new RuntimeException("Validated repository " + dir + " isn't directory");
        }
    }

    private void validatedRepositoryShouldNotBeEmpty(ValidatorContext ctx) {
        File dir = ctx.getValidatedRepository();
        if (dir.list().length == 0) {
            logger.error("Validated repository " + dir + " is empty");
            throw new RuntimeException("Validated repository " + dir + " is empty");
        }
    }

    private void remoteRepositoriesShouldNotBeEmpty(ValidatorContext ctx) {
        List<RemoteRepository> remoteRepositories = ctx.getRemoteRepositories();
        if (remoteRepositories.isEmpty()) {
            logger.warn("Remote repositories should not be empty");
        }
    }

    private void logInformation(ValidatorContext ctx) {
        StringBuilder log = new StringBuilder();
        log.append("Used configuration \n");
        log.append("    local repository     : ").append(localRepository.getBasedir()).append("\n");
        log.append("    validated repository : ").append(ctx.getValidatedRepository()).append("\n");
        log.append("    remote repositories  : \n");
        for (RemoteRepository remoteRepository : ctx.getRemoteRepositories()) {
            log.append("        ").append(remoteRepository.getUrl()).append("\n");
        }
        logger.info(log.toString());
    }

}