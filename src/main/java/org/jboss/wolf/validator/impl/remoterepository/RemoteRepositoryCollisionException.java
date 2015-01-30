package org.jboss.wolf.validator.impl.remoterepository;

public class RemoteRepositoryCollisionException extends Exception {

    private static final long serialVersionUID = 1L;

    public RemoteRepositoryCollisionException(String msg) {
        super(msg);
    }

    public RemoteRepositoryCollisionException(String msg, Throwable cause) {
        super(msg, cause);
    }

}