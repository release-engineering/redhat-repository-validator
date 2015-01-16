package org.jboss.wolf.validator.filter;

import org.jboss.wolf.validator.ExceptionFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.regex.Pattern;

public class FileBasedExceptionFilter implements ExceptionFilter {

    private static final Logger LOG = LoggerFactory.getLogger(FileBasedExceptionFilter.class);

    private final Pattern filenamePattern;

    private final Pattern filepathPattern;

    private final Class<? extends Exception> exceptionClass;

    private final Pattern exceptionMsgPattern;

    public FileBasedExceptionFilter(String nameRegex, String pathRegex, Class<? extends Exception> exceptionClass,
                                    String exceptionMsgRegex) {
        if (nameRegex == null && pathRegex == null) {
            final IllegalArgumentException e = new IllegalArgumentException("nameRegex or pathRegex must be defined");
            LOG.error(e.getMessage(), e);
            throw e;
        }
        this.filenamePattern = nameRegex != null ? Pattern.compile(nameRegex) : null;
        this.filepathPattern = pathRegex != null ? Pattern.compile(pathRegex) : null;
        this.exceptionClass = exceptionClass;
        this.exceptionMsgPattern = exceptionMsgRegex != null ? Pattern.compile(exceptionMsgRegex) : null;
    }

    public FileBasedExceptionFilter(String nameRegex, String pathRegex, String exceptionMsgRegex) {
        this(nameRegex, pathRegex, null, exceptionMsgRegex);
    }

    public Class<? extends Exception> getExceptionClass() {
        return exceptionClass;
    }

    public String getNameRegex() {
        return filenamePattern != null ? filenamePattern.pattern() : null;
    }

    public String getPathRegex() {
        return filepathPattern != null ? filepathPattern.pattern() : null;
    }

    public String getExceptionMsgRegex() {
        return exceptionMsgPattern != null ? exceptionMsgPattern.pattern() : null;
    }

    @Override
    public boolean shouldIgnore(Exception exception, File fileInRepo) {
        return exceptionMatches(exception) && exceptionMsgMatches(exception) && filenamePatternMatches(fileInRepo)
                && filepathPatternMatches(fileInRepo);
    }

    private boolean exceptionMatches(final Exception exception) {
        return exceptionClass == null || exceptionClass.isInstance(exception);
    }

    private boolean exceptionMsgMatches(final Exception exception) {
        return exceptionMsgPattern == null
                || (exception.getMessage() != null && exceptionMsgPattern.matcher(exception.getMessage()).matches());
    }

    private boolean filenamePatternMatches(final File file) {
        return filenamePattern == null || filenamePattern.matcher(file.getName()).matches();
    }

    private boolean filepathPatternMatches(final File file) {
        return filepathPattern == null || filepathPattern.matcher(file.getPath()).matches();
    }

    @Override
    public String toString() {
        return "FileBasedExceptionFilter{" +
                "nameRegex=" + getNameRegex() +
                ", pathRegex=" + getPathRegex() +
                ", exceptionClass=" + (exceptionClass != null ? exceptionClass.getCanonicalName() : "") +
                ", exceptionMsgRegex=" + getExceptionMsgRegex() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileBasedExceptionFilter that = (FileBasedExceptionFilter) o;
        if (getNameRegex() != null ? !getNameRegex().equals(that.getNameRegex()) : that.getNameRegex() != null)
            return false;
        if (getPathRegex() != null ? !getPathRegex().equals(that.getPathRegex()) : that.getPathRegex() != null)
            return false;
        if (exceptionClass != null ? !exceptionClass.equals(that.exceptionClass) : that.exceptionClass != null)
            return false;
        if (getExceptionMsgRegex() != null ? !getExceptionMsgRegex().equals(that.getExceptionMsgRegex()) : that.getExceptionMsgRegex() != null)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = getNameRegex() != null ? getNameRegex().hashCode() : 0;
        result = 31 * result + (getPathRegex() != null ? getPathRegex().hashCode() : 0);
        result = 31 * result + (exceptionClass != null ? exceptionClass.hashCode() : 0);
        result = 31 * result + (getExceptionMsgRegex() != null ? getExceptionMsgRegex().hashCode() : 0);
        return result;
    }

}
