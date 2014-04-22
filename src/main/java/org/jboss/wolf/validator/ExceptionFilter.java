package org.jboss.wolf.validator;

import java.io.File;

/**
 * Purpose of the exception filter is the ability to define exceptions (failures) that should be ignored by the
 * validator. Such failures may come from the fact that the repository contains some known flaw(s) which won't be
 * fixed (for whatever reason) and thus will only clutter the report and potentially hide newly found issues.
 */
public interface ExceptionFilter {

    public boolean shouldIgnore(Exception exception, File fileInRepo);
}
