package com.redhat.repository.validator.impl.signature;

import static com.redhat.repository.validator.impl.signature.JarSignatureValidatorMode.VERIFY_JAR_IS_SIGNED;
import static com.redhat.repository.validator.impl.signature.JarSignatureValidatorMode.VERIFY_JAR_IS_UNSIGNED;
import static com.redhat.repository.validator.internal.Utils.relativize;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.suffixFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.repository.validator.Validator;
import com.redhat.repository.validator.ValidatorContext;

@Named
public class JarSignatureValidator implements Validator {
    
    private static final Logger logger = LoggerFactory.getLogger(JarSignatureValidator.class);

    @Inject @Named("jarSignatureValidatorFilter")
    private IOFileFilter fileFilter;

    private final JarSignatureValidatorMode mode;

    public JarSignatureValidator() {
        this(VERIFY_JAR_IS_UNSIGNED);
    }

    public JarSignatureValidator(JarSignatureValidatorMode mode) {
        this.mode = mode;
    }

    @Override
    public void validate(ValidatorContext ctx) {
        Collection<File> files = listFiles(ctx.getValidatedRepository(), and(fileFilter, suffixFileFilter(".jar")), trueFileFilter());
        for (File file : files) {
            logger.trace("validating {}", relativize(ctx, file));
            validateSignature(ctx, file);
        }
    }

    private void validateSignature(ValidatorContext ctx, File file) {
        File fileRelative = relativize(ctx, file);
        try {
            ProcessBuilder pb = new ProcessBuilder("jarsigner", "-verify", file.getAbsolutePath());
            Process p = pb.start();
            p.waitFor();
            String output = IOUtils.toString(p.getInputStream());
            if (p.exitValue() == 0 && output.contains("jar is unsigned")) {
                if (mode == VERIFY_JAR_IS_SIGNED) {
                    ctx.addError(this, file, new JarUnsignedException(fileRelative));
                }
            } else if (p.exitValue() == 0 && output.contains("jar verified")) {
                if (mode == VERIFY_JAR_IS_UNSIGNED) {
                    ctx.addError(this, file, new JarSignedException(fileRelative));
                }
            } else {
                ctx.addError(this, file, new JarSignatureVerificationException(fileRelative, output));
            }
        } catch (InterruptedException e) {
            ctx.addError(this, file, new JarSignatureVerificationException(fileRelative, e));
        } catch (IOException e) {
            ctx.addError(this, file, new JarSignatureVerificationException(fileRelative, e));
        }
    }

}