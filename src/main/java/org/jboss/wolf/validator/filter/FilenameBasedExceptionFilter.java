package org.jboss.wolf.validator.filter;

import org.jboss.wolf.validator.ExceptionFilter;

import java.io.File;
import java.util.regex.Pattern;

public class FilenameBasedExceptionFilter implements ExceptionFilter {
    private final Class<? extends Exception> exceptionClass;

    private final Pattern filenamePattern;

    public FilenameBasedExceptionFilter(Class<? extends Exception> exceptionClass, String filenameRegex) {
        this.exceptionClass = exceptionClass;
        this.filenamePattern = Pattern.compile(filenameRegex);
    }

    @Override
    public boolean shouldIgnore(Exception exception, File fileInRepo) {
        return exceptionClass.isInstance(exception) && filenamePattern.matcher(fileInRepo.getName()).matches();
    }

}
