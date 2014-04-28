package org.jboss.wolf.validator;

import static org.jboss.wolf.validator.internal.Utils.relativize;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidatorContext {

    private static final Logger logger = LoggerFactory.getLogger(Validator.class);

    private final File validatedRepository;
    private final File validatedDistribution;
    private final List<RemoteRepository> remoteRepositories;
    private final Map<File, List<Exception>> exceptions = new HashMap<File, List<Exception>>();
    private final Set<Exception> processedExceptions = new HashSet<Exception>();

    public ValidatorContext(File validatedRepository, File validatedDistribution, List<RemoteRepository> remoteRepositories) {
        this.validatedRepository = validatedRepository;
        this.validatedDistribution = validatedDistribution; 
        this.remoteRepositories = Collections.unmodifiableList(new ArrayList<RemoteRepository>(remoteRepositories));
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
        return exceptions.isEmpty();
    }

    public void addException(File file, Exception e) {
        logger.debug("for `{}` register exception `{}: {}`", relativize(this, file), e.getClass().getSimpleName(), e.getMessage());

        List<Exception> exceptionList = exceptions.get(file);
        if (exceptionList == null) {
            exceptionList = new ArrayList<Exception>();
            exceptions.put(file, exceptionList);
        }
        exceptionList.add(e);
    }
    
    public void addProcessedException(Exception e) {
        processedExceptions.add(e);
    }

    public List<Exception> getExceptions() {
        List<Exception> result = new ArrayList<Exception>();
        for (List<Exception> exceptionList : exceptions.values()) {
            result.addAll(exceptionList);
        }
        return Collections.unmodifiableList(result);
    }

    public List<Exception> getExceptions(File pomFile) {
        if (exceptions.containsKey(pomFile)) {
            return Collections.unmodifiableList(exceptions.get(pomFile));
        } else {
            return Collections.emptyList();
        }
    }

    public <E extends Exception> List<E> getExceptions(Class<E> exceptionType) {
        return filterExceptions(getExceptions(), exceptionType);
    }

    public <E extends Exception> List<E> getExceptions(File pomFile, Class<E> exceptionType) {
        return filterExceptions(getExceptions(pomFile), exceptionType);
    }

    private <E extends Exception> List<E> filterExceptions(List<Exception> exceptions, Class<E> exceptionType) {
        List<E> result = new ArrayList<E>();
        for (Exception exception : exceptions) {
            if (exceptionType.isInstance(exception)) {
                result.add(exceptionType.cast(exception));
            }
        }
        return Collections.unmodifiableList(result);
    }

    public List<Exception> getUnprocessedExceptions() {
        List<Exception> unprocessedExceptions = new ArrayList<Exception>();
        for (Exception exception : getExceptions()) {
            if (!processedExceptions.contains(exception)) {
                unprocessedExceptions.add(exception);
            }
        }
        return Collections.unmodifiableList(unprocessedExceptions);
    }

    public void applyExceptionFilters(ExceptionFilter[] exceptionFilters) {
        if (exceptionFilters == null) {
            logger.debug("Skipping the exception filtering as no filters were defined!");
            return;
        }
        logger.debug("Applying {} exception filter(s).", exceptionFilters.length);
        for (ExceptionFilter exceptionFilter : exceptionFilters) {
            logger.debug("Applying exception filter " + exceptionFilter);
            applyExceptionFilter(exceptionFilter);
        }
    }

    public void applyExceptionFilter(ExceptionFilter exceptionFilter) {
        for (Map.Entry<File, List<Exception>> fileToExceptions : exceptions.entrySet()) {
            File fileInRepo = fileToExceptions.getKey();
            List<Exception> exceptionList = fileToExceptions.getValue();
            List<Exception> exceptionsToRemove = new ArrayList<Exception>();
            for (Exception exception : exceptionList) {
                if (exceptionFilter.shouldIgnore(exception, fileInRepo)) {
                    logger.debug("Filtering (ignoring) exception: " + exception);
                    exceptionsToRemove.add(exception);
                }
            }
            // remove the marked exceptions from list and update the exceptions map
            if (!exceptionsToRemove.isEmpty()) {
                exceptionList.removeAll(exceptionsToRemove);
                exceptions.put(fileInRepo, exceptionList);
            }
        }
    }

}