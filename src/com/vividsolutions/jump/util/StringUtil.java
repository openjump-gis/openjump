/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */
package com.vividsolutions.jump.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import com.vividsolutions.jts.util.Assert;

/**
 * Useful String-related utilities.
 */
public class StringUtil {

    /**
     * Warning: hinders internationalization
     */
    public static String s(int n) {
        return (n != 1) ? "s" : "";
    }

    /**
     * Warning: hinders internationalization
     */
    public static String ies(int n) {
        return (n != 1) ? "ies" : "y";
    }

    public static String substitute(String string, Object[] substitutions) {
        for (int i = 0; i < substitutions.length; i++) {
            string = StringUtil.replaceAll(string, "$" + (i + 1),
                    substitutions[i].toString());
        }
        return string;
    }

    public static String classNameWithoutQualifiers(String className) {
        return className.substring(
            Math.max(className.lastIndexOf("."), className.lastIndexOf("$")) + 1);
    }

    public static String classNameWithoutPackageQualifiers(String className) {
        return className.substring(className.lastIndexOf(".") + 1);
    }

    public static String repeat(char c, int n) {
        StringBuilder b = new StringBuilder();

        for (int i = 0; i < n; i++) {
            b.append(c);
        }

        return b.toString();
    }

    /**
     *  Line-wraps s by inserting a newline instead of the first space after the nth
     *  column. Word-wraps.
     */
    public static String split(String s, int n) {
        StringBuilder b = new StringBuilder();
        boolean wrapPending = false;

        for (int i = 0; i < s.length(); i++) {
            if (((i % n) == 0) && (i > 0)) {
                wrapPending = true;
            }

            char c = s.charAt(i);

            if (wrapPending && (c == ' ')) {
                b.append("\n");
                wrapPending = false;
            } else {
                b.append(c);
            }
        }

        return b.toString();
    }

    public static String capitalize(String word) {
        if (word.length() == 0) {
            return word;
        }

        return (word.charAt(0) + "").toUpperCase() + word.substring(1);
    }

    public static String uncapitalize(String word) {
        if (word.length() == 0) {
            return word;
        }

        return (word.charAt(0) + "").toLowerCase() + word.substring(1);
    }

    /**
     * Converts the comma-delimited string into a List of trimmed strings.
     * @param s a String with comma-delimited values
     * @return a List of the Strings that were delimited by commas
     */
    public static List<String> fromCommaDelimitedString(String s) {
        if (s.trim().length() == 0) { return new ArrayList<>(); }
        List<String> result = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(s, ",");

        while (tokenizer.hasMoreTokens()) {
            result.add(tokenizer.nextToken().trim());
        }

        return result;
    }

    /**
     * Returns a List of empty Strings.
     * @param size the size of the List to create
     * @return a List of blank Strings
     */
    public static List<String> blankStringList(int size) {
        List<String> list = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            list.add("");
        }

        return list;
    }

    public static String toFriendlyName(String className) {
        return toFriendlyName(className, null);
    }

    public static String friendlyName(Class c) {
        return toFriendlyName(c.getName());
    }

    public static String toFriendlyName(String className, String substringToRemove) {
        String name = className;

        //Remove substring sooner rather than later because, for example,
        //?"PlugIn" will become "Plug In". [Jon Aquino]
        if (substringToRemove != null) {
            name = StringUtil.replaceAll(name, substringToRemove, "");
        }

        name = StringUtil.classNameWithoutQualifiers(name);
        name = insertSpaces(name);

        return name;
    }

    public static String insertSpaces(String s) {
        if (s.length() < 2) {
            return s;
        }

        String result = "";

        for (int i = 0; i < (s.length() - 2); i++) { //-2
            result += s.charAt(i);

            if ((Character.isLowerCase(s.charAt(i))
                && Character.isUpperCase(s.charAt(i + 1)))
                || (Character.isUpperCase(s.charAt(i + 1))
                    && Character.isLowerCase(s.charAt(i + 2)))) {
                result += " ";
            }
        }

        result += s.charAt(s.length() - 2);
        result += s.charAt(s.length() - 1);

        return result.trim();
    }

    /**
     * Returns the elements of c separated by commas. If c is empty, an empty
     * String will be returned.
     * @param c a Collection of objects to convert to Strings and delimit by commas
     * @return a String containing c's elements, delimited by commas
     */
    public static String toCommaDelimitedString(Collection<?> c) {
        return toDelimitedString(c, ", ");
    }

    /**
     *  Returns original with all occurrences of oldSubstring replaced by
     *  newSubstring
     */
    public static String replaceAll(
        String original,
        String oldSubstring,
        String newSubstring) {
        return replace(original, oldSubstring, newSubstring, true);
    }

    /**
     *  Returns original with occurrences of oldSubstring replaced by
     *  newSubstring. Set all to true to replace all occurrences, or false to
     *  replace the first occurrence only.
     */
    public static String replace(
        String original,
        String oldSubstring,
        String newSubstring,
        boolean all) {
        StringBuffer b = new StringBuffer(original);
        replace(b, oldSubstring, newSubstring, all);

        return b.toString();
    }

    /**
     *  Replaces all instances of the String o with the String n in the
     *  StringBuffer orig if all is true, or only the first instance if all is
     *  false. Posted by Steve Chapel <schapel@breakthr.com> on UseNet
     */
    public static void replace(StringBuffer orig, String o, String n, boolean all) {
        if ((orig == null) || (o == null) || (o.length() == 0) || (n == null)) {
            throw new IllegalArgumentException("Null or zero-length String");
        }

        int i = 0;

        while ((i + o.length()) <= orig.length()) {
            if (orig.substring(i, i + o.length()).equals(o)) {
                orig.replace(i, i + o.length(), n);

                if (!all) {
                    break;
                } else {
                    i += n.length();
                }
            } else {
                i++;
            }
        }
    }

    /**
     * Returns an throwable's stack trace
     */
    public static String stackTrace(Throwable t) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        t.printStackTrace(ps);

        return os.toString();
    }

    public static String head(String s, int lines) {
        int newlinesEncountered = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                newlinesEncountered++;
                if (newlinesEncountered == lines) {
                    return s.substring(0, i);
                }
            }
        }
        return s;
    }

    public static String limitLength(String s, int maxLength) {
        Assert.isTrue(maxLength >= 3);

        if (s == null) {
            return null;
        }

        if (s.length() > maxLength) {
            return s.substring(0, maxLength - 3) + "...";
        }

        return s;
    }

    public static boolean isNumber(String token) {
        try {
            Double.parseDouble(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String toDelimitedString(Collection<?> c, String delimiter) {
        if (c.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        for (Object object : c) {
            result.append(delimiter).append(object == null ? "" : object.toString());
        }

        return result.substring(delimiter.length());
    }

    public static String toTimeString(long milliseconds) {
        long remainder = milliseconds;
        long days = remainder / 86400000;
        remainder = remainder % 86400000;

        long hours = remainder / 3600000;
        remainder = remainder % 3600000;

        long minutes = remainder / 60000;
        remainder = remainder % 60000;

        long seconds = remainder / 1000;
        String s = "";

        if (days > 0) {
            s += (days + " days ");
        }

        s += (Fmt.fmt(hours, 2, Fmt.ZF)
                + ":"
                + Fmt.fmt(minutes, 2, Fmt.ZF)
                + ":"
                + Fmt.fmt(seconds, 2, Fmt.ZF));

        return s;
    }


    public static boolean isEmpty(String value) {
        return (value == null || value.trim().length() == 0);
    }

    public static String fillString(int len, char ch) {
        StringBuilder buf = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            buf.append(ch);
        }

        return buf.toString();
    }

    // set up a locale independent decimal formatter, using dot separator and no grouping
    static DecimalFormat allDecimals;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        allDecimals = new DecimalFormat("#0.0###########", symbols);
        allDecimals.setGroupingUsed(false);
    }
    /**
     * format Doubles to a String representation, cutting zeroes from the decimal end
     * the minimum number of decimals is one, hinting the decimal nature of this number
     * the maximimum number of decimal is hardcoded 12 and will be rounded
     * eg. 1234.000 -> "1234.0", 1234.5600 -> "1234.56"
     * @param d
     * @return string
     */
    public static String toString(double d) {
        String s = allDecimals.format(d);
        double d2 = Double.valueOf(s);
        // check if we lost precision and if so we accept floating point
        // representation
        if (d != d2)
            s = Double.toString(d);

        return s;
    }
}
