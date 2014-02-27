package org.jboss.wolf.validator.impl.suspicious;

import static org.jboss.wolf.validator.impl.suspicious.TestSuspiciousFileValidator.touch;
import static org.junit.Assert.assertEquals;

import org.jboss.wolf.validator.impl.AbstractTest;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = "classpath*:TestSuspiciousFileValidatorWithFilterInXml-context.xml", inheritLocations = false)
public class TestSuspiciousFileValidatorWithFilterInXml extends AbstractTest {

    @Test
    public void shouldUseFilterDefinedInXml() {
        touch("readme.txt");
        touch("expected-file.xml");
        touch("unexpected-file");

        validationExecutor.execute(ctx);

        assertEquals(ctx.getExceptions().size(), 1);
        assertExpectedException(SuspiciousFileException.class, "File unexpected-file is suspicious");
    }

}