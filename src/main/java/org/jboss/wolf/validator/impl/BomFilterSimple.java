package org.jboss.wolf.validator.impl;

import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;

public class BomFilterSimple implements BomFilter {

    @Override
    public boolean isBom(Model model) {
        if (model.getPackaging().equals("pom")) {
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