package org.jboss.wolf.validator.impl.distribution;

import com.google.common.collect.*;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.eclipse.aether.util.ChecksumUtils;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorContext;
import org.jboss.wolf.validator.internal.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.util.Map.Entry;
import static org.apache.commons.io.FileUtils.contentEquals;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.filefilter.FileFilterUtils.*;
import static org.jboss.wolf.validator.internal.Utils.relativize;
import static org.jboss.wolf.validator.internal.Utils.relativizeFile;

@Named
public class DistributionValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(DistributionValidator.class);
    public static final String HASH_ALGORITHM = "SHA-1";

    @Value("#{systemProperties['wolf-distribution']?:'workspace/validated-distribution'}")
    private String path2distribution;
    @Inject
    @Named("distributionValidatorFilter")
    private IOFileFilter fileFilter;

    @Override
    public void validate(ValidatorContext ctx) {

        File dirDist = new File(path2distribution);
        if (dirDist.exists() && dirDist.isDirectory()) {

            Collection<File> filesRepo = listFiles(ctx.getValidatedRepository(), and(fileFilter, suffixFileFilter(".jar")), trueFileFilter());
            ListMultimap<String, File> filesHashRepo = getSHAsForCollectionOfFiles(ctx, filesRepo, true);

            Collection<File> filesDist = listFiles(dirDist, and(fileFilter, suffixFileFilter(".jar")), trueFileFilter());
            ListMultimap<String, File> filesHashDist = getSHAsForCollectionOfFiles(ctx, filesDist, false);

            validateFiles(ctx, filesHashRepo, filesHashDist);
        } else {
            logger.trace("Skipping running DistributionValidator");
        }
    }

    private ListMultimap<String, File> getSHAsForCollectionOfFiles(ValidatorContext ctx, Collection<File> files, boolean isRepository) {
        ListMultimap<String, File> filesHash = ArrayListMultimap.create();
        for (File file : files) {
            try {
                String checksum = ChecksumUtils.calc(file, Arrays.asList(HASH_ALGORITHM)).get(HASH_ALGORITHM).toString();
                filesHash.put(checksum, file);
            } catch (IOException e) {
                if (isRepository)
                    ctx.addException(file, new DistributionFileException(relativize(ctx, file), "Can not calculate checksum"));
                else {
                    ctx.addException(file, new DistributionFileException(relativizeFile(new File(path2distribution), file), "Can not calculate checksum"));
                }
            }
        }
        return filesHash;
    }

    private void validateFiles(ValidatorContext ctx, ListMultimap<String, File> filesHashRepo, ListMultimap<String, File> filesHashDist) {
        validateDistAndRepoInterset(ctx, filesHashRepo, filesHashDist);

        for (Entry<String, Collection<File>> entryRepo : filesHashRepo.asMap().entrySet()) {
            validateFilesWithSameHash(ctx, entryRepo, true);
            for (Entry<String, Collection<File>> entryDist : filesHashDist.asMap().entrySet()) {
                validateIfNamesAreEqual(ctx, entryRepo, entryDist);
                validateFilesWithSameHash(ctx, entryDist, false);
            }
        }
    }

    private void validateIfNamesAreEqual(ValidatorContext ctx, Entry<String, Collection<File>> entryRepo, Entry<String, Collection<File>> entryDist) {
        String hashRepo = entryRepo.getKey();
        String hashDist = entryDist.getKey();
        File fileFromRepo = Iterables.getFirst(entryRepo.getValue(), null);
        File fileFromDist = Iterables.getFirst(entryDist.getValue(), null);

        if (hashRepo.equals(hashDist)) {
            try {
                logger.trace("validating {} <---> {}", relativize(ctx, fileFromRepo), relativizeFile(new File(path2distribution), fileFromDist));

                if (!contentEquals(Iterables.getFirst(entryRepo.getValue(), null), Iterables.getFirst(entryDist.getValue(), null)))
                    ctx.addException(fileFromRepo, new DistributionNotEqualFilesException(relativize(ctx, fileFromRepo), relativizeFile(new File(path2distribution), fileFromDist)));
                if (!fileFromRepo.getName().equals(fileFromDist.getName()))
                    ctx.addException(fileFromRepo, new DistributionNotEqualNamesException(relativize(ctx, fileFromRepo), relativizeFile(new File(path2distribution), fileFromDist)));
            } catch (IOException e) {
                // Ignore Exception, only files are passed to contentEquals(_,_)
            }
        } else {
            if (fileFromRepo.getName().equals(fileFromDist.getName()))
                ctx.addException(fileFromRepo, new DistributionNotEqualSizeException(relativize(ctx, fileFromRepo), relativizeFile(new File(path2distribution), fileFromDist)));
        }
    }

    private void validateDistAndRepoInterset(ValidatorContext ctx, ListMultimap<String, File> filesHashRepo, ListMultimap<String, File> filesHashDist) {
        Set<String> repoDiff = Sets.difference(filesHashRepo.keySet(), filesHashDist.keySet());
        Set<String> distDiff = Sets.difference(filesHashDist.keySet(), filesHashRepo.keySet());
        if (!repoDiff.isEmpty()) {
            for (String key : repoDiff) {
                File file = filesHashRepo.get(key).get(0);
                ctx.addException(file, new DistributionFileException(relativize(ctx, file), "not present in DISTRIBUTION"));
            }
        }

        if (!distDiff.isEmpty()) {
            for (String key : distDiff) {
                File file = filesHashDist.get(key).get(0);
                ctx.addException(file, new DistributionFileException(relativizeFile(new File(path2distribution), file), "not pressent in REPOSITORY"));
            }
        }
    }

    private void validateFilesWithSameHash(ValidatorContext ctx, Entry<String, Collection<File>> entry, boolean isRepository) {
        if (entry.getValue().size() > 1) {
            Collection<File> filePaths = new ArrayList<File>();
            if (isRepository) {
                for (File file : entry.getValue()) {
                    filePaths.add(relativize(ctx, file));
                }
                Collection<File> sorted = Utils.sortFilesViaPath(filePaths);
                ctx.addException(Iterables.getFirst(entry.getValue(), null), new DistributionEqualFilesException("REPOSITORY", sorted));
            } else {
                for (File file : entry.getValue()) {
                    filePaths.add(relativizeFile(new File(path2distribution), file));
                }
                Collection<File> sorted = Utils.sortFilesViaPath(filePaths);
                ctx.addException(Iterables.getFirst(entry.getValue(), null), new DistributionEqualFilesException("DISTRIBUTION", sorted));
            }
        }
    }
}