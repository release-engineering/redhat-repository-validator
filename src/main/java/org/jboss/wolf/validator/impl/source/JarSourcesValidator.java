package org.jboss.wolf.validator.impl.source;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.Collection;

import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.apache.commons.io.filefilter.FileFilterUtils.*;
import static org.jboss.wolf.validator.internal.Utils.relativize;

@Named
public class JarSourcesValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(JarSourcesValidator.class);

    @Inject
    @Named("jarSourcesValidatorFilter")
    private IOFileFilter fileFilter;

    @Override
    public void validate(ValidatorContext ctx) {
        Collection<File> files = listFiles(ctx.getValidatedRepository(), and(fileFilter, excludeUncheckedJarsFilter()), trueFileFilter());
        for (File file : files) {
            logger.trace("validating {}", relativize(ctx, file));
            validateSources(ctx, file);
        }
    }

    private void validateSources(ValidatorContext ctx, File file) {
        String jarName = file.getName();
        String jarDir = file.getParent();
        if (file.isDirectory() && file.getAbsolutePath().equals(ctx.getValidatedRepository().getAbsolutePath())) {
            return;
        }
        File sourcesFile = new File(jarDir, removeExtension(jarName) + "-sources.jar");
        if (!sourcesFile.exists() || !sourcesFile.isFile()) {
            ctx.addException(file, new JarSourcesVerificationException(relativize(ctx, file)));
        }
    }

    public IOFileFilter excludeSourcesJarFilter() {
        return notFileFilter(suffixFileFilter("-sources.jar"));
    }

    public IOFileFilter excludeJavaDocJarFilter() {
        return notFileFilter(suffixFileFilter("-javadoc.jar"));
    }

    public IOFileFilter excludeTestsJarFilter() {
        return notFileFilter(suffixFileFilter("-tests.jar"));
    }

    public IOFileFilter excludeUncheckedJarsFilter() {
        return and(excludeSourcesJarFilter(), excludeJavaDocJarFilter(), excludeTestsJarFilter(), suffixFileFilter(".jar"));
    }
}