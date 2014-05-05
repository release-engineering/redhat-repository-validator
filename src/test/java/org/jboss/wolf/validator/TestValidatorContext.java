package org.jboss.wolf.validator;

import org.eclipse.aether.repository.RemoteRepository;
import org.jboss.wolf.validator.filter.FilenameBasedExceptionFilter;
import org.jboss.wolf.validator.impl.source.JarSourcesVerificationException;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class TestValidatorContext {

    @Test
    public void shouldStoreFilteredExceptions() {
        ValidatorContext ctx = new ValidatorContext(new File(""), new File(""), Collections.<RemoteRepository>emptyList());
        Exception exception = new Exception();
        ctx.addException(new File(""), exception);
        JarSourcesVerificationException jarSourcesEx = new JarSourcesVerificationException(new File(""));
        ctx.addException(new File("some-file.jar"), jarSourcesEx);

        ExceptionFilter filter = new FilenameBasedExceptionFilter("some-file.jar", JarSourcesVerificationException.class);
        ctx.applyExceptionFilters(new ExceptionFilter[]{filter});

        assertEquals("Number of filtered exceptions", 1, ctx.getFilteredExceptions().size());
        assertEquals("Filtered exception", jarSourcesEx, ctx.getFilteredExceptions().get(0));

        assertEquals("Number of non-filtered reported exceptions", 1, ctx.getExceptions().size());
        assertEquals("Non-filtered exception", exception, ctx.getExceptions().get(0));
    }

}
