package org.jboss.wolf.validator.impl;

import org.apache.maven.model.Model;

public interface BomFilter {

    boolean isBom(Model model);

}