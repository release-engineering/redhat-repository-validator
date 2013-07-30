package org.jboss.wolf.validator.impl.signature;

import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.suffixFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;
import static org.jboss.wolf.validator.impl.signature.JarSignatureValidatorMode.VERIFY_JAR_IS_SIGNED;
import static org.jboss.wolf.validator.impl.signature.JarSignatureValidatorMode.VERIFY_JAR_IS_UNSIGNED;
import static org.jboss.wolf.validator.internal.Utils.relativize;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorContext;

@Named
public class JarSignatureValidator implements Validator {

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
                    ctx.addException(file, new JarUnsignedException(fileRelative));
                }
            } else if (p.exitValue() == 0 && output.contains("jar verified")) {
                if (mode == VERIFY_JAR_IS_UNSIGNED) {
                    ctx.addException(file, new JarSignedException(fileRelative));
                }
            } else {
                ctx.addException(file, new JarSignatureVerificationException(fileRelative, output));
            }
        } catch (InterruptedException e) {
            ctx.addException(file, new JarSignatureVerificationException(fileRelative, e));
        } catch (IOException e) {
            ctx.addException(file, new JarSignatureVerificationException(fileRelative, e));
        }
    }

}