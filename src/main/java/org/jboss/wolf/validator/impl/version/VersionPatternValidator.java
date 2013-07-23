package org.jboss.wolf.validator.impl.version;

import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.model.Model;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorContext;
import org.jboss.wolf.validator.internal.ValidatorSupport;

@Named
public class VersionPatternValidator implements Validator {

    private static final String DEFAULT_REDHAT_VERSION_PATTERN = ".+-redhat-[0-9]+";

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
        List<Model> models = validatorSupport.resolveEffectiveModels(ctx, fileFilter);
        for (Model model : models) {
            if (!pattern.matcher(model.getVersion()).matches()) {
                Exception e = new VersionPatternException(model.getId(), pattern.pattern());
                ctx.addException(model.getPomFile(), e);
            }
        }
    }

}