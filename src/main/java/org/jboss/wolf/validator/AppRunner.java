package org.jboss.wolf.validator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class AppRunner {

    private static final Logger logger = LoggerFactory.getLogger(AppRunner.class);

    public static void main(String[] args) {
        new AppRunner().run(args);
    }

    private final Option validatedRepositoryOption = createOption("vr", "validated-repository", "validate given repository, \ndefault value is `workspace/validated-repository`", "dir");
    private final Option validatedDistributionOption = createOption("vd", "validated-distribution", "validate given distribution, verify if current distribution is valid, \ndefault value is `workspace/validated-distribution`", "dir");
    private final Option localRepositoryOption = createOption("lr", "local-repository", "use given local repository, \ndefault value is `workspace/local-repository`", "dir");
    private final Option remoteRepositoryOption = createOption("rr", "remote-repository", "use given remote repository, this option can be used multiple times, \ndefault remote repository is only maven central", "url");
    private final Option reportFileOption = createOption("r", "report", "write generated report into this file, \ndefault location for reports is in `workspace/report.txt`", "file");
    private final Option configOption = createOption("c", "config", "use given configuration file, \ndefault value is `wolf-validator-config.xml`", "file");
    private final Option helpOption = createOption("h", "help", "print help and exit", null);

    protected ApplicationContext appCtx;
    
    @Inject
    protected AppInitializer initializer;
    @Inject
    protected ValidatorContext context;
    @Inject
    protected ValidationExecutor validationExecutor;
    @Inject
    protected ReportingExecutor reportingExecutor;
    @Autowired(required = false)
    protected ExceptionFilter[] exceptionFilters;

    public void run(String... arguments) {
        Options options = new Options();
        options.addOption(validatedRepositoryOption);
        options.addOption(validatedDistributionOption);
        options.addOption(localRepositoryOption);
        options.addOption(remoteRepositoryOption);
        options.addOption(reportFileOption);
        options.addOption(configOption);
        options.addOption(helpOption);
        
        try {
            CommandLineParser parser = new BasicParser();
            CommandLine line = parser.parse(options, arguments);

            if (line.hasOption(helpOption.getOpt())) {
                runHelp(options);
            } else {
                initApplicationContext(line);
                runValidation();
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
        formatter.setSyntaxPrefix("Usage: ");
        formatter.setWidth(120);
        formatter.setLeftPadding(4);
        
        String header = "Wolf-validator is a tool used to validate the internal consistency of a maven artifact repository.\n";
        String footer = "Example: \n"
                + "    to run against a given validated repository directory, use: \n"
                + "    $ wolf-validator -vr ~/myrepository";
        
        System.out.println(header);
        formatter.printHelp("wolf-validator", options, true);
        System.out.println(footer);
    }

    protected void runValidation() {
        initializer.initialize(context);
        validationExecutor.execute(context);
        context.applyExceptionFilters(exceptionFilters);
        reportingExecutor.execute(context);
    }

    private void initApplicationContext(CommandLine line) {
        String reportFile = line.getOptionValue(reportFileOption.getOpt(), "workspace/report.txt");
        String validatedRepo = line.getOptionValue(validatedRepositoryOption.getOpt(), "workspace/validated-repository");
        String validatedDist = line.getOptionValue(validatedDistributionOption.getOpt(), "workspace/validated-distribution");
        String localRepo = line.getOptionValue(localRepositoryOption.getOpt(), "workspace/local-repository");
        String[] remoteRepos = line.getOptionValues(remoteRepositoryOption.getOpt());

        System.setProperty("wolf-reportFile", reportFile);
        System.setProperty("wolf-validatedRepository", validatedRepo);
        System.setProperty("wolf-validatedDistribution", validatedDist);
        System.setProperty("wolf-localRepository", localRepo);
        System.setProperty("wolf-remoteRepositories", StringUtils.defaultString(StringUtils.join(remoteRepos, ';')));

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

        appCtx = new GenericXmlApplicationContext(resources.toArray(new Resource[] {}));
        
        AutowireCapableBeanFactory autowireCapableBeanFactory = appCtx.getAutowireCapableBeanFactory();
        autowireCapableBeanFactory.autowireBean(this);
    }

    private Option createOption(String opt, String longOpt, String description, String argName) {
        Option option = new Option(opt, argName != null, description);
        option.setLongOpt(longOpt);
        option.setArgName(argName);
        return option;
    }

}