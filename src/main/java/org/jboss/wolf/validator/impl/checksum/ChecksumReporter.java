package org.jboss.wolf.validator.impl.checksum;

import static org.jboss.wolf.validator.internal.Utils.relativize;

import java.io.PrintStream;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.wolf.validator.Reporter;
import org.jboss.wolf.validator.ValidatorContext;
import org.springframework.core.annotation.Order;

@Named
@Order(200)
public class ChecksumReporter implements Reporter {

    @Inject @Named("checksumReporterStream")
    private PrintStream out;

    @Override
    public void report(ValidatorContext ctx) {
        List<ChecksumNotExistException> checksumNotExistExceptions = ctx.getExceptions(ChecksumNotExistException.class);
        List<ChecksumNotMatchException> checksumNotMatchExceptions = ctx.getExceptions(ChecksumNotMatchException.class);

        if (checksumNotExistExceptions.isEmpty() && checksumNotMatchExceptions.isEmpty()) {
            return;
        }

        out.println("--- CHECKSUM REPORT ---");

        if (!checksumNotExistExceptions.isEmpty()) {
            out.println("Found " + checksumNotExistExceptions.size() + " missing checksums.");
            for (ChecksumNotExistException e : checksumNotExistExceptions) {
                out.println(relativize(ctx, e.getFile()) + " not exist " + e.getAlgorithm());
                ctx.addProcessedException(e);
            }
        }

        if (!checksumNotMatchExceptions.isEmpty()) {
            out.println("Found " + checksumNotMatchExceptions.size() + " not match checksums.");
            for (ChecksumNotMatchException e : checksumNotMatchExceptions) {
                out.println(relativize(ctx, e.getFile()) + " not match " + e.getAlgorithm());
                ctx.addProcessedException(e);
            }
        }

        out.println();
        out.flush();
    }

}