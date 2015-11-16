package com.redhat.repository.validator.filter;

import com.redhat.repository.validator.ExceptionFilter;

import java.io.File;
import java.util.regex.Pattern;

public class FilenameBasedExceptionFilter implements ExceptionFilter {
    private final Pattern filenamePattern;

    private final Class<? extends Exception> exceptionClass;

    public FilenameBasedExceptionFilter(String filenameRegex, Class<? extends Exception> exceptionClass) {
        this.filenamePattern = Pattern.compile(filenameRegex);
        this.exceptionClass = exceptionClass;
    }

    public FilenameBasedExceptionFilter(String filenameRegex) {
        this(filenameRegex, null);
    }

    public Class<? extends Exception> getExceptionClass() {
        return exceptionClass;
    }

    public String getFilenameRegex() {
        return filenamePattern.pattern();
    }

    @Override
    public boolean shouldIgnore(Exception exception, File fileInRepo) {
        if (exceptionClass != null) {
            return exceptionClass.isInstance(exception) && filenamePattern.matcher(fileInRepo.getName()).matches();
        } else {
            return filenamePattern.matcher(fileInRepo.getName()).matches();
        }
    }

    @Override
    public String toString() {
        return "FilenameBasedExceptionFilter{" +
                "filenameRegex=" + filenamePattern.pattern() +
                ", exceptionClass=" + exceptionClass.getCanonicalName() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FilenameBasedExceptionFilter that = (FilenameBasedExceptionFilter) o;

        if (filenamePattern != null ? !filenamePattern.pattern().equals(that.filenamePattern.pattern()) : that.filenamePattern != null)
            return false;
        if (exceptionClass != null ? !exceptionClass.equals(that.exceptionClass) : that.exceptionClass != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = filenamePattern != null ? filenamePattern.pattern().hashCode() : 0;
        result = 31 * result + (exceptionClass != null ? exceptionClass.hashCode() : 0);
        return result;
    }

}
