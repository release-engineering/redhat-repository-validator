package org.jboss.wolf.validator;

import static org.jboss.wolf.validator.internal.Utils.relativize;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidatorContext {

    private static final Logger logger = LoggerFactory.getLogger(Validator.class);

    private final File validatedRepository;
    private final File validatedDistribution;
    private final List<RemoteRepository> remoteRepositories;
    private final List<ExceptionFilter> exceptionFilters;
    private final List<ValidationError> errors = new ArrayList<ValidationError>();
    private final List<ValidationError> ignoredErrors = new ArrayList<ValidationError>();

    public ValidatorContext(File validatedRepository, File validatedDistribution, List<RemoteRepository> remoteRepositories) {
        this(validatedRepository, validatedDistribution, remoteRepositories, null);
    }

    public ValidatorContext(File validatedRepository, File validatedDistribution, List<RemoteRepository> remoteRepositories, List<ExceptionFilter> exceptionFilters) {
        this.validatedRepository = validatedRepository;
        this.validatedDistribution = validatedDistribution; 
        this.remoteRepositories = remoteRepositories;
        this.exceptionFilters = exceptionFilters;
    }

    public File getValidatedRepository() {
        return validatedRepository;
    }
    
    public File getValidatedDistribution() {
        return validatedDistribution;
    }

    public List<RemoteRepository> getRemoteRepositories() {
        return remoteRepositories;
    }

    public boolean isSuccess() {
        return errors.isEmpty();
    }

    public void addError(Validator validator, File file, Exception e) {
        if( isIgnored(validator, file, e) ) {
            logger.debug("ignoring exception `{}: {}`", e.getClass().getSimpleName(), e.getMessage());
            ignoredErrors.add(new ValidationError(validator, e, file));
        } else {
            logger.debug("for `{}` register exception `{}: {}`", relativize(this, file), e.getClass().getSimpleName(), e.getMessage());
            errors.add(new ValidationError(validator, e, file));
        }
    }
    
    private boolean isIgnored(Validator validator, File file, Exception e) {
        if (exceptionFilters != null) {
            for (ExceptionFilter exceptionFilter : exceptionFilters) {
                if (exceptionFilter.shouldIgnore(e, file)) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<ValidationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public List<ValidationError> getErrors(File pomFile) {
        List<ValidationError> result = new ArrayList<ValidationError>();
        for(ValidationError error : errors) {
            if( pomFile.equals(error.getFile()) ) {
                result.add(error);
            }
        }
        return Collections.unmodifiableList(result);
    }
    
    public List<Exception> getExceptions() {
        List<Exception> result = new ArrayList<Exception>();
        for (ValidationError error : errors) {
            result.add(error.getException());
        }
        return Collections.unmodifiableList(result);
    }

    public <E extends Exception> List<E> getExceptions(Class<E> exceptionType) {
        List<E> result = new ArrayList<E>();
        for (Exception exception : getExceptions()) {
            if (exceptionType.isInstance(exception)) {
                result.add(exceptionType.cast(exception));
            }
        }
        return Collections.unmodifiableList(result);
    }
    
    public List<ValidationError> getIgnoredErrors() {
        return Collections.unmodifiableList(ignoredErrors);
    }
    
    public List<Exception> getIgnoredExceptions() {
        List<Exception> result = new ArrayList<Exception>();
        for (ValidationError ignoredError : ignoredErrors) {
            result.add(ignoredError.getException());
        }
        return Collections.unmodifiableList(result);
    }
    
}