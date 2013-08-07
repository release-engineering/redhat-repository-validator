package org.jboss.wolf.validator.impl;

import static org.jboss.wolf.validator.impl.TestUtil.pom;

import org.apache.maven.model.Model;
import org.junit.Test;

public class TestDependencyNotFoundReporter extends AbstractReporterTest {
    
    @Test
    public void shouldReportMissingDependency() {
        Model fooApi = pom().artifactId("foo-api").build();
        pom().artifactId("foo-impl").dependency(fooApi).create(repoFooDir);

        validator.validate(ctx);
        reporter.report(ctx);

        assertReportContains(
                  "Found 1 missing dependencies.\n"
                + "miss: com.acme:foo-api:jar:1.0\n"
                + "    from: com.acme:foo-impl:pom:1.0\n"
                + "        path: com.acme:foo-impl:pom:1.0 > com.acme:foo-api:jar:1.0");
    }

    @Test
    public void shouldReportMissingDependencies() {
        Model barApi = pom().artifactId("bar-api").build();
        Model barImpl = pom().artifactId("bar-impl").dependency(barApi).create(repoBarDir);
        Model fooApi = pom().artifactId("foo-api").build();
        pom().artifactId("foo-impl").dependency(fooApi).dependency(barImpl).create(repoFooDir);
        pom().artifactId("foo-dist").dependency(fooApi).create(repoFooDir);

        validator.validate(ctx);
        reporter.report(ctx);

        assertReportContains(
                  "Found 2 missing dependencies.\n"
                + "miss: com.acme:bar-api:jar:1.0\n"
                + "    from: com.acme:foo-impl:pom:1.0\n"
                + "        path: com.acme:foo-impl:pom:1.0 > com.acme:bar-impl:jar:1.0 > com.acme:bar-api:jar:1.0\n"
                + "miss: com.acme:foo-api:jar:1.0\n"
                + "    from: com.acme:foo-dist:pom:1.0\n"
                + "        path: com.acme:foo-dist:pom:1.0 > com.acme:foo-api:jar:1.0\n"
                + "    from: com.acme:foo-impl:pom:1.0\n"
                + "        path: com.acme:foo-impl:pom:1.0 > com.acme:foo-api:jar:1.0");
    }

}