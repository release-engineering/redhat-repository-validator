package org.jboss.wolf.validator;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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
        new ValidatorRunner().run(args);
    }

    @SuppressWarnings("static-access")
    public void run(String... arguments) {
        Option help = new Option("h", "help", false, "print help and exit");

        Option config = OptionBuilder.withArgName("file")
                .hasArg()
                .withDescription("use given configuration file")
                .withLongOpt("config")
                .create("c");

        Options options = new Options();
        options.addOption(help);
        options.addOption(config);
        try {
            CommandLineParser parser = new BasicParser();
            CommandLine line = parser.parse(options, arguments);

            if (line.hasOption("h")) {
                runHelp(options);
            }
            else {
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
        ValidatorContext validatorContext = appCtx.getBean(ValidatorContext.class);
        Validator validator = appCtx.getBean(Validator.class);
        validator.validate(validatorContext);
    }

    protected ApplicationContext createApplicationContext(CommandLine line) {
        String userConfigFile = line.getOptionValue("c");
        if (userConfigFile == null) {
            // TODO default location
        }

        List<Resource> resources = new ArrayList<Resource>();
        resources.add(new ClassPathResource("wolf-validator-app-context.xml"));
        if (userConfigFile != null) {
            resources.add(new FileSystemResource(userConfigFile));
        }

        GenericXmlApplicationContext appCtx = new GenericXmlApplicationContext(resources.toArray(new Resource[] {}));
        return appCtx;
    }

}