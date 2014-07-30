package org.jboss.wolf.validator;

import java.io.File;

public class ValidationError {

    private final Validator validator;
    private final Exception exception;
    private final File file;

    public ValidationError(Validator validator, Exception exception, File file) {
        super();
        this.validator = validator;
        this.exception = exception;
        this.file = file;
    }

    public Validator getValidator() {
        return validator;
    }

    public Exception getException() {
        return exception;
    }

    public File getFile() {
        return file;
    }

}