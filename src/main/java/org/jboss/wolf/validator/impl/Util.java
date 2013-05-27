package org.jboss.wolf.validator.impl;

import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.suffixFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.filefilter.IOFileFilter;

public class Util {

    public static Collection<File> listPomFiles(File dir, IOFileFilter filter) {
        Collection<File> pomFiles = listFiles(dir, and(filter, suffixFileFilter(".pom")), trueFileFilter());
        return pomFiles;
    }

}