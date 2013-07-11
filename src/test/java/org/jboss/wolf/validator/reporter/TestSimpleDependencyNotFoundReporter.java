package org.jboss.wolf.validator.reporter;

import static org.jboss.wolf.validator.impl.TestUtil.pom;

import org.apache.maven.model.Model;
import org.junit.Test;

public class TestSimpleDependencyNotFoundReporter extends AbstractReporterTest {

    @Test
    public void shouldReportMissingDependencies() {
        Model barApi = pom().artifactId("bar-api").build();
        Model barImpl = pom().artifactId("bar-impl").dependency(barApi).create(repoBarDir);
        Model fooApi = pom().artifactId("foo-api").build();
        pom().artifactId("foo-impl").dependency(fooApi).dependency(barImpl).create(repoFooDir);
        pom().artifactId("foo-dist").dependency(fooApi).create(repoFooDir);

        validator.validate(ctx);
        reporter.report(ctx);

        assertReportContains("Found 2 missing dependencies");
        assertReportContains("com.acme:bar-api:jar:1.0\n    in: com.acme:foo-impl:pom:1.0");
        assertReportContains("com.acme:foo-api:jar:1.0\n    in: com.acme:foo-dist:pom:1.0\n    in: com.acme:foo-impl:pom:1.0");
    }

}