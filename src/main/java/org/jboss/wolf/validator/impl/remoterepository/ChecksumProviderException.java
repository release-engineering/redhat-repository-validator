package org.jboss.wolf.validator.impl.remoterepository;

public class ChecksumProviderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ChecksumProviderException(String msg) {
        super(msg);
    }

    public ChecksumProviderException(String msg, Throwable cause) {
        super(msg, cause);
    }

}