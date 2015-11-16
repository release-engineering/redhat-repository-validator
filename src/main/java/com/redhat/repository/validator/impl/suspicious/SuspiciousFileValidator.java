package com.redhat.repository.validator.impl.suspicious;

import static com.redhat.repository.validator.internal.Utils.relativize;
import static org.apache.commons.io.FileUtils.listFilesAndDirs;
import static org.apache.commons.io.FilenameUtils.isExtension;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;

import java.io.File;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.repository.validator.Validator;
import com.redhat.repository.validator.ValidatorContext;

@Named
public class SuspiciousFileValidator implements Validator {
    
    private static final Logger logger = LoggerFactory.getLogger(SuspiciousFileValidator.class);
    
    private static final String[] POM_EXTENSION = { "pom" };
    private static final String[] CHECKSUM_EXTENSIONS = { "sha1", "md5" };
    private static final String[] ATTACHED_ARTIFACT_TYPES = { "-javadoc.jar", "-tests.jar", "-test-sources.jar", "-sources.jar" };
    private static final String[] ALLOWED_ARTIFACT_FILE_EXTENIONS = { "jar", "war", "ear", "par", "rar", "zip", "aar", "apklib" };

    private final String[] attachedArtifactTypes;
    private final String[] checsumExtensions;
    private final String[] allowedArtifactFileExtensions;
    
    @Inject @Named("suspiciousFileValidatorFilter")
    private IOFileFilter fileFilter;

    public SuspiciousFileValidator() {
        this.attachedArtifactTypes = ATTACHED_ARTIFACT_TYPES;
        this.checsumExtensions = CHECKSUM_EXTENSIONS;
        this.allowedArtifactFileExtensions = ALLOWED_ARTIFACT_FILE_EXTENIONS;
    }

    public SuspiciousFileValidator(String[] attachedArtifactTypes, String[] checsumExtensions, String[] allowedArtifactFileExtensions) {
        super();
        this.attachedArtifactTypes = attachedArtifactTypes;
        this.checsumExtensions = checsumExtensions;
        this.allowedArtifactFileExtensions = allowedArtifactFileExtensions;
    }

    @Override
    public void validate(ValidatorContext ctx) {
        Collection<File> files = listFilesAndDirs(ctx.getValidatedRepository(), fileFilter, fileFilter);
        for (File file : files) {
            logger.trace("validating {}", relativize(ctx, file));
            validateFile(ctx, file);
        }
    }

    private void validateFile(ValidatorContext ctx, File file) {
        String fileName = file.getName();
        String fileDir = file.getParent();
        
        if (file.isDirectory() && file.getAbsolutePath().equals(ctx.getValidatedRepository().getAbsolutePath())) {
            return;
        }

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

        for (String attachedArtifactType : attachedArtifactTypes) {
            if (fileName.endsWith(attachedArtifactType)) {
                if (!existPrimaryArtifact(fileDir, fileName, attachedArtifactType)) {
                    fail(ctx, file, "artifact " + removeStart(attachedArtifactType, "-") + " without primary artifact");
                }
                return;
            }
        }

        final String extension = FilenameUtils.getExtension(fileName); 
        
        if (endsOnKnownFileExtension(fileName)) {
            File pomFile = new File(fileDir, removeExtension(fileName) + ".pom");
            if (!pomFile.isFile()) {
                Collection<File> pomFiles = FileUtils.listFiles(file.getParentFile(), POM_EXTENSION, false);
                if (pomFiles.isEmpty()) {
                    fail(ctx, file, extension + " file without pom");
                } else if (pomFiles.size() == 1) {
                    fail(ctx, file, extension + " file without pom, but there is other pom in directory " + pomFiles.iterator().next().getName());
                } else {
                    fail(ctx, file, extension + " file without pom, but there are other poms in directory");
                }
            }
            return;
        }

        fail(ctx, file, "suspicious");
    }

    private void fail(ValidatorContext ctx, File file, String msg) {
        ctx.addError(this, file, new SuspiciousFileException(relativize(ctx, file), msg));
    }
    
    private boolean endsOnKnownFileExtension(final String fileName) {
        boolean endsOnKnownFileExtension = false;

        for (final String suffix : allowedArtifactFileExtensions) {
            if (fileName.endsWith("." + suffix)) {
                endsOnKnownFileExtension = true;
                break;
            }
        }

        return endsOnKnownFileExtension;
    }
    
    private boolean existPrimaryArtifact(String fileDir, String fileName, String attachedArtifactType) {
        String fileNameWithoutExtension = removeEnd(fileName, attachedArtifactType);
        for (String allowedArtifactFileExtension : allowedArtifactFileExtensions) {
            File primaryArtifact = new File(fileDir, fileNameWithoutExtension + "." + allowedArtifactFileExtension);
            if (primaryArtifact.isFile()) {
                return true;
            }
        }
        return false;
    }
    
}