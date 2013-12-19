package org.jboss.wolf.validator.impl;

import static org.apache.commons.io.filefilter.FileFilterUtils.nameFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.notFileFilter;
import static org.jboss.wolf.validator.impl.TestUtil.createArtifact;
import static org.jboss.wolf.validator.impl.TestUtil.dependency;
import static org.jboss.wolf.validator.impl.TestUtil.pom;
import static org.jboss.wolf.validator.impl.TestUtil.toArtifactFile;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.model.Activation;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import com.google.common.io.Files;

@ContextConfiguration
public class TestDependenciesValidator extends AbstractTest {
    
    @Configuration
    public static class TestConfiguration {

        @Bean
        public IOFileFilter dependenciesValidatorFilter() {
            return new TestFileFilter();
        }

    }
    
    @Test
    public void shouldResolvePom() {
        Model foo = pom().artifactId("foo").packaging("pom").create(repoFooDir);

        validator.validate(ctx);

        assertSuccess();
        assertLocalRepoContains(foo);
    }

    @Test
    public void shouldResolvePomAndJar() {
        Model foo = pom().artifactId("foo").create(repoFooDir);

        validator.validate(ctx);

        assertSuccess();
        assertLocalRepoContains(foo);
    }

    @Test
    public void shouldResolveParent() {
        Model fooParent = pom().artifactId("foo-parent").packaging("pom").create(repoFooDir);
        Model fooApi = pom().artifactId("foo-api").parent(fooParent).create(repoFooDir);

        fileFilter = notFileFilter(nameFileFilter("foo-parent-1.0.pom"));
        validator.validate(ctx);

        assertSuccess();
        assertLocalRepoContains(fooApi);
        assertLocalRepoContains(fooParent);
    }
    
    @Test
    public void shouldResolveParentFromCentral() {
        Model jbossParent = pom().artifactId("jboss-parent").groupId("org.jboss").version("6").packaging("pom").model();
        Model fooApi = pom().artifactId("foo-api").parent(jbossParent).create(repoFooDir);

        validator.validate(ctx);

        assertSuccess();
        assertLocalRepoContains(fooApi);
        assertLocalRepoContains(jbossParent);
    }

    @Test
    public void shouldResolveGrandParent() {
        Model barParent = pom().artifactId("bar-parent").packaging("pom").create(repoBarDir);
        Model fooParent = pom().artifactId("foo-parent").packaging("pom").parent(barParent).create(repoFooDir);
        Model fooApi = pom().artifactId("foo-api").parent(fooParent).create(repoFooDir);

        fileFilter = notFileFilter(nameFileFilter("foo-parent-1.0.pom"));
        validator.validate(ctx);

        assertSuccess();
        assertLocalRepoContains(fooApi);
        assertLocalRepoContains(fooParent);
        assertLocalRepoContains(barParent);
    }

    @Test
    public void shouldResolveDirectDependency() {
        Model fooApi = pom().artifactId("foo-api").create(repoFooDir);
        Model fooImpl = pom().artifactId("foo-impl").dependency(fooApi).create(repoFooDir);

        fileFilter = notFileFilter(nameFileFilter("foo-api-1.0.pom"));
        validator.validate(ctx);

        assertSuccess();
        assertLocalRepoContains(fooApi);
        assertLocalRepoContains(fooImpl);
    }

    @Test
    public void shouldResolveTransitiveDependency() {
        Model barApi = pom().artifactId("bar-api").create(repoBarDir);
        Model fooApi = pom().artifactId("foo-api").dependency(barApi).create(repoFooDir);
        Model fooImpl = pom().artifactId("foo-impl").dependency(fooApi).create(repoFooDir);

        fileFilter = notFileFilter(nameFileFilter("foo-api-1.0.pom"));
        validator.validate(ctx);

        assertSuccess();
        assertLocalRepoContains(barApi);
        assertLocalRepoContains(fooApi);
        assertLocalRepoContains(fooImpl);
    }

    @Test
    public void shouldResolveDependencyFromCentral() {
        Model lang = pom().groupId("commons-lang").artifactId("commons-lang").version("2.6").model();
        Model foo = pom().artifactId("foo").dependency(lang).create(repoFooDir);

        validator.validate(ctx);

        assertSuccess();
        assertLocalRepoContains(foo);
        assertLocalRepoContains(lang);
    }

    @Test
    public void shouldResolveDependencyWithClassifier() throws IOException {
        Model bar = pom().artifactId("bar").create(repoBarDir);
        pom().artifactId("foo").dependency(dependency().to(bar).classifier("jdk14").build()).create(repoFooDir);

        File barFile = toArtifactFile(repoBarDir, bar);
        File barJdk4File = new File(barFile.getParentFile(), "bar-1.0-jdk14.jar");
        Files.move(barFile, barJdk4File);

        validator.validate(ctx);

        assertSuccess();
        assertLocalRepoContains("com/acme/bar/1.0/bar-1.0.pom");
        assertLocalRepoContains("com/acme/bar/1.0/bar-1.0-jdk14.jar");
    }

    @Test
    public void shouldResolveDependencyWithType() throws IOException {
        Model bar = pom().artifactId("bar").create(repoBarDir);
        pom().artifactId("foo").dependency(dependency().to(bar).type("test-jar").build()).create(repoFooDir);

        File barFile = toArtifactFile(repoBarDir, bar);
        File barTestsFile = new File(barFile.getParentFile(), "bar-1.0-tests.jar");
        Files.move(barFile, barTestsFile);

        validator.validate(ctx);

        assertSuccess();
        assertLocalRepoContains("com/acme/bar/1.0/bar-1.0.pom");
        assertLocalRepoContains("com/acme/bar/1.0/bar-1.0-tests.jar");
    }

    @Test
    public void shouldResolveDependencyFromDepMng() {
        Model bar = pom().artifactId("bar").create(repoBarDir);

        Model fooParent = pom().artifactId("foo-parent").packaging("pom")
                .dependencyManagement(bar)
                .create(repoFooDir);

        Model fooApi = pom().artifactId("foo-api")
                .parent(fooParent)
                .dependency(dependency().to(bar).version(null).build())
                .create(repoFooDir);

        fileFilter = notFileFilter(nameFileFilter("foo-parent-1.0.pom"));
        validator.validate(ctx);

        assertSuccess();
        assertLocalRepoContains(bar);
        assertLocalRepoContains(fooParent);
        assertLocalRepoContains(fooApi);
    }

    @Test
    public void shouldResolveOverridenDependencyFromDepMng() {
        Model bar = pom().artifactId("bar").create(repoBarDir);

        Model fooParent = pom().artifactId("foo-parent").packaging("pom")
                .dependencyManagement(dependency().to(bar).version("999").build())
                .create(repoFooDir);

        Model fooApi = pom().artifactId("foo-api")
                .parent(fooParent)
                .dependency(dependency().to(bar).version("1.0").build())
                .create(repoFooDir);

        fileFilter = notFileFilter(nameFileFilter("foo-parent-1.0.pom"));
        validator.validate(ctx);

        assertSuccess();
        assertLocalRepoContains(bar);
        assertLocalRepoContains(fooParent);
        assertLocalRepoContains(fooApi);
    }

    @Test
    public void shouldResolveDependencyFromBOM() {
        Model bar = pom().artifactId("bar").create(repoBarDir);

        Model fooBom = pom().artifactId("foo-bom").packaging("pom")
                .dependencyManagement(bar)
                .create(repoFooDir);

        Model fooApi = pom().artifactId("foo-api")
                .dependencyManagement(dependency().to(fooBom).type("pom").scope("import").build())
                .dependency(dependency().to(bar).version(null).build())
                .create(repoFooDir);

        fileFilter = notFileFilter(nameFileFilter("foo-bom-1.0.pom"));
        validator.validate(ctx);

        assertSuccess();
        assertLocalRepoContains(bar);
        assertLocalRepoContains(fooBom);
        assertLocalRepoContains(fooApi);
    }
    
    @Test
    public void shouldResolveDependencyFromDefaultProfile() {
        Model bar = pom().artifactId("bar").create(repoBarDir);
        Model foo = pom().artifactId("foo").create(repoFooDir);

        Activation fooProfileActivation = new Activation();
        fooProfileActivation.setActiveByDefault(true);

        Profile fooProfile = new Profile();
        fooProfile.setId("fooProfile");
        fooProfile.setActivation(fooProfileActivation);
        fooProfile.addDependency(dependency().to(bar).build());
        foo.addProfile(fooProfile);

        createArtifact(repoFooDir, foo);

        validator.validate(ctx);

        assertSuccess();
        assertLocalRepoContains(foo);
        assertLocalRepoContains(bar);
    }
    
    @Test
    public void shouldResolveDependencyFromActiveProfile() {
        Model bar = pom().artifactId("bar").create(repoBarDir);
        Model foo = pom().artifactId("foo").create(repoFooDir);

        Activation fooProfileActivation = new Activation();
        fooProfileActivation.setJdk("!1.0");

        Profile fooProfile = new Profile();
        fooProfile.setId("fooProfile");
        fooProfile.setActivation(fooProfileActivation);
        fooProfile.addDependency(dependency().to(bar).build());
        foo.addProfile(fooProfile);

        createArtifact(repoFooDir, foo);

        validator.validate(ctx);

        assertSuccess();
        assertLocalRepoContains(foo);
        assertLocalRepoContains(bar);
    }
    
    @Test
    public void shouldResolveCustomPackaging() throws IOException {
        Model foo = pom().artifactId("foo").packaging("bundle").create(repoFooDir);

        File fooBundleFile = toArtifactFile(repoFooDir, foo);
        File fooJarFile = new File(fooBundleFile.getParentFile(), "foo-1.0.jar");
        Files.move(fooBundleFile, fooJarFile);

        validator.validate(ctx);

        assertSuccess();
        assertLocalRepoContains("com/acme/foo/1.0/foo-1.0.pom");
        assertLocalRepoContains("com/acme/foo/1.0/foo-1.0.jar");
    }
    
    @Test // bug WOLF-51
    public void shouldResolveMavenArchetypePackaging() throws IOException {
        Model fooArchetype = pom().artifactId("foo-archetype").packaging("maven-archetype").create(repoFooDir);

        File fooArchetypeFile = toArtifactFile(repoFooDir, fooArchetype);
        File fooJarFile = new File(fooArchetypeFile.getParentFile(), "foo-archetype-1.0.jar");
        Files.move(fooArchetypeFile, fooJarFile);

        validator.validate(ctx);

        assertSuccess();
        assertLocalRepoContains("com/acme/foo-archetype/1.0/foo-archetype-1.0.pom");
        assertLocalRepoContains("com/acme/foo-archetype/1.0/foo-archetype-1.0.jar");
    }
    
    @Test // bug WOLF-51
    public void shouldNotThrowNPEForUnknownArtifactType() {
        pom().artifactId("foo").packaging("unknown").create(repoFooDir);
        validator.validate(ctx);
        assertExpectedException(UnknownArtifactTypeException.class, "Unknown artifact type, packaging is unknown in pom com/acme/foo/1.0/foo-1.0.pom");
    }

    @Test
    public void shouldFindMissingJar() throws IOException {
        Model foo = pom().artifactId("foo").create(repoFooDir);
        
        File fooJar = toArtifactFile(repoFooDir, foo);
        fooJar.delete();

        validator.validate(ctx);

        assertExpectedException(ArtifactNotFoundException.class, "Could not find artifact com.acme:foo:jar:1.0");
    }

    @Test
    public void shouldFindMissingParent() {
        Model fooParent = pom().artifactId("foo-parent").packaging("pom").model();
        pom().artifactId("foo-api").parent(fooParent).create(repoFooDir);

        validator.validate(ctx);

        assertExpectedException(ArtifactNotFoundException.class, "Could not find artifact com.acme:foo-parent:pom:1.0");
    }

    @Test
    public void shouldFindMissingGrandParent() {
        Model barParent = pom().artifactId("bar-parent").packaging("pom").model();
        Model fooParent = pom().artifactId("foo-parent").packaging("pom").parent(barParent).create(repoFooDir);
        pom().artifactId("foo-api").parent(fooParent).create(repoFooDir);

        fileFilter = notFileFilter(nameFileFilter("foo-parent-1.0.pom"));
        validator.validate(ctx);

        assertExpectedException(ArtifactNotFoundException.class, "Could not find artifact com.acme:bar-parent:pom:1.0");
    }

    @Test
    public void shouldFindMissingDirectDependency() {
        Model fooApi = pom().artifactId("foo-api").model();
        pom().artifactId("foo-impl").dependency(fooApi).create(repoFooDir);

        validator.validate(ctx);

        assertExpectedException(ArtifactNotFoundException.class, "Could not find artifact com.acme:foo-api:jar:1.0");
    }

    @Test
    public void shouldFindMissingTransitiveDependency() {
        Model barApi = pom().artifactId("bar-api").model();
        Model fooApi = pom().artifactId("foo-api").dependency(barApi).create(repoFooDir);
        pom().artifactId("foo-impl").dependency(fooApi).create(repoFooDir);

        fileFilter = notFileFilter(nameFileFilter("foo-api-1.0.pom"));
        validator.validate(ctx);

        assertExpectedException(ArtifactNotFoundException.class, "Could not find artifact com.acme:bar-api:jar:1.0");
    }

    @Test
    public void shouldFindMissingDependencyFromCentral() {
        Model lang = pom().groupId("commons-lang").artifactId("commons-lang").version("999").model();
        pom().artifactId("foo").dependency(lang).create(repoFooDir);

        validator.validate(ctx);

        assertExpectedException(ArtifactNotFoundException.class, "Could not find artifact commons-lang:commons-lang:jar:999");
    }

    @Test
    public void shouldFindMissingDependencyWithClassifier() {
        Model bar = pom().artifactId("bar").create(repoBarDir);
        pom().artifactId("foo").dependency(dependency().to(bar).classifier("jdk14").build()).create(repoFooDir);

        validator.validate(ctx);

        assertExpectedException(ArtifactNotFoundException.class, "Could not find artifact com.acme:bar:jar:jdk14:1.0");
    }

    @Test
    public void shouldFindMissingDependencyWithType() {
        Model bar = pom().artifactId("bar").create(repoBarDir);
        pom().artifactId("foo").dependency(dependency().to(bar).type("test-jar").build()).create(repoFooDir);

        validator.validate(ctx);

        assertExpectedException(ArtifactNotFoundException.class, "Could not find artifact com.acme:bar:jar:tests:1.0");
    }

    @Test
    public void shouldFindMissingDependencyFromDepMng() {
        Model bar = pom().artifactId("bar").model();

        Model fooParent = pom().artifactId("foo-parent").packaging("pom")
                .dependencyManagement(bar)
                .create(repoFooDir);

        pom().artifactId("foo-api")
                .parent(fooParent)
                .dependency(dependency().to(bar).version(null).build())
                .create(repoFooDir);

        fileFilter = notFileFilter(nameFileFilter("foo-parent-1.0.pom"));
        validator.validate(ctx);

        assertExpectedException(ArtifactNotFoundException.class, "Could not find artifact com.acme:bar:jar:1.0");
    }

    @Test
    public void shouldFindMissingBOM() {
        Model bar = pom().artifactId("bar").model();

        Model fooBom = pom().artifactId("foo-bom").packaging("pom")
                .dependencyManagement(bar)
                .model();

        pom().artifactId("foo-api")
                .dependencyManagement(dependency().to(fooBom).type("pom").scope("import").build())
                .dependency(dependency().to(bar).version(null).build())
                .create(repoFooDir);

        fileFilter = notFileFilter(nameFileFilter("foo-bom-1.0.pom"));
        validator.validate(ctx);

        assertExpectedException(ArtifactNotFoundException.class, "Could not find artifact com.acme:foo-bom:pom:1.0");
    }    

    @Test
    public void shouldFindMissingDependencyFromBOM() {
        Model bar = pom().artifactId("bar").model();

        Model fooBom = pom().artifactId("foo-bom").packaging("pom")
                .dependencyManagement(bar)
                .create(repoFooDir);

        pom().artifactId("foo-api")
                .dependencyManagement(dependency().to(fooBom).type("pom").scope("import").build())
                .dependency(dependency().to(bar).version(null).build())
                .create(repoFooDir);

        fileFilter = notFileFilter(nameFileFilter("foo-bom-1.0.pom"));
        validator.validate(ctx);

        assertExpectedException(ArtifactNotFoundException.class, "Could not find artifact com.acme:bar:jar:1.0");
    }
    
    @Test
    public void shouldFindMissingDependencyFromActiveProfile() {
        Model bar = pom().artifactId("bar").model();
        Model foo = pom().artifactId("foo").create(repoFooDir);

        Activation fooProfileActivation = new Activation();
        fooProfileActivation.setJdk("!1.0");

        Profile fooProfile = new Profile();
        fooProfile.setId("fooProfile");
        fooProfile.setActivation(fooProfileActivation);
        fooProfile.addDependency(dependency().to(bar).build());
        foo.addProfile(fooProfile);

        createArtifact(repoFooDir, foo);

        validator.validate(ctx);

        assertExpectedException(ArtifactNotFoundException.class, "Could not find artifact com.acme:bar:jar:1.0");
    }

    @Test
    public void shouldIgnoreDependencyWithTestScope() {
        Model bar = pom().artifactId("bar").model();
        pom().artifactId("foo").dependency(dependency().to(bar).scope("test").build()).create(repoFooDir);

        validator.validate(ctx);

        assertSuccess();
    }
    
    @Test
    public void shouldIgnoreDependencyWithScopeProvided() {
        Model bar = pom().artifactId("bar-provided").model();
        pom().artifactId("foo").dependency(dependency().to(bar).scope("provided").build()).create(repoFooDir);

        validator.validate(ctx);

        assertSuccess();
    }
    
    @Test
    public void shouldIgnoreDependencyWithScopeRuntime() {
        Model bar = pom().artifactId("bar-runtime").model();
        pom().artifactId("foo").dependency(dependency().to(bar).scope("runtime").build()).create(repoFooDir);

        validator.validate(ctx);

        assertSuccess();
    }

    @Test
    public void shouldIgnoreOptionalDependency() {
        Model baz = pom().artifactId("baz").model();
        Model bar = pom().artifactId("bar").dependency(dependency().to(baz).optional().build()).create(repoBarDir);
        pom().artifactId("foo").dependency(dependency().to(bar).build()).create(repoFooDir);

        validator.validate(ctx);

        assertSuccess();
    }

    @Test
    public void shouldIgnoreExcludedDependency() {
        Model baz = pom().artifactId("baz").model();
        Model bar = pom().artifactId("bar").dependency(baz).create(repoBarDir);
        pom().artifactId("foo").dependency(dependency().to(bar).exclude(baz).build()).create(repoFooDir);

        validator.validate(ctx);

        assertSuccess();
    }

    @Test
    public void shouldIgnoreMissingPlugin() {
        Plugin barPlugin = new Plugin();
        barPlugin.setArtifactId("bar-plugin");
        barPlugin.setGroupId("com.acme");
        barPlugin.setVersion("1.0");

        Build fooBuild = new Build();
        fooBuild.addPlugin(barPlugin);

        Model foo = pom().artifactId("foo").model();
        foo.setBuild(fooBuild);

        createArtifact(repoFooDir, foo);

        validator.validate(ctx);

        assertSuccess();
    }

    @Test
    public void shouldIgnoreMissingPluginDependency() {
        Dependency barApi = dependency().artifactId("bar-api").groupId("com.acme").version("1.0").build();

        Plugin barPlugin = new Plugin();
        barPlugin.setArtifactId("bar-plugin");
        barPlugin.setGroupId("com.acme");
        barPlugin.setVersion("1.0");
        barPlugin.addDependency(barApi);

        Build fooBuild = new Build();
        fooBuild.addPlugin(barPlugin);

        Model foo = pom().artifactId("foo").model();
        foo.setBuild(fooBuild);

        createArtifact(repoFooDir, foo);

        validator.validate(ctx);

        assertSuccess();
    }
    
    @Test
    public void shouldIgnoreDeeperDependency() {
        Model baz = pom().artifactId("baz").create(repoBarDir);
        
        Model bar = pom().artifactId("bar")
                .dependency(dependency().to(baz).version("999").build())
                .create(repoBarDir);
        
        pom().artifactId("foo")
                .dependency(bar)
                .dependency(baz)
                .create(repoFooDir);
        
        validator.validate(ctx);
        
        assertSuccess();
    }

}