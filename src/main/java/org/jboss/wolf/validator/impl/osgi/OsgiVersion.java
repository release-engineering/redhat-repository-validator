package org.jboss.wolf.validator.impl.osgi;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/*
 * copy/pasted from org.osgi.framework.Version class
 */
/**
 * Version identifier for capabilities such as bundles and packages.
 * 
 * <p>
 * Version identifiers have four components.
 * <ol>
 * <li>Major version. A non-negative integer.</li>
 * <li>Minor version. A non-negative integer.</li>
 * <li>Micro version. A non-negative integer.</li>
 * <li>Qualifier. A text string. See {@code Version(String)} for the format of the qualifier string.</li>
 * </ol>
 * 
 * <p>
 * Version string grammar:
 * 
 * <pre>
 * version ::= major('.'minor('.'micro('.'qualifier)?)?)?
 * major ::= digit+
 * minor ::= digit+
 * micro ::= digit+
 * qualifier ::= (alpha|digit|'_'|'-')+
 * digit ::= [0..9]
 * alpha ::= [a..zA..Z]
 * </pre>
 * 
 */
public class OsgiVersion {

    private static final String SEPARATOR = ".";

    public OsgiVersion(String version) {
        Integer major = null;
        Integer minor = null;
        Integer micro = null;
        String qualifier = "";

        try {
            StringTokenizer st = new StringTokenizer(version, SEPARATOR, true);
            major = parseInt(st.nextToken(), version);

            if (st.hasMoreTokens()) { // minor
                st.nextToken(); // consume delimiter
                minor = parseInt(st.nextToken(), version);

                if (st.hasMoreTokens()) { // micro
                    st.nextToken(); // consume delimiter
                    micro = parseInt(st.nextToken(), version);

                    if (st.hasMoreTokens()) { // qualifier separator
                        st.nextToken(); // consume delimiter
                        qualifier = st.nextToken(""); // remaining string

                        if (st.hasMoreTokens()) { // fail safe
                            throw new IllegalArgumentException("invalid version \"" + version + "\": invalid format");
                        }
                    }
                }
            }
        } catch (NoSuchElementException e) {
            IllegalArgumentException iae = new IllegalArgumentException("invalid version \"" + version + "\": invalid format");
            iae.initCause(e);
            throw iae;
        }
        
        if (major == null) {
            throw new IllegalArgumentException("invalid version \"" + version + "\": missing major part");
        }
        if (minor == null) {
            throw new IllegalArgumentException("invalid version \"" + version + "\": missing minor part");
        }
        if (micro == null) {
            throw new IllegalArgumentException("invalid version \"" + version + "\": missing micro part");
        }

        if (major < 0) {
            throw new IllegalArgumentException("invalid version \"" + version + "\": negative number \"" + major + "\"");
        }
        if (minor < 0) {
            throw new IllegalArgumentException("invalid version \"" + version + "\": negative number \"" + minor + "\"");
        }
        if (micro < 0) {
            throw new IllegalArgumentException("invalid version \"" + version + "\": negative number \"" + micro + "\"");
        }
        
        for (char ch : qualifier.toCharArray()) {
            if (('A' <= ch) && (ch <= 'Z')) {
                continue;
            }
            if (('a' <= ch) && (ch <= 'z')) {
                continue;
            }
            if (('0' <= ch) && (ch <= '9')) {
                continue;
            }
            if ((ch == '_') || (ch == '-')) {
                continue;
            }
            throw new IllegalArgumentException("invalid version \"" + version + "\": invalid qualifier \"" + qualifier + "\"");
        }
    }

    private int parseInt(String value, String version) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            IllegalArgumentException iae = new IllegalArgumentException("invalid version \"" + version + "\": non-numeric \"" + value + "\"");
            iae.initCause(e);
            throw iae;
        }
    }

}