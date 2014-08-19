package org.jboss.wolf.validator.impl.remoterepository;

import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorContext;

public class RemoteRepositoryCompareValidator implements Validator {

    private static final String[] ARTIFACT_FILE_EXTENSIONS = { "pom", "jar", "war", "ear", "par", "rar", "zip", "aar", "apklib" };

    private final int maxConnTotal;
    private final String remoteRepositoryUrl;
    private final RemoteRepositoryCompareStrategy compareStrategy;
    private final IOFileFilter fileFilter;

    public RemoteRepositoryCompareValidator(String remoteRepositoryUrl, RemoteRepositoryCompareStrategy compareStrategy) {
        this(remoteRepositoryUrl, compareStrategy, FileFilterUtils.trueFileFilter(), 20);
    }

    public RemoteRepositoryCompareValidator(String remoteRepositoryUrl, RemoteRepositoryCompareStrategy compareStrategy, IOFileFilter fileFilter, int maxConnTotal) {
        super();
        this.remoteRepositoryUrl = remoteRepositoryUrl;
        this.compareStrategy = compareStrategy;
        this.fileFilter = fileFilter;
        this.maxConnTotal = maxConnTotal;
    }

    @Override
    public void validate(final ValidatorContext ctx) {
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
                                compareStrategy.compareArtifact(httpClient, localArtifact, remoteArtifact);
                            } catch (RemoteRepositoryCompareException e) {
                                ctx.addError(RemoteRepositoryCompareValidator.this, file, e);
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

    private Collection<File> findFiles(ValidatorContext ctx) {
        IOFileFilter artifactsFilter = new SuffixFileFilter(ARTIFACT_FILE_EXTENSIONS);
        Collection<File> files = listFiles(ctx.getValidatedRepository(), and(fileFilter, artifactsFilter), trueFileFilter());
        return files;
    }

}