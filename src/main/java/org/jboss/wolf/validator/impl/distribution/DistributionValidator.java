package org.jboss.wolf.validator.impl.distribution;

import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.suffixFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;
import static org.jboss.wolf.validator.internal.Utils.relativize;
import static org.jboss.wolf.validator.internal.Utils.relativizeFile;
import static org.jboss.wolf.validator.internal.Utils.sortFiles;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.eclipse.aether.util.ChecksumUtils;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;

@Named
public class DistributionValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(DistributionValidator.class);
    
    private static final String HASH_ALGORITHM = "SHA-1";

    @Inject @Named("distributionValidatorFilter")
    private IOFileFilter fileFilter;

    @Override
    public void validate(ValidatorContext ctx) {
        if (!ctx.getValidatedDistribution().isDirectory()) {
            logger.trace("validation skipped, because distribution directory {} doesn't exists", ctx.getValidatedDistribution());
            return;
        }

        ListMultimap<String, File> repoFilesMap = mapFilesToChecksum(ctx.getValidatedRepository());
        ListMultimap<String, File> distFilesMap = mapFilesToChecksum(ctx.getValidatedDistribution());

        // find files which are in repository, but not in distribution
        findMissingFiles(ctx, repoFilesMap, distFilesMap);
        
        // find files which are in distribution, but not in repository
        findRedundantFiles(ctx, repoFilesMap, distFilesMap);
        
        // find files which are identical, has same content
        findDuplicateFiles(ctx, repoFilesMap, distFilesMap);
        
        // find files which are identical with artifact, but have different names 
        findMisnomerFiles(ctx, repoFilesMap, distFilesMap);
        
        // find files which have same name like some artifact, but different content
        findCoruptedFiles(ctx, repoFilesMap, distFilesMap);
    }

    private void findMissingFiles(ValidatorContext ctx, ListMultimap<String, File> repoFilesMap, ListMultimap<String, File> distFilesMap) {
        Set<String> missingFileHashSet = Sets.difference(repoFilesMap.keySet(), distFilesMap.keySet());
        for (String missingFileHash : missingFileHashSet) {
            File missingFile = repoFilesMap.get(missingFileHash).get(0);
            ctx.addException(missingFile, 
                    new DistributionMissingFileException(relativize(ctx, missingFile)));
        }
    }

    private void findRedundantFiles(ValidatorContext ctx, ListMultimap<String, File> repoFilesMap, ListMultimap<String, File> distFilesMap) {
        Set<String> redundantFileHashSet = Sets.difference(distFilesMap.keySet(), repoFilesMap.keySet());
        for (String redundantFileHash : redundantFileHashSet) {
            File redundantFile = distFilesMap.get(redundantFileHash).get(0);
            ctx.addException(redundantFile, 
                    new DistributionRedundantFileException(relativizeFile(ctx.getValidatedDistribution(), redundantFile)));
        }
    }

    private void findDuplicateFiles(ValidatorContext ctx, ListMultimap<String, File> repoFilesMap, ListMultimap<String, File> distFilesMap) {
        for (Entry<String, Collection<File>> distFileEntry : distFilesMap.asMap().entrySet()) {
            if (distFileEntry.getValue().size() > 1) {
                List<File> duplicateFiles = new ArrayList<File>();
                for (File distFile : distFileEntry.getValue()) {
                    duplicateFiles.add(relativizeFile(ctx.getValidatedDistribution(), distFile));
                }
                duplicateFiles = sortFiles(duplicateFiles);
                ctx.addException(duplicateFiles.get(0), 
                        new DistributionDuplicateFilesException(duplicateFiles.toArray(new File[] {})));
            }
        }
    }

    private void findMisnomerFiles(ValidatorContext ctx, ListMultimap<String, File> repoFilesMap, ListMultimap<String, File> distFilesMap) {
        for (Entry<String, File> distFileEntry : distFilesMap.entries()) {
            for (Entry<String, File> repoFileEntry : repoFilesMap.entries()) {
                String distFileHash = distFileEntry.getKey();
                String repoFileHash = repoFileEntry.getKey();
                File distFile = distFileEntry.getValue();
                File repoFile = repoFileEntry.getValue();

                if (distFileHash.equals(repoFileHash) && !distFile.getName().equals(repoFile.getName())) {
                    ctx.addException(repoFile,
                            new DistributionMisnomerFileException(
                                    relativizeFile(ctx.getValidatedRepository(), repoFile),
                                    relativizeFile(ctx.getValidatedDistribution(), distFile)));
                }
            }
        }
    }

    private void findCoruptedFiles(ValidatorContext ctx, ListMultimap<String, File> repoFilesMap, ListMultimap<String, File> distFilesMap) {
        for (Entry<String, File> distFileEntry : distFilesMap.entries()) {
            for (Entry<String, File> repoFileEntry : repoFilesMap.entries()) {
                String distFileHash = distFileEntry.getKey();
                String repoFileHash = repoFileEntry.getKey();
                File distFile = distFileEntry.getValue();
                File repoFile = repoFileEntry.getValue();

                if (distFile.getName().equals(repoFile.getName()) && !distFileHash.equals(repoFileHash) ) {
                    ctx.addException(repoFile,
                            new DistributionCoruptedFileException(
                                    relativizeFile(ctx.getValidatedRepository(), repoFile),
                                    relativizeFile(ctx.getValidatedDistribution(), distFile)));
                }
            }
        }
    }

    private ListMultimap<String, File> mapFilesToChecksum(File dir) {
        Collection<File> files = listFiles(dir, and(fileFilter, suffixFileFilter(".jar")), trueFileFilter());
        ListMultimap<String, File> filesHash = ArrayListMultimap.create();
        for (File file : files) {
            try {
                String checksum = ChecksumUtils.calc(file, Collections.singleton(HASH_ALGORITHM)).get(HASH_ALGORITHM).toString();
                filesHash.put(checksum, file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return filesHash;
    }

}