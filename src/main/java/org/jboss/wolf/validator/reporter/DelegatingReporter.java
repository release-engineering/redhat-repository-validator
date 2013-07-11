package org.jboss.wolf.validator.reporter;

import org.jboss.wolf.validator.Reporter;
import org.jboss.wolf.validator.ValidatorContext;

public class DelegatingReporter implements Reporter {

    private final Reporter[] reporters;

    public DelegatingReporter(Reporter... reporters) {
        this.reporters = reporters;
    }

    @Override
    public void report(ValidatorContext ctx) {
        for (Reporter reporter : reporters) {
            reporter.report(ctx);
        }
    }

}