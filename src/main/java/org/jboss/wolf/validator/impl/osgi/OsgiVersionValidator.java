package org.jboss.wolf.validator.impl.osgi;

import static org.jboss.wolf.validator.internal.Utils.relativize;

import java.util.Iterator;

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
public class OsgiVersionValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(OsgiVersionValidator.class);

    @Inject @Named("osgiVersionValidatorFilter")
    private IOFileFilter fileFilter;
    @Inject
    private ValidatorSupport validatorSupport;

    @Override
    public void validate(ValidatorContext ctx) {
        Iterator<Model> modelIterator = validatorSupport.effectiveModelIterator(ctx, fileFilter);
        while (modelIterator.hasNext()) {
            Model model = modelIterator.next();
            if (model != null) {
                logger.trace("validating {}", relativize(ctx, model.getPomFile()));
                validateVersion(ctx, model);
            }
        }
    }

    private void validateVersion(ValidatorContext ctx, Model model) {
        try {
            new OsgiVersion(model.getVersion());
        } catch (Exception e) {
            ctx.addError(this, model.getPomFile(), new OsgiVersionException(model.getId(), e));
        }
    }

}