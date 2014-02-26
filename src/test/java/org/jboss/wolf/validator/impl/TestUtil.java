package org.jboss.wolf.validator.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.eclipse.aether.util.ChecksumUtils;

public class TestUtil {
    
    public static boolean containsExceptionMessage(Exception exception, Class<?> exceptionType, String message) {
        if (exceptionType.isInstance(exception)) {
            if (StringUtils.containsIgnoreCase(exception.getMessage(), message)) {
                return true;
            }
        } else {
            for (Throwable cause : ExceptionUtils.getThrowables(exception)) {
                if (exceptionType.isInstance(cause)) {
                    if (StringUtils.containsIgnoreCase(cause.getMessage(), message)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static PomBuilder pom() {
        return new PomBuilder();
    }
    
    public static ParentBuilder parent() {
        return parent(null);
    }
    
    public static ParentBuilder parent(Model parent) {
        return new ParentBuilder(parent);
    }

    public static DependencyBuilder dependency() {
        return new DependencyBuilder();
    }

    public static class PomBuilder {

        private Model model = new Model();
        private String originalText;
        private String demagedText;
        
        public PomBuilder() {
            model.setModelVersion("4.0.0");
            model.setGroupId("com.acme");
            model.setArtifactId("foo");
            model.setVersion("1.0");
        }

        public PomBuilder groupId(String groupId) {
            model.setGroupId(groupId);
            return this;
        }

        public PomBuilder artifactId(String artifactId) {
            model.setArtifactId(artifactId);
            return this;
        }
        
        public PomBuilder classifier(String classifier) {
            return this;
        }

        public PomBuilder version(String version) {
            model.setVersion(version);
            return this;
        }
        
        public PomBuilder packaging(String packaging) {
            model.setPackaging(packaging);
            return this;
        }
        
        public PomBuilder parent(Model m) {
            model.setParent(TestUtil.parent(m).build());
            return this;
        }
        
        public PomBuilder property(String key, String value) {
            model.addProperty(key, value);
            return this;
        }
        
        public PomBuilder dependency(Model m) {
            return dependency(TestUtil.dependency().to(m).build());
        }
        
        public PomBuilder dependency(Dependency d) {
            model.addDependency(d);
            return this;
        }
        
        public PomBuilder dependencyManagement(Model m) {
            return dependencyManagement(TestUtil.dependency().to(m).build());
        }
        
        public PomBuilder dependencyManagement(Dependency d) {
            DependencyManagement dm = model.getDependencyManagement();
            if (dm == null) {
                dm = new DependencyManagement();
                model.setDependencyManagement(dm);
            }
            dm.addDependency(d);
            return this;
        }
        
        public PomBuilder withDamage(String originalText, String demagedText) {
            this.originalText = originalText;
            this.demagedText = demagedText;
            return this;
        }
        
        public Model model() {
            return model;
        }
        
        public Model create(File repoDir) {
            createArtifact(repoDir, model, originalText, demagedText);
            return model;
        }
        
        public Model create(File repoDir, String jarPath) {
            createArtifact(repoDir, model, originalText, demagedText, jarPath);
            return model;
        }

    }
    
    public static class ParentBuilder {

        private Parent parent = new Parent();
        
        public ParentBuilder(Model model) {
            parent.setGroupId(model.getGroupId());
            parent.setArtifactId(model.getArtifactId());
            parent.setVersion(model.getVersion());
        }

        public ParentBuilder groupId(String groupId) {
            parent.setGroupId(groupId);
            return this;
        }

        public ParentBuilder artifactId(String artifactId) {
            parent.setArtifactId(artifactId);
            return this;
        }

        public ParentBuilder version(String version) {
            parent.setVersion(version);
            return this;
        }

        public Parent build() {
            return parent;
        }

    }
    
    public static class DependencyBuilder {

        private Dependency dependency = new Dependency();
        
        public DependencyBuilder() {
            dependency.setGroupId("com.acme");
            dependency.setArtifactId("foo");
            dependency.setVersion("1.0");
        }
        
        public DependencyBuilder to(Model model) {
            dependency.setGroupId(model.getGroupId());
            dependency.setArtifactId(model.getArtifactId());
            dependency.setVersion(model.getVersion());
            return this;
        }

        public DependencyBuilder groupId(String groupId) {
            dependency.setGroupId(groupId);
            return this;
        }

        public DependencyBuilder artifactId(String artifactId) {
            dependency.setArtifactId(artifactId);
            return this;
        }

        public DependencyBuilder version(String version) {
            dependency.setVersion(version);
            return this;
        }
        
        public DependencyBuilder optional() {
            dependency.setOptional(true);
            return this;
        }
        
        public DependencyBuilder scope(String scope) {
            dependency.setScope(scope);
            return this;
        }
        
        public DependencyBuilder type(String type) {
            dependency.setType(type);
            return this;
        }
        
        public DependencyBuilder classifier(String classifier) {
            dependency.setClassifier(classifier);
            return this;
        }
        
        public DependencyBuilder exclude(Model m) {
            Exclusion exclusion = new Exclusion();
            exclusion.setArtifactId(m.getArtifactId());
            exclusion.setGroupId(m.getGroupId());
            dependency.addExclusion(exclusion);
            return this;
        }

        public Dependency build() {
            return dependency;
        }

    }
    
    public static void createArtifact(File repoDir, Model pomModel) {
        createArtifact(repoDir, pomModel, null, null);
    }
    
    public static void createArtifact(File repoDir, Model pomModel, String originalText, String demagedText) {
        createArtifact(repoDir, pomModel, originalText, demagedText, "target/test-classes/empty.jar");
    }
    
    public static void createArtifact(File repoDir, Model pomModel, String originalText, String demagedText, String jarPath) {
        try {
            File pomFile = toPomFile(repoDir, pomModel);
            FileUtils.touch(pomFile);

            FileOutputStream s = new FileOutputStream(pomFile);
            new MavenXpp3Writer().write(s, pomModel);

            s.flush();
            s.close();
            
            if (!StringUtils.equals(pomModel.getPackaging(), "pom")) {
                File srcFile = new File(jarPath);
                File destFile = toArtifactFile(repoDir, pomModel);
                FileUtils.copyFile(srcFile, destFile);
                createChecksums(destFile);
            }
            
            if (originalText != null && demagedText != null) {
                String pomText = FileUtils.readFileToString(pomFile);
                pomText = pomText.replace(originalText, demagedText);
                FileUtils.writeStringToFile(pomFile, pomText);
            }
            
            createChecksums(pomFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void createChecksums(File file) {
        try {
            Map<String, String> checksumAlgos = new HashMap<String, String>();
            checksumAlgos.put("SHA-1", ".sha1");
            checksumAlgos.put("MD5", ".md5");

            Map<String, Object> checksumResults = ChecksumUtils.calc(file, checksumAlgos.keySet());
            for (Entry<String, Object> checksumEntry : checksumResults.entrySet()) {
                String checksumType = checksumEntry.getKey();
                Object checksumValue = checksumEntry.getValue();

                File checksumFile = new File(file.getPath() + checksumAlgos.get(checksumType));
                OutputStreamWriter checksumWriter = new OutputStreamWriter(new FileOutputStream(checksumFile), "US-ASCII");
                checksumWriter.write(checksumValue.toString());
                checksumWriter.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static File toArtifactDir(File repoDir, Model pomModel) {
        File pomDir = new File(repoDir, pomModel.getGroupId().replace('.', '/') + "/" + pomModel.getArtifactId() + "/" + pomModel.getVersion());
        return pomDir;
    }
    
    public static File toArtifactFile(File repoDir, Model pomModel) {
        File pomDir = toArtifactDir(repoDir, pomModel);
        File pomFile = new File(pomDir, pomModel.getArtifactId() + "-" + pomModel.getVersion() + "." + pomModel.getPackaging());
        return pomFile;
    }
    
    public static File toPomFile(File repoDir, Model pomModel) {
        File pomDir = toArtifactDir(repoDir, pomModel);
        File pomFile = new File(pomDir, pomModel.getArtifactId() + "-" + pomModel.getVersion() + ".pom");
        return pomFile;
    }

}