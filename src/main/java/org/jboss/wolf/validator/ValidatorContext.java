package org.jboss.wolf.validator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidatorContext {

    private static final Logger logger = LoggerFactory.getLogger(ValidatorContext.class);

    private final File validatedRepository;
    private final List<RemoteRepository> remoteRepositories;
    private final Map<File, List<Exception>> exceptions = new HashMap<File, List<Exception>>();

    public ValidatorContext(File validatedRepository, List<RemoteRepository> remoteRepositories) {
        this.validatedRepository = validatedRepository;
        this.remoteRepositories = Collections.unmodifiableList(new ArrayList<RemoteRepository>(remoteRepositories));
    }

    public File getValidatedRepository() {
        return validatedRepository;
    }

    public List<RemoteRepository> getRemoteRepositories() {
        return remoteRepositories;
    }

    public boolean isSuccess() {
        return getExceptions().isEmpty();
    }

    public void addException(File pomFile, Exception e) {
        logger.debug("for `{}` register exception `{}`", pomFile, e.getMessage());

        List<Exception> exceptionList = exceptions.get(pomFile);
        if (exceptionList == null) {
            exceptionList = new ArrayList<Exception>();
            exceptions.put(pomFile, exceptionList);
        }
        exceptionList.add(e);
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

}