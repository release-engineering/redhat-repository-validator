package org.jboss.wolf.validator.impl.distribution;


import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Collection;

public class DistributionEqualFilesException extends Exception {

    private static final long serialVersionUID = 1L;

    private final Collection<File> filePaths;

    public DistributionEqualFilesException(String checkFolder, Collection<File> filePaths) {
        super(String.format("Files from %s %s are equal", checkFolder, StringUtils.join(filePaths)));
        this.filePaths = filePaths;
    }

    public Collection<File> getFilePaths() {
        return filePaths;
    }
}
