package org.jboss.wolf.validator.impl;

import static org.jboss.wolf.validator.internal.Utils.sortExceptions;

import java.io.PrintStream;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.jboss.wolf.validator.Reporter;
import org.jboss.wolf.validator.ValidatorContext;

@Named
public class UnprocessedExceptionsReporter implements Reporter {
    
    @Inject @Named("unprocessedExceptionsReporterStream")
    private PrintStream out;

    @Override
    public void report(ValidatorContext ctx) {
        List<Exception> allExceptions = sortExceptions(ctx.getExceptions());
        List<Exception> unprocessedExceptions = sortExceptions(ctx.getUnprocessedExceptions());

        if (!unprocessedExceptions.isEmpty()) {
            out.println("--- UNPROCESSED EXCEPTIONS REPORT ---");
            out.println("Unprocessed exceptions count " + unprocessedExceptions.size() + ", from total " + allExceptions.size() + ".");
            
            Exception lastException = null;
            for (Exception unprocessedException : unprocessedExceptions) {
                if (lastException == null || !lastException.getClass().getName().equals(unprocessedException.getClass().getName())) {
                    out.println();
                }
                reportException(unprocessedException, 0);
                lastException = unprocessedException;
            }
            out.println();
            out.flush();
        }
    }
    
    private void reportException(Throwable e, int depth) {
        StringBuilder msg = new StringBuilder();
        msg.append(StringUtils.repeat(" ", depth * 4));
        msg.append(e.getClass().getSimpleName());
        msg.append(" ");
        msg.append(e.getMessage());

        out.println(msg.toString());

        if (e.getCause() != null) {
            reportException(e.getCause(), depth + 1);
        }
    }

}