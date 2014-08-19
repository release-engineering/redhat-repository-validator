package org.jboss.wolf.validator.impl.remoterepository;

public class RemoteRepositoryCompareException extends Exception {

    private static final long serialVersionUID = 1L;

    public RemoteRepositoryCompareException(String msg) {
        super(msg);
    }

    public RemoteRepositoryCompareException(String msg, Throwable cause) {
        super(msg, cause);
    }

}