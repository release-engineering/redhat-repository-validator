package org.jboss.wolf.validator.impl.remoterepository;

import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;
import static org.jboss.wolf.validator.internal.Utils.relativize;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RemoteRepositoryAbstractValidator implements Validator {
    
    private static final String[] ARTIFACT_FILE_EXTENSIONS = { "pom", "jar", "war", "ear", "par", "rar", "zip", "aar", "apklib" };

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    protected final int maxConnTotal;
    protected final String remoteRepositoryUrl;
    protected final IOFileFilter fileFilter;

    public RemoteRepositoryAbstractValidator(String remoteRepositoryUrl, IOFileFilter fileFilter, int maxConnTotal) {
        super();
        this.remoteRepositoryUrl = remoteRepositoryUrl;
        this.fileFilter = fileFilter;
        this.maxConnTotal = maxConnTotal;
    }

    @Override
    public final void validate(final ValidatorContext ctx) {
        final ExecutorService executorService = Executors.newFixedThreadPool(maxConnTotal);
        final CloseableHttpClient httpClient = HttpClients.custom().setMaxConnTotal(maxConnTotal).build();

        try {
            for (final File file : findFiles(ctx)) {
                try {
                    final URI repoUri = ctx.getValidatedRepository().toURI();
                    final URI localArtifact = file.toURI();
                    final URI remoteArtifact = new URI(remoteRepositoryUrl + repoUri.relativize(localArtifact).toString());

                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                logger.trace("validating {}", relativize(ctx, file));
                                validateArtifact(httpClient, localArtifact, remoteArtifact);
                            } catch (Exception e) {
                                ctx.addError(RemoteRepositoryAbstractValidator.this, file, e);
                            }
                        }
                    });

                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }

            try {
                executorService.shutdown();
                executorService.awaitTermination(1, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected Collection<File> findFiles(ValidatorContext ctx) {
        IOFileFilter artifactsFilter = new SuffixFileFilter(ARTIFACT_FILE_EXTENSIONS);
        Collection<File> files = listFiles(ctx.getValidatedRepository(), and(fileFilter, artifactsFilter), trueFileFilter());
        return files;
    }
    
    protected abstract void validateArtifact(CloseableHttpClient httpClient, URI localArtifact, URI remoteArtifact) throws Exception;
    
    @Override
    public String toString() {
        return new ToStringBuilder(this).append(remoteRepositoryUrl).toString();
    }

}