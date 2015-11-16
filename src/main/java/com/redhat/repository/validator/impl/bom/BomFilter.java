package com.redhat.repository.validator.impl.bom;

import org.apache.maven.model.Model;

public interface BomFilter {

    boolean isBom(Model model);

}