package org.jboss.wolf.validator.impl.version;

import static org.jboss.wolf.validator.internal.Utils.relativize;

import java.util.Iterator;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.model.Model;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorContext;
import org.jboss.wolf.validator.internal.ValidatorSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class VersionPatternValidator implements Validator {
    
    private static final Logger logger = LoggerFactory.getLogger(VersionPatternValidator.class);

    private static final String DEFAULT_REDHAT_VERSION_PATTERN = ".+[\\.-]redhat-[0-9]+";

    @Inject @Named("versionPatternValidatorFilter")
    private IOFileFilter fileFilter;
    @Inject
    private ValidatorSupport validatorSupport;

    private final Pattern pattern;

    public VersionPatternValidator() {
        this(DEFAULT_REDHAT_VERSION_PATTERN);
    }

    public VersionPatternValidator(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    @Override
    public void validate(ValidatorContext ctx) {
        Iterator<Model> modelIterator = validatorSupport.effectiveModelIterator(ctx, fileFilter);
        while (modelIterator.hasNext()) {
            Model model = modelIterator.next();
            if (model != null) {
                logger.trace("validating {}", relativize(ctx, model.getPomFile()));
                validateVersionPattern(ctx, model);
            }
        }
    }

    private void validateVersionPattern(ValidatorContext ctx, Model model) {
        if (!pattern.matcher(model.getVersion()).matches()) {
            Exception e = new VersionPatternException(model.getId(), pattern.pattern());
            ctx.addError(this, model.getPomFile(), e);
        }
    }

}