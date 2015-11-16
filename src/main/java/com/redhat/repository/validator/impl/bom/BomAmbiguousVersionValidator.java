package com.redhat.repository.validator.impl.bom;

import static com.redhat.repository.validator.internal.Utils.relativize;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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

import com.redhat.repository.validator.Validator;
import com.redhat.repository.validator.ValidatorContext;
import com.redhat.repository.validator.internal.ValidatorSupport;

@Named
public class BomAmbiguousVersionValidator implements Validator {

    @Inject @Named("bomAmbiguousVersionValidatorFilter")
    private IOFileFilter fileFilter;
    @Inject
    private ValidatorSupport validatorSupport;
    @Inject
    private BomFilter bomFilter;

    @Override
    public void validate(ValidatorContext ctx) {
        validateAmbiguousVersions(ctx);
    }
    
    private void validateAmbiguousVersions(ValidatorContext ctx) {
        Map<DepKey, Map<DepVersion, List<Pair<Dependency, File>>>> dependencies = collectDependencies(ctx);
        for (DepKey depKey : dependencies.keySet()) {
            Map<DepVersion, List<Pair<Dependency, File>>> versions = dependencies.get(depKey);
            if (versions.size() > 1) {
                List<Pair<Dependency, File>> ambiguousDependencies = new ArrayList<Pair<Dependency, File>>();
                for (Entry<DepVersion, List<Pair<Dependency, File>>> versionEntry : versions.entrySet()) {
                    ambiguousDependencies.addAll(versionEntry.getValue());
                }
                Exception ambiguousVersionException = new BomAmbiguousVersionException(ambiguousDependencies.get(0).getKey().getManagementKey(), ambiguousDependencies);
                for (Pair<Dependency, File> ambiguousDependency : ambiguousDependencies) {
                    ctx.addError(this, ambiguousDependency.getValue(), ambiguousVersionException);
                }
            }
        }
    }

    private Map<DepKey, Map<DepVersion, List<Pair<Dependency, File>>>> collectDependencies(ValidatorContext ctx) {
        Map<DepKey, Map<DepVersion, List<Pair<Dependency, File>>>> dependencies = new HashMap<DepKey, Map<DepVersion, List<Pair<Dependency, File>>>>();
        
        Iterator<Model> modelIterator = validatorSupport.effectiveModelIterator(ctx, fileFilter);
        while (modelIterator.hasNext()) {
            Model model = modelIterator.next();
            if (model != null) {
                if( bomFilter.isBom(model) ) {
                    for (Dependency dependency : model.getDependencyManagement().getDependencies()) {
                        DepKey depKey = new DepKey(dependency);
                        DepVersion depVersion = new DepVersion(dependency);

                        Map<DepVersion, List<Pair<Dependency, File>>> versions = dependencies.get(depKey);
                        if (versions == null) {
                            versions = new HashMap<DepVersion, List<Pair<Dependency, File>>>();
                            dependencies.put(depKey, versions);
                        }

                        List<Pair<Dependency, File>> pairs = versions.get(depVersion);
                        if (pairs == null) {
                            pairs = new ArrayList<Pair<Dependency, File>>();
                            versions.put(depVersion, pairs);
                        }

                        pairs.add(new ImmutablePair<Dependency, File>(dependency, relativize(ctx, model.getPomFile())));
                    }
                }
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