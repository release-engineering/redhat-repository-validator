package com.redhat.repository.validator.impl.bom;

import java.util.regex.Pattern;

import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;

public class BomFilterSimple implements BomFilter {
    
    private final Pattern bomGavPattern;
    
    public BomFilterSimple() {
        bomGavPattern = null;

    }
    
    public BomFilterSimple(String bomGavRegex) {
        bomGavPattern = Pattern.compile(bomGavRegex);
    }

    @Override
    public boolean isBom(Model model) {
        if (bomGavPattern != null) {
            String gav = model.getId();
            if (bomGavPattern.matcher(gav).matches()) {
                return true;
            }
        }
        if (model.getPackaging() != null &&
            model.getGroupId() != null &&
            model.getArtifactId() != null &&
            model.getPackaging().equals("pom")) {
            if (model.getGroupId().contains("bom") || model.getArtifactId().contains("bom")) {
                DependencyManagement depMng = model.getDependencyManagement();
                if (depMng != null && depMng.getDependencies() != null && !depMng.getDependencies().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

}