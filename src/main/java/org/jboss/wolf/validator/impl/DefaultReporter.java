package org.jboss.wolf.validator.impl;

import static org.jboss.wolf.validator.internal.Utils.sortExceptions;

import java.io.PrintStream;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jboss.wolf.validator.Reporter;
import org.jboss.wolf.validator.ValidatorContext;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

@Named
public class DefaultReporter implements Reporter {

    @Inject @Named("defaultReporterStream")
    private PrintStream out;

    @Override
    public void report(ValidatorContext ctx) {
        List<Exception> exceptionList = sortExceptions(ctx.getUnprocessedExceptions());
        if (exceptionList.isEmpty()) {
            return;
        }

        Multimap<Class<? extends Exception>, Exception> exceptionMultimap = LinkedListMultimap.create();
        for (Exception exception : exceptionList) {
            exceptionMultimap.put(exception.getClass(), exception);
        }

        out.println("--- EXCEPTIONS REPORT ---");
        for (Class<? extends Exception> exceptionType : exceptionMultimap.keySet()) {
            out.println();
            out.println(exceptionType.getSimpleName() + " (total count " + exceptionMultimap.get(exceptionType).size() + ")");
            for (Exception exception : exceptionMultimap.get(exceptionType)) {
                reportException(exception, 0);
            }
        }
        out.println();
        out.flush();
    }

    private void reportException(Throwable e, int depth) {
        StringBuilder msg = new StringBuilder();
        if (depth > 0) {
            msg.append(StringUtils.repeat(" ", depth * 4));
            msg.append(e.getClass().getSimpleName());
            msg.append(" ");
        }
        
        if (e.getMessage() != null) {
            msg.append(e.getMessage());
        }
        if (e.getMessage() == null && e.getCause() == null) {
            msg.append(ExceptionUtils.getStackTrace(e));
            msg.append(SystemUtils.LINE_SEPARATOR);
        }

        out.println(msg.toString());

        if (e.getCause() != null) {
            reportException(e.getCause(), depth + 1);
        }
    }

}