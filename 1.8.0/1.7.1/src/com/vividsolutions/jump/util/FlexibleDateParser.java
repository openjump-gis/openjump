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

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;

import java.awt.Color;
import java.awt.Component;

import java.io.IOException;
import java.io.InputStream;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;

/**
 * Warning: This class can parse a wide variety of formats. This flexibility is fine for parsing user
 * input because the user immediately sees whether the parser is correct and can fix it if
 * necessary. However, GML files are advised to stick with a safe format like yyyy-MM-dd.
 * yy/MM/dd is not as safe because while 99/03/04 will be parsed as yyyy/MM/dd, 
 * 02/03/04 will be parsed as MM/dd/yyyy (because MM/dd/yyyy appears earlier than yyyy/MM/dd
 * in FlexibleDateParser.txt).
 */
public class FlexibleDateParser {
    private static Collection lenientFormatters = null;
    private static Collection unlenientFormatters = null;
    //CellEditor used to be a static field CELL_EDITOR, but I was getting
    //problems calling it from ESETextField (it simply didn't appear).
    //The problems vanished when I turned it into a static class. I didn't
    //investigate further. [Jon Aquino]
    public static final class CellEditor extends DefaultCellEditor {
        public CellEditor(DateFormat formatter) {
            super(new JTextField());
            //Same formatter as used by JTable.DateRenderer. [Jon Aquino]
            this.formatter = formatter;
        }
        private Object value;
        private FlexibleDateParser parser = new FlexibleDateParser();
        DateFormat formatter;

        public boolean stopCellEditing() {
            try {
                value = parser.parse((String) super.getCellEditorValue(), true);
            } catch (Exception e) {
                ((JComponent) getComponent()).setBorder(new LineBorder(Color.red));

                return false;
            }

            return super.stopCellEditing();
        }

        public Component getTableCellEditorComponent(
            JTable table,
            Object value,
            boolean isSelected,
            int row,
            int column) {
            this.value = null;
            ((JComponent) getComponent()).setBorder(new LineBorder(Color.black));

            return super.getTableCellEditorComponent(
                table,
                format((Date) value),
                isSelected,
                row,
                column);
        }

        private String format(Date date) {
            return (date == null) ? "" : formatter.format(date);
        }

        public Object getCellEditorValue() {
            return value;
        }
    };
    
    public static final class CellRenderer extends DefaultTableCellRenderer {
        
        DateFormat formatter = DateFormat.getDateTimeInstance();
        
        public CellRenderer() { super(); }

        public void setValue(Object value) {
            if (formatter==null) {
                formatter = DateFormat.getDateTimeInstance();
            }
            setText((value == null) ? "" : formatter.format(value));
        }
    }

    private boolean verbose = false;

    private Collection sortByComplexity(Collection patterns) {
        //Least complex to most complex. [Jon Aquino]
        TreeSet sortedPatterns = new TreeSet(new Comparator() {
            public int compare(Object o1, Object o2) {
                int result = complexity(o1.toString()) - complexity(o2.toString());
                if (result == 0) {
                    //The two patterns have the same level of complexity.
                    //Sort by order of appearance (e.g. to resolve
                    //MM/dd/yyyy vs dd/MM/yyyy [Jon Aquino]
                    result = ((Pattern) o1).index - ((Pattern) o2).index;
                }
                return result;
            }

            private TreeSet uniqueCharacters = new TreeSet();

            private int complexity(String pattern) {
                uniqueCharacters.clear();

                for (int i = 0; i < pattern.length(); i++) {
                    if (("" + pattern.charAt(i)).trim().length() > 0) {
                        uniqueCharacters.add("" + pattern.charAt(i));
                    }
                }

                return uniqueCharacters.size();
            }
        });
        sortedPatterns.addAll(patterns);

        return sortedPatterns;
    }

    private Collection lenientFormatters() {
        if (lenientFormatters == null) {
            load();
        }

        return lenientFormatters;
    }

    private Collection unlenientFormatters() {
        if (unlenientFormatters == null) {
            load();
        }

        return unlenientFormatters;
    }

    /**
     * @return null if s is empty
     */
    public Date parse(String s, boolean lenient) throws ParseException {
        if (s.trim().length() == 0) {
            return null;
        }
        //The deprecated Date#parse method is actually pretty flexible. [Jon Aquino]
        // [mmichaud 2012-03-17] Date parse without taking Locale into account
        // -> prefer parse method using localized formatters
        //try {
        //    if (verbose) {
        //        System.out.println(s + " -- Date constructor");
        //    }
        //    return new Date(s);
        //} catch (Exception e) {
        //    //Eat it. [Jon Aquino]
        //}

        try {
            return parse(s, unlenientFormatters());
        } catch (ParseException e) {
            if (lenient) {
                return parse(s, lenientFormatters());
            }

            throw e;
        }
    }

    private Date parse(String s, Collection formatters) throws ParseException {
        ParseException firstParseException = null;

        for (Iterator i = formatters.iterator(); i.hasNext();) {
            SimpleDateFormat formatter = (SimpleDateFormat) i.next();

            if (verbose) {
                System.out.println(
                    s
                        + " -- "
                        + formatter.toPattern()
                        + (formatter.isLenient() ? "lenient" : ""));
            }

            try {
                return parse(s, formatter);
            } catch (ParseException e) {
                if (firstParseException == null) {
                    firstParseException = e;
                }
            }
        }

        throw firstParseException;
    }

    private Date parse(String s, SimpleDateFormat formatter) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Date date = formatter.parse(s, pos);

        if (pos.getIndex() == 0) {
            throw new ParseException(
                "Unparseable date: \"" + s + "\"",
                pos.getErrorIndex());
        }

        //SimpleDateFormat ignores trailing characters in the pattern string that it
        //doesn't need. Don't allow it to ignore any characters. [Jon Aquino]
        if (pos.getIndex() != s.length()) {
            throw new ParseException(
                "Unparseable date: \"" + s + "\"",
                pos.getErrorIndex());
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        if ((calendar.get(Calendar.YEAR) == 1970) && (s.indexOf("70") == -1)) {
            calendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
        }

        return calendar.getTime();
    }

    private static class Pattern {
        private String pattern;
        private int index;
        public Pattern(String pattern, int index) {
            this.pattern = pattern;
            this.index = index;
        }
        public String toString() {
            return pattern;
        }
    }

    private void load() {
        if (lenientFormatters == null) {
            InputStream inputStream =
                getClass().getResourceAsStream(I18N.get("com.vividsolutions.jump.util.FlexibleDateParser"));

            try {
                try {
                    Collection patterns = new ArrayList();
                    int index = 0;
                    for (Iterator i = FileUtil.getContents(inputStream).iterator();
                        i.hasNext();
                        ) {
                        String line = ((String) i.next()).trim();

                        if (line.startsWith("#")) {
                            continue;
                        }

                        if (line.length() == 0) {
                            continue;
                        }

                        patterns.add(new Pattern(line, index));
                        index++;
                    }

                    unlenientFormatters = toFormatters(false, patterns);
                    lenientFormatters = toFormatters(true, patterns);
                } finally {
                    inputStream.close();
                }
            } catch (IOException e) {
                Assert.shouldNeverReachHere(e.toString());
            }
        }
    }

    private Collection toFormatters(boolean lenient, Collection patterns) {
        ArrayList formatters = new ArrayList();
        //Sort from least complex to most complex; otherwise, ddMMMyyyy 
        //instead of MMMd will match "May 15". [Jon Aquino]
        for (Iterator i = sortByComplexity(patterns).iterator(); i.hasNext();) {
            Pattern pattern = (Pattern) i.next();
            SimpleDateFormat formatter = new SimpleDateFormat(pattern.pattern);
            formatter.setLenient(lenient);
            formatters.add(formatter);
        }

        return formatters;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(DateFormat.getDateInstance().parse("03-Mar-1998"));
    }

    public void setVerbose(boolean b) {
        verbose = b;
    }
}
