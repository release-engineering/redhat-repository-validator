package org.jboss.wolf.validator.impl;

import static org.apache.commons.io.FileUtils.listFilesAndDirs;
import static org.apache.commons.io.FilenameUtils.isExtension;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.jboss.wolf.validator.internal.Utils.relativize;

import java.io.File;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class SuspiciousFileValidator implements Validator {
    
    private static final Logger logger = LoggerFactory.getLogger(SuspiciousFileValidator.class);
    
    private static final String[] CHECKSUM_EXTENSIONS = { "sha1", "md5" };
    private static final String[] ATTACHED_ARTIFACT_TYPES = { "-sources.jar", "-javadoc.jar", "-tests.jar" };
    private static final String[] ROOT_FILES = { "readme.md", "readme.txt", "example-settings.xml", "JBossEULA.txt" };

    private final String[] rootFiles;
    private final String[] attachedArtifactTypes;
    private final String[] checsumExtensions;
    
    @Inject @Named("suspiciousFileValidatorFilter")
    private IOFileFilter fileFilter;
    
    public SuspiciousFileValidator() {
        this.rootFiles = ROOT_FILES;
        this.attachedArtifactTypes = ATTACHED_ARTIFACT_TYPES;
        this.checsumExtensions = CHECKSUM_EXTENSIONS;
    }

    public SuspiciousFileValidator(String[] rootFiles, String[] attachedArtifactTypes, String[] checsumExtensions) {
        super();
        this.rootFiles = rootFiles;
        this.attachedArtifactTypes = attachedArtifactTypes;
        this.checsumExtensions = checsumExtensions;
    }

    @Override
    public void validate(ValidatorContext ctx) {
        logger.debug("start...");
        Collection<File> files = listFilesAndDirs(ctx.getValidatedRepository(), fileFilter, fileFilter);
        for (File file : files) {
            validateFile(ctx, file);
        }
    }

    private void validateFile(ValidatorContext ctx, File file) {
        String fileName = file.getName();
        String fileDir = file.getParent();
        
        if (file.isDirectory()) {
            if (file.list().length == 0) {
                fail(ctx, file, "empty directory");
            }
            return;
        }

        if (fileName.endsWith(".pom")) {
            return;
        }

        for (String checksumExtension : checsumExtensions) {
            if (isExtension(fileName, checksumExtension)) {
                File sourceFile = new File(fileDir, removeExtension(fileName));
                if (!sourceFile.isFile()) {
                    fail(ctx, file, "checksum without source file");
                }
                return;
            }
        }

        for (String attachedArtifact : attachedArtifactTypes) {
            if (fileName.endsWith(attachedArtifact)) {
                File primaryArtifact = new File(fileDir, removeEnd(fileName, attachedArtifact) + ".jar");
                if (!primaryArtifact.isFile()) {
                    fail(ctx, file, "artifact " + removeStart(attachedArtifact, "-") + " without primary jar file");
                }
                return;
            }
        }
        
        if (fileName.endsWith(".jar")) {
            File pomFile = new File(fileDir, removeExtension(fileName) + ".pom");
            if (!pomFile.isFile()) {
                fail(ctx, file, "jar file without pom");
            }
            return;
        }

        if (ctx.getValidatedRepository().getAbsolutePath().equals(file.getParentFile().getAbsolutePath())) {
            for (String rootFile : rootFiles) {
                if (fileName.equalsIgnoreCase(rootFile)) {
                    return; // expected files
                }
            }
        }

        fail(ctx, file, "suspicious");
    }
    
    private void fail(ValidatorContext ctx, File file, String msg) {
        ctx.addException(file, new SuspiciousFileException(relativize(ctx, file), msg));
    }

}