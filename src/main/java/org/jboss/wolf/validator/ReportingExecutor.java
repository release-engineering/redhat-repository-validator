package org.jboss.wolf.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportingExecutor {
    private static Logger logger = LoggerFactory.getLogger(ReportingExecutor.class);

    private final Reporter[] reporters;

    public ReportingExecutor(Reporter[] reporters) {
        this.reporters = reporters;
    }

    public void execute(ValidatorContext ctx) {
        for (Reporter reporter : reporters) {
            try {
                reporter.report(ctx);
            } catch (RuntimeException e) {
                logger.error("reporter " + reporter.getClass().getSimpleName() + " ended with unexpected exception!", e);
            }
        }
    }

}
