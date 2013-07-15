package org.jboss.wolf.validator.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class BomAmbiguousVersionValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(BomAmbiguousVersionValidator.class);

    @Inject @Named("bomAmbiguousVersionValidatorFilter")
    private IOFileFilter fileFilter;
    @Inject
    private ValidatorSupport validatorSupport;

    @Override
    public void validate(ValidatorContext ctx) {
        logger.debug("start...");
        validateAmbiguousVersions(ctx);
    }
    
    private void validateAmbiguousVersions(ValidatorContext ctx) {
        Map<DepKey, Map<DepVersion, List<Pair<Dependency, Model>>>> dependencies = collectDependencies(ctx);
        for (DepKey depKey : dependencies.keySet()) {
            Map<DepVersion, List<Pair<Dependency, Model>>> versions = dependencies.get(depKey);
            if (versions.size() > 1) {
                List<Pair<Dependency, Model>> ambiguousDependencies = new ArrayList<Pair<Dependency, Model>>();
                for (Entry<DepVersion, List<Pair<Dependency, Model>>> versionEntry : versions.entrySet()) {
                    ambiguousDependencies.addAll(versionEntry.getValue());
                }
                Exception ambiguousVersionException = new BomAmbiguousVersionException(ambiguousDependencies.get(0).getKey().getManagementKey(), ambiguousDependencies);
                for (Pair<Dependency, Model> ambiguousDependency : ambiguousDependencies) {
                    ctx.addException(ambiguousDependency.getValue().getPomFile(), ambiguousVersionException);
                }
            }
        }
    }

    private Map<DepKey, Map<DepVersion, List<Pair<Dependency, Model>>>> collectDependencies(ValidatorContext ctx) {
        Map<DepKey, Map<DepVersion, List<Pair<Dependency, Model>>>> dependencies = new HashMap<DepKey, Map<DepVersion, List<Pair<Dependency, Model>>>>();
        
        List<Model> boms = validatorSupport.findBoms(ctx, fileFilter);
        for (Model bom : boms) {
            for (Dependency dependency : bom.getDependencyManagement().getDependencies()) {
                DepKey depKey = new DepKey(dependency);
                DepVersion depVersion = new DepVersion(dependency);
    
                Map<DepVersion, List<Pair<Dependency, Model>>> versions = dependencies.get(depKey);
                if (versions == null) {
                    versions = new HashMap<DepVersion, List<Pair<Dependency, Model>>>();
                    dependencies.put(depKey, versions);
                }
    
                List<Pair<Dependency, Model>> pairs = versions.get(depVersion);
                if (pairs == null) {
                    pairs = new ArrayList<Pair<Dependency, Model>>();
                    versions.put(depVersion, pairs);
                }
    
                pairs.add(new ImmutablePair<Dependency, Model>(dependency, bom));
            }
        }
    
        return dependencies;
    }

    private static class DepKey {

        private final String key;

        private DepKey(Dependency dependency) {
            this.key = dependency.getManagementKey();
        }

        @Override
        public boolean equals(Object obj) {
            return key.equals(((DepKey) obj).key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

    }

    private static class DepVersion {

        private final String version;

        private DepVersion(Dependency dependency) {
            this.version = dependency.getVersion();
        }

        @Override
        public boolean equals(Object obj) {
            return version.equals(((DepVersion) obj).version);
        }

        @Override
        public int hashCode() {
            return version.hashCode();
        }

    }

}