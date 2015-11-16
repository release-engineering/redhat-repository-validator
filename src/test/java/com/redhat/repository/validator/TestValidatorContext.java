package com.redhat.repository.validator;

import org.eclipse.aether.repository.RemoteRepository;
import org.junit.Test;

import com.redhat.repository.validator.ExceptionFilter;
import com.redhat.repository.validator.ValidatorContext;
import com.redhat.repository.validator.filter.FilenameBasedExceptionFilter;
import com.redhat.repository.validator.impl.source.JarSourcesVerificationException;

import java.io.File;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class TestValidatorContext {

    @Test
    public void shouldStoreFilteredExceptions() {
        ExceptionFilter filter = new FilenameBasedExceptionFilter("some-file.jar", JarSourcesVerificationException.class);
        ValidatorContext ctx = new ValidatorContext(new File(""), new File(""), Collections.<RemoteRepository>emptyList(), Collections.singletonList(filter));
        Exception exception = new Exception();
        ctx.addError(null, new File(""), exception);
        JarSourcesVerificationException jarSourcesEx = new JarSourcesVerificationException(new File(""));
        ctx.addError(null, new File("some-file.jar"), jarSourcesEx);

        assertEquals("Number of filtered exceptions", 1, ctx.getIgnoredExceptions().size());
        assertEquals("Filtered exception", jarSourcesEx, ctx.getIgnoredExceptions().get(0));

        assertEquals("Number of non-filtered reported exceptions", 1, ctx.getExceptions().size());
        assertEquals("Non-filtered exception", exception, ctx.getExceptions().get(0));
    }

}