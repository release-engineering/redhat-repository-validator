package org.jboss.wolf.validator.filter;

import org.jboss.wolf.validator.ExceptionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.regex.Pattern;

public class FilenameBasedExceptionFilter implements ExceptionFilter {

	private static final Logger LOG = LoggerFactory.getLogger(FilenameBasedExceptionFilter.class);

	private final Pattern filenamePattern;
    private final Pattern filepathPattern;

    private final Class<? extends Exception> exceptionClass;

    public FilenameBasedExceptionFilter(String filenameRegex, String filepathRegex, Class<? extends Exception> exceptionClass) {
		if (filenameRegex == null && filepathRegex == null) {
			final IllegalArgumentException e = new IllegalArgumentException("filenameRegex or filepathRegex must be defined");
			LOG.error(e.getMessage(), e);
			throw e;
		}
        this.filenamePattern = filenameRegex != null ? Pattern.compile(filenameRegex) : null;
        this.filepathPattern = filepathRegex != null ? Pattern.compile(filepathRegex) : null;
        this.exceptionClass = exceptionClass;
    }

    public FilenameBasedExceptionFilter(String filenameRegex, String filepathRegex) {
        this(filenameRegex, filepathRegex, null);
    }

    public Class<? extends Exception> getExceptionClass() {
        return exceptionClass;
    }

    public String getFilenameRegex() {
        return filenamePattern != null ? filenamePattern.pattern() : null;
    }

    public String getFilepathRegex() {
        return filepathPattern != null ? filepathPattern.pattern() : null;
    }

    @Override
    public boolean shouldIgnore(Exception exception, File fileInRepo) {
		return exceptionMatches(exception) && filenamePatternMatches(fileInRepo) && filepathPatternMatches(fileInRepo);
    }

	private boolean exceptionMatches(final Exception exception) {
		return exceptionClass == null || exceptionClass.isInstance(exception);
	}

	private boolean filenamePatternMatches(final File file) {
		return filenamePattern == null || filenamePattern.matcher(file.getName()).matches();
	}

	private boolean filepathPatternMatches(final File file) {
		return filepathPattern == null || filepathPattern.matcher(file.getPath()).matches();
	}

    @Override
    public String toString() {
		return "FilenameBasedExceptionFilter{" +
				"filenameRegex=" + getFilenameRegex() +
				", filepathRegex=" + getFilepathRegex() +
				", exceptionClass=" + (exceptionClass != null ? exceptionClass.getCanonicalName() : "") +
				'}';
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FilenameBasedExceptionFilter that = (FilenameBasedExceptionFilter) o;

        if (getFilenameRegex() != null ? !getFilenameRegex().equals(that.getFilenameRegex()) : that.getFilenameRegex() != null)
            return false;
		if (getFilepathRegex() != null ? !getFilepathRegex().equals(that.getFilepathRegex()) : that.getFilepathRegex() != null)
			return false;
        if (exceptionClass != null ? !exceptionClass.equals(that.exceptionClass) : that.exceptionClass != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = getFilenameRegex() != null ? getFilenameRegex().hashCode() : 0;
		result = 31 * result + (getFilepathRegex() != null ? getFilepathRegex().hashCode() : 0);
        result = 31 * result + (exceptionClass != null ? exceptionClass.hashCode() : 0);
        return result;
    }

}
