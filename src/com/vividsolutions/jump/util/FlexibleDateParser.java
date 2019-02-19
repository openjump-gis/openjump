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

import java.awt.Color;
import java.awt.Component;

import java.io.IOException;
import java.io.InputStream;

import java.text.*;

import java.util.*;

import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Warning: This class can parse a wide variety of formats. This flexibility is fine for parsing user
 * input because the user immediately sees whether the parser is correct and can fix it if
 * necessary. However, GML files are advised to stick with a safe format like yyyy-MM-dd.
 * yy/MM/dd is not as safe because while 99/03/04 will be parsed as yyyy/MM/dd, 
 * 02/03/04 will be parsed as MM/dd/yyyy (because MM/dd/yyyy appears earlier than yyyy/MM/dd
 * in FlexibleDateParser.txt).
 */
public class FlexibleDateParser {
    private static FlexibleDateParser instance = null;

    private static Collection<SimpleDateFormat> lenientFormatters = null;
    private static Collection<SimpleDateFormat> unlenientFormatters = null;

    //CellEditor used to be a static field CELL_EDITOR, but I was getting
    //problems calling it from ESETextField (it simply didn't appear).
    //The problems vanished when I turned it into a static class. I didn't
    //investigate further. [Jon Aquino]
    public static final class CellEditor extends DefaultCellEditor {
        private Object value;
        DateFormat formatter;

        public CellEditor(DateFormat formatter) {
            super(new JTextField());
            //Same formatter as used by JTable.DateRenderer. [Jon Aquino]
            this.formatter = formatter;
        }

        public boolean stopCellEditing() {
            String newValue = (String) super.getCellEditorValue();
            try {
              // allow nullification
              if (newValue == null || newValue.isEmpty())
                this.value = null;
              else
                // First try to use the user defined formatter
                try {
                  this.value = formatter.parse(newValue);
                } catch(ParseException e1) {
                  this.value = FlexibleDateParser.getDefaultInstance().parse(newValue, true);
                }
            } catch (Exception e) {
                // red alert ;) please try again
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
                format(value),
                isSelected,
                row,
                column);
        }

        private String format(Object date) {
          if (date == null || date.toString().isEmpty())
            return "";
          if (date instanceof Date)
            return formatter.format(date);
          else
            return date.toString();
        }

        public Object getCellEditorValue() {
            return value;
        }
    }
    
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

    private Collection<DatePattern> sortByComplexity(Collection<DatePattern> patterns) {
        //Least complex to most complex. [Jon Aquino]
        TreeSet<DatePattern> sortedPatterns = new TreeSet<>(new Comparator<DatePattern>() {
            public int compare(DatePattern o1, DatePattern o2) {
                int result = complexity(o1.pattern) - complexity(o2.pattern);
                if (result == 0) {
                    //The two patterns have the same level of complexity.
                    //Sort by order of appearance (e.g. to resolve
                    //MM/dd/yyyy vs dd/MM/yyyy [Jon Aquino]
                    result = o1.index - o2.index;
                }
                return result;
            }

            private TreeSet<String> uniqueCharacters = new TreeSet<>();

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

    private Collection<SimpleDateFormat> lenientFormatters() {
        if (lenientFormatters == null) {
            load();
        }
        return lenientFormatters;
    }

    private Collection<SimpleDateFormat> unlenientFormatters() {
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

    private Date parse(String s, Collection<SimpleDateFormat> formatters) throws ParseException {
        ParseException firstParseException = null;

        for (SimpleDateFormat formatter : formatters) {

            if (verbose) {
                System.out.println(
                    s
                        + " -- "
                        + formatter.toPattern()
                        + (formatter.isLenient() ? "lenient" : ""));
            }

            try {
              Date d = parse(s, formatter);
                return d;
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

        // If the parser found date is in year 1970 and 70 is not in the
        // original String, set year to current year : why ???
        if ((calendar.get(Calendar.YEAR) == 1970) && !s.contains("70")) {
            calendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
        }

        return calendar.getTime();
    }

    private static class DatePattern {
        private String pattern;
        private int index;
        public DatePattern(String pattern, int index) {
            this.pattern = pattern;
            this.index = index;
        }
        public String toString() {
            return pattern;
        }
    }

    public FlexibleDateParser setLocale(Locale locale) {
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(locale);
        lenientFormatters = null;
        unlenientFormatters = null;
        load();
        Locale.setDefault(defaultLocale);
        return this;
    }

    private void load() {
        if (lenientFormatters == null) {
            // Does not use 18N to be able to reload another language file dynamically
            // (with 18N, things seems to be started only once at the start of the application)
            ResourceBundle resourceBundle = ResourceBundle.getBundle("language/jump");

            try (InputStream inputStream =
                         getClass().getResourceAsStream(resourceBundle.getString(
                                 "com.vividsolutions.jump.util.FlexibleDateParser")
                         )) {
                Collection<DatePattern> patterns = new ArrayList<>();
                int index = 0;
                for (String line : FileUtil.getContents(inputStream)) {

                    if (line.trim().length() > 0 && !line.startsWith("#")) {
                        patterns.add(new DatePattern(line, index));
                        index++;
                    }

                }
                unlenientFormatters = toFormatters(false, patterns);
                lenientFormatters = toFormatters(true, patterns);
            } catch (IOException e) {
                Assert.shouldNeverReachHere(e.toString());
            }
        }
    }

    private Collection<SimpleDateFormat> toFormatters(boolean lenient, Collection<DatePattern> patterns) {
        List<SimpleDateFormat> formatters = new ArrayList<>();
        //Sort from least complex to most complex; otherwise, ddMMMyyyy 
        //instead of MMMd will match "May 15". [Jon Aquino]
        for (DatePattern pattern : sortByComplexity(patterns)) {
            SimpleDateFormat formatter = new SimpleDateFormat(pattern.pattern);
            formatter.setLenient(lenient);
            formatters.add(formatter);
        }
        return formatters;
    }

    public void setVerbose(boolean b) {
        verbose = b;
    }

    public static FlexibleDateParser getDefaultInstance() {
      if (instance == null)
        instance = new FlexibleDateParser();
      return instance;
    }

    public static void main(String[] args) throws Exception {
      FlexibleDateParser fdp = new FlexibleDateParser();
      fdp.setVerbose(true);

      //System.out.println(new FlexibleDateParser().parse("03-Mars-1998", false));
      //System.out.println(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse("2008-11-11T00:00:00.000+0200"));
      System.out.println(fdp.parse("2019/02/17 22:44:35.325+02", true));
      //System.out.println(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSX").parse("2019/02/17 22:44:35.325+02"));
    }
}
