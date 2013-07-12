package org.jboss.wolf.validator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class ValidatorRunner {

    private static final Logger logger = LoggerFactory.getLogger(ValidatorRunner.class);

    public static void main(String[] args) {
        logger.debug("start");
        try {
            new ValidatorRunner().run(args);
        } finally {
            logger.debug("stop");
        }
    }

    private final Option validatedRepositoryOption = createOption("vr", "validated-repository", "validate given repository", "dir");
    private final Option localRepositoryOption = createOption("lr", "local-repository", "use given local repository", "dir");
    private final Option remoteRepositoryOption = createOption("rr", "remote-repository", "use given remote repository", "url");
    private final Option configOption = createOption("c", "config", "use given configuration file", "file");
    private final Option helpOption = createOption("h", "help", "print help and exit", null);

    public void run(String... arguments) {
        Options options = new Options();
        options.addOption(validatedRepositoryOption);
        options.addOption(localRepositoryOption);
        options.addOption(remoteRepositoryOption);
        options.addOption(configOption);
        options.addOption(helpOption);
        
        try {
            CommandLineParser parser = new BasicParser();
            CommandLine line = parser.parse(options, arguments);

            if (line.hasOption(helpOption.getOpt())) {
                runHelp(options);
            } else {
                runValidation(line);
            }
        } catch (ParseException e) {
            logger.warn("{}\n", e.getMessage());
            runHelp(options);
        } catch (RuntimeException e) {
            logger.error("Unhandled exception", e);
            throw e;
        } catch (Exception e) {
            logger.error("Unhandled exception", e);
            throw new RuntimeException(e);
        }
    }

    protected void runHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("wolf-validator", options);
    }

    protected void runValidation(CommandLine line) {
        ApplicationContext appCtx = createApplicationContext(line);
        runValidation(appCtx);
    }

    protected void runValidation(ApplicationContext appCtx) {
        ValidatorContext ctx = appCtx.getBean(ValidatorContext.class);
        
        ValidatorInitializer initializer = appCtx.getBean(ValidatorInitializer.class);
        initializer.initialize(ctx);

        Validator validator = appCtx.getBean(Validator.class);
        validator.validate(ctx);

        Reporter reporter = appCtx.getBean(Reporter.class);
        reporter.report(ctx);
    }

    private ApplicationContext createApplicationContext(CommandLine line) {
        String validatedRepo = line.getOptionValue(validatedRepositoryOption.getOpt(), "workspace/validated-repository");
        String localRepo = line.getOptionValue(localRepositoryOption.getOpt(), "workspace/local-repository");
        String[] remoteRepos = line.getOptionValues(remoteRepositoryOption.getOpt());

        System.setProperty("validatedRepository", validatedRepo);
        System.setProperty("localRepository", localRepo);
        System.setProperty("remoteRepositories", StringUtils.defaultString(StringUtils.join(remoteRepos, ';')));

        String userConfigFile = line.getOptionValue(configOption.getOpt());
        if (userConfigFile == null) {
            File defaultUserConfig = new File("wolf-validator-config.xml");
            if (defaultUserConfig.exists() && defaultUserConfig.isFile()) {
                userConfigFile = defaultUserConfig.getAbsolutePath();
            }
        }

        List<Resource> resources = new ArrayList<Resource>();
        resources.add(new ClassPathResource("wolf-validator-app-context.xml"));
        if (userConfigFile != null) {
            resources.add(new FileSystemResource(userConfigFile));
        }

        GenericXmlApplicationContext appCtx = new GenericXmlApplicationContext(resources.toArray(new Resource[] {}));
        return appCtx;
    }

    private Option createOption(String opt, String longOpt, String description, String argName) {
        Option option = new Option(opt, argName != null, description);
        option.setLongOpt(longOpt);
        option.setArgName(argName);
        return option;
    }

}