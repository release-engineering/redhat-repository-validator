package org.jboss.wolf.validator.impl;

import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorContext;

public class DelegatingValidator implements Validator {

    private final Validator[] validators;

    public DelegatingValidator(Validator... validators) {
        this.validators = validators;
    }

    @Override
    public void validate(ValidatorContext ctx) {
        for (Validator validator : validators) {
            validator.validate(ctx);
        }
    }

}