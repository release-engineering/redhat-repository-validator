package com.redhat.repository.validator.impl.bom;

import org.apache.commons.lang3.StringUtils;

public class BomVersionPropertyException extends Exception {

    private static final long serialVersionUID = 1L;

    private static String formatMessage(String bomGav, String[] bomDependenciesWithoutVersionProperty) {
        return "BOM " + bomGav + " contains dependencies without version property: " + StringUtils.join(bomDependenciesWithoutVersionProperty, ", ");
    }

    private final String bomGav;
    private final String[] bomDependenciesWithoutVersionProperty;

    public BomVersionPropertyException(String bomGav, String[] bomDependenciesWithoutVersionProperty) {
        super(formatMessage(bomGav, bomDependenciesWithoutVersionProperty));
        this.bomGav = bomGav;
        this.bomDependenciesWithoutVersionProperty = bomDependenciesWithoutVersionProperty;
    }

    public String getBomGav() {
        return bomGav;
    }

    public String[] getBomDependenciesWithoutVersionProperty() {
        return bomDependenciesWithoutVersionProperty;
    }

}