package com.redhat.repository.validator.impl.suspicious;

import static com.redhat.repository.validator.impl.suspicious.TestSuspiciousFileValidator.touch;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import com.redhat.repository.validator.impl.AbstractTest;
import com.redhat.repository.validator.impl.suspicious.SuspiciousFileException;

@ContextConfiguration(locations = "classpath*:TestSuspiciousFileValidatorWithFilterInGroovy-context.xml", inheritLocations = false)
public class TestSuspiciousFileValidatorWithFilterInGroovy extends AbstractTest {

    @Test
    public void shouldUseFilterDefinedInGroovy() {
        touch("readme.txt");
        touch("expected-file.groovy");
        touch("unexpected-file");

        validationExecutor.execute(ctx);

        assertEquals(ctx.getExceptions().size(), 1);
        assertExpectedException(SuspiciousFileException.class, "File unexpected-file is suspicious");
    }

}