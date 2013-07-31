package org.jboss.wolf.validator.internal;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// see project shrinkwrap-resolver, class org.jboss.shrinkwrap.resolver.impl.maven.logging.LogTransferListener
public class LogTransferListener extends AbstractTransferListener {

    private static final Logger logger = LoggerFactory.getLogger(LogTransferListener.class);

    private static final long TRANSFER_THRESHOLD = 1024 * 50;

    private final Map<TransferResource, Long> downloads = new ConcurrentHashMap<TransferResource, Long>();

    @Override
    public void transferInitiated(TransferEvent event) {
        TransferResource resource = event.getResource();

        StringBuilder sb = new StringBuilder()
                .append(event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploading" : "Downloading").append(":")
                .append(resource.getRepositoryUrl()).append(resource.getResourceName());

        downloads.put(resource, new Long(0));
        logger.debug(sb.toString());
    }

    @Override
    public void transferProgressed(TransferEvent event) {
        TransferResource resource = event.getResource();

        long lastTransferred = downloads.get(resource);
        long transferred = event.getTransferredBytes();

        if (transferred - lastTransferred >= TRANSFER_THRESHOLD) {
            downloads.put(resource, Long.valueOf(transferred));
            long total = resource.getContentLength();
            logger.trace(getStatus(transferred, total) + ", ");
        }
    }

    @Override
    public void transferSucceeded(TransferEvent event) {
        TransferResource resource = event.getResource();

        downloads.remove(resource);

        long contentLength = event.getTransferredBytes();
        if (contentLength >= 0) {
            long duration = System.currentTimeMillis() - resource.getTransferStartTime();
            double kbPerSec = (contentLength / 1024.0) / (duration / 1000.0);

            StringBuilder sb = new StringBuilder().append("Completed")
                    .append(event.getRequestType() == TransferEvent.RequestType.PUT ? " upload of " : " download of ")
                    .append(resource.getResourceName())
                    .append(event.getRequestType() == TransferEvent.RequestType.PUT ? " into " : " from ")
                    .append(resource.getRepositoryUrl()).append(", transferred ")
                    .append(contentLength >= 1024 ? toKB(contentLength) + " KB" : contentLength + " B").append(" at ")
                    .append(new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.ENGLISH)).format(kbPerSec))
                    .append("KB/sec");

            logger.debug(sb.toString());
        }
    }

    @Override
    public void transferFailed(TransferEvent event) {
        TransferResource resource = event.getResource();

        downloads.remove(resource);

        StringBuilder sb = new StringBuilder().append("Failed")
                .append(event.getRequestType() == TransferEvent.RequestType.PUT ? " uploading " : " downloading ")
                .append(resource.getResourceName())
                .append(event.getRequestType() == TransferEvent.RequestType.PUT ? " into " : " from ")
                .append(resource.getRepositoryUrl()).append(". ");

        if (event.getException() != null) {
            sb.append("Reason: \n").append(event.getException());
        }

        logger.warn(sb.toString());
    }

    @Override
    public void transferCorrupted(TransferEvent event) {
        TransferResource resource = event.getResource();

        downloads.remove(resource);

        StringBuilder sb = new StringBuilder().append("Corrupted")
                .append(event.getRequestType() == TransferEvent.RequestType.PUT ? " upload of " : " download of ")
                .append(resource.getResourceName())
                .append(event.getRequestType() == TransferEvent.RequestType.PUT ? " into " : " from ")
                .append(resource.getRepositoryUrl()).append(". ");

        if (event.getException() != null) {
            sb.append("Reason: \n").append(event.getException());
        }

        logger.warn(sb.toString());

    }

    private String getStatus(long complete, long total) {
        if (total >= 1024) {
            return toKB(complete) + "/" + toKB(total) + " KB";
        } else if (total >= 0) {
            return complete + "/" + total + " B";
        } else if (complete >= 1024) {
            return toKB(complete) + " KB";
        } else {
            return complete + " B";
        }
    }

    private long toKB(long bytes) {
        return (bytes + 1023) / 1024;
    }

}