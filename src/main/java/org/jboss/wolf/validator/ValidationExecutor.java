package org.jboss.wolf.validator;

import static org.springframework.core.annotation.AnnotationAwareOrderComparator.sort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationExecutor.class);

    private final Validator[] validators;

    public ValidationExecutor(Validator... validators) {
        sort(validators);
        this.validators = validators;
    }

    public Validator[] getValidators() {
        return validators;
    }

    public void execute(ValidatorContext ctx) {
        logValidatorNames();
        for (Validator validator : validators) {
            logger.debug("starting {}", validator.getClass().getSimpleName());
            try {
                validator.validate(ctx);
            } catch (RuntimeException e) {
                logger.error("validator " + validator.getClass().getSimpleName() + " ended with unexpected exception!", e);
                ctx.addException(ctx.getValidatedRepository(), e);
            }
        }
    }

    private void logValidatorNames() {
        StringBuilder validatorNamesBuilder = new StringBuilder();
        for (Validator validator : validators) {
            validatorNamesBuilder.append("\t");
            validatorNamesBuilder.append(validator.getClass().getCanonicalName());
            validatorNamesBuilder.append("\n");
        }
        logger.debug("following validators will be executed:\n" + validatorNamesBuilder);
    }

}
