package org.openjump.core.ccordsys.utils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class to lookup in srid.txt.
 * Can find the srid, the name and/or the unit from the code or the name
 * of a Coordinate Reference System
 */
public class SridLookupTable {

    private static final Pattern pattern = Pattern
            .compile("<([^<>]*)>\\s*;\\s*<([^<>]*)>\\s*;\\s*\\[([^\\[\\]]*)\\]");

    /** Test the class*/
    public static void main(String[] args) throws UnsupportedEncodingException {
        System.out.println("4326       -> " + Arrays.toString(getSrsAndUnitFromCode("4326")));
        System.out.println("WGS 84     -> " + Arrays.toString(getSrsAndUnitFromName("WGS 84")));
        System.out.println("LAMB93     -> " + Arrays.toString(getSrsAndUnitFromCode("LAMB93")));
        System.out.println("Lambert 93 -> " + Arrays.toString(getSrsAndUnitFromName("Lambert 93")));
        System.out.println("Geoportail - Reunion -> " + Arrays.toString(getSrsAndUnitFromName("Geoportail - Reunion")));
        System.out.println("4326       -> " + getSrsNameFromCode("4326"));
        System.out.println("WGS 84     -> " + getSrsCodeFromName("WGS 84"));
        System.out.println("4326       -> " + getUnitFromCode("4326"));
        System.out.println("WGS 84     -> " + getUnitFromName("WGS 84"));
    }

    private static Scanner getScanner() throws UnsupportedEncodingException {
        InputStream is = ProjUtils.class.getResourceAsStream("srid.txt");
        InputStreamReader isr = new InputStreamReader(is, "UTF-8");
        Scanner scanner = new Scanner(isr);
        scanner.useDelimiter("\\n");
        return scanner;
    }

    public static String[] getSrsAndUnitFromCode(String code) throws UnsupportedEncodingException {
        Scanner scanner = getScanner();
        String[] srsAndUnit = null;
        try {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                Matcher m = pattern.matcher(line);
                if (m.matches()) {
                    if (m.group(1).equals(code)) {
                        srsAndUnit = new String[]{m.group(1), m.group(2), m.group(3)};
                    }
                }
            }
        } finally {
            scanner.close();
            return srsAndUnit;
        }
    }

    public static String[] getSrsAndUnitFromName(String name) throws UnsupportedEncodingException {
        Scanner scanner = getScanner();
        String[] srsAndUnit = null;
        try {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                Matcher m = pattern.matcher(line);
                if (m.matches()) {
                    if (normalize(m.group(2)).equals(normalize(name))) {
                        srsAndUnit = new String[]{m.group(1), m.group(2), m.group(3)};
                    }
                }
            }
        } finally {
            scanner.close();
            return srsAndUnit;
        }
    }

    public static String[] getSrsAndUnitFromCodeOrName(String codeOrName) throws UnsupportedEncodingException {
        Scanner scanner = getScanner();
        String[] srsAndUnit = null;
        try {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                Matcher m = pattern.matcher(line);
                if (m.matches()) {
                    if (m.group(1).equals(codeOrName) ||
                            normalize(m.group(2)).equals(normalize(codeOrName))) {
                        srsAndUnit = new String[]{m.group(1), m.group(2), m.group(3)};
                    }
                }
            }
        } finally {
            scanner.close();
            return srsAndUnit;
        }
    }


    public static String getSrsCodeFromName(String name) throws UnsupportedEncodingException {
        String[] srsAndUnit = getSrsAndUnitFromName(name);
        return srsAndUnit == null ? null : srsAndUnit[0];
    }

    public static String getSrsNameFromCode(String code) throws UnsupportedEncodingException {
        String[] srsAndUnit = getSrsAndUnitFromCode(code);
        return srsAndUnit == null ? null : srsAndUnit[1];
    }

    public static String getUnitFromName(String name) throws UnsupportedEncodingException {
        String[] srsAndUnit = getSrsAndUnitFromName(name);
        return srsAndUnit == null ? null : srsAndUnit[2];
    }

    public static String getUnitFromCode(String code) throws UnsupportedEncodingException {
        String[] srsAndUnit = getSrsAndUnitFromCode(code);
        return srsAndUnit == null ? null : srsAndUnit[2];
    }

    /**
     * Use this function to normalize srs name and get a chance to match it
     * with the one in srid.txt
     */
    private static String normalize(String name) {
        name = Normalizer.normalize(name, Normalizer.Form.NFD); // separe base character from accent
        name = name.replaceAll("\\p{M}", ""); // remove accents
        return name.toLowerCase();
    }
}
