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

package com.vividsolutions.jump.workbench.ui.plugin.analysis;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.FlexibleDateParser;
import com.vividsolutions.jump.workbench.ui.GenericNames;

/**
 * A function object for {@link Geometry} functions (which return a Geometry).
 * Provides metadata about the function.
 *
 * @author Martin Davis
 * @version 1.0
 */
public abstract class AttributePredicate {
  static AttributePredicate[] methods = { new EqualPredicate(),
      new NotEqualPredicate(), new LessThanPredicate(),
      new LessThanOrEqualPredicate(), new GreaterThanPredicate(),
      new GreaterThanOrEqualPredicate(), new ContainsPredicate(),
      new StartsWithPredicate(), new EndsWithPredicate(),
      new MatchesPredicate() };

  static AttributePredicate[] methodsCaseInsensitive = {
      new EqualCIPredicate(), new NotEqualCIPredicate(),
      new LessThanCIPredicate(), new LessThanOrEqualCIPredicate(),
      new GreaterThanCIPredicate(), new GreaterThanOrEqualCIPredicate(),
      new ContainsCIPredicate(), new StartsWithCIPredicate(),
      new EndsWithCIPredicate(), new MatchesCIPredicate() };

  static List<String> getNames() {
    List names = new ArrayList();
    for (int i = 0; i < methods.length; i++) {
      names.add(methods[i].name);
    }
    return names;
  }

  static List<String> getNamesCI() {
    List names = new ArrayList();
    for (int i = 0; i < methodsCaseInsensitive.length; i++) {
      names.add(methodsCaseInsensitive[i].name);
    }
    return names;
  }

  static AttributePredicate getPredicate(String name) {
    for (int i = 0; i < methods.length; i++) {
      if (methods[i].name.equals(name))
        return methods[i];
    }
    return null;
  }

  static AttributePredicate getPredicate(String name, boolean caseInsensitive) {
    AttributePredicate pred = null;
    if (caseInsensitive) {
      for (int i = 0; i < methodsCaseInsensitive.length; i++) {
        if (methodsCaseInsensitive[i].name.equals(name))
          pred = methodsCaseInsensitive[i];
      }
    }
    // checkbox might be disabled but still ticked on, return nonCI pred in that
    // case
    if (pred == null) {
      pred = getPredicate(name);
    }

    return pred;
  }

  private static FlexibleDateParser dateParser = new FlexibleDateParser();

  private String name;
  private String description;

  public AttributePredicate(String name) {
    this(name, null);
  }

  public AttributePredicate(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public abstract boolean isTrue(Object arg1, Object arg2);

  protected boolean compareObjects(Object arg1, Object arg2) {
    Object o2 = coerce((String) arg2, arg1);
    if (o2 == null)
      return false;
    int comp = compareTo(arg1, o2);
    if (comp == NOT_COMPARABLE)
      return false;
    return testCompareValue(comp);
  }

  /**
   * Subclasses calling compareObjects should override this method
   * 
   * @param comp
   * @return false
   */
  protected boolean testCompareValue(int comp) {
    return false;
  }

  public static Object coerce(String constantValue, Object attrVal) {
    try {
      if (attrVal instanceof Boolean) {
        return new Boolean(getBooleanLoose(constantValue));
      }
      if (attrVal instanceof Double) {
        return new Double(constantValue);
      }
      if (attrVal instanceof Integer) {
        return new Integer(constantValue);
      }
      if (attrVal instanceof String) {
        return constantValue;
      }
      if (attrVal instanceof Date) {
        return dateParser.parse(constantValue, true);
      }
    } catch (ParseException ex) {
      // eat it
    } catch (NumberFormatException ex) {
      // eat it
    }
    // just return it as a String
    return null;
  }

  protected static final int NOT_COMPARABLE = Integer.MIN_VALUE;

  protected static int compareTo(Object o1, Object o2) {
    if (o1 instanceof Boolean && o2 instanceof Boolean)
      return ((Boolean) o1).equals(o2) ? 0 : 1;

    if (!(o1 instanceof Comparable))
      return NOT_COMPARABLE;
    if (!(o2 instanceof Comparable))
      return NOT_COMPARABLE;
    return ((Comparable) o1).compareTo((Comparable) o2);
  }

  private static boolean getBooleanLoose(String boolStr) {
    return boolStr.equalsIgnoreCase("true") || boolStr.equalsIgnoreCase("yes")
        || boolStr.equalsIgnoreCase("1") || boolStr.equalsIgnoreCase("y");
  }

  // == predicates
  private static class EqualPredicate extends AttributePredicate {
    public EqualPredicate() {
      super("=");
    }

    public boolean isTrue(Object arg1, Object arg2) {
      return compareObjects(arg1, arg2);
    }

    protected boolean testCompareValue(int comp) {
      return comp == 0;
    }
  }
  
  private static class EqualCIPredicate extends EqualPredicate {
    public boolean isTrue(Object arg1, Object arg2) {
      if (arg1 instanceof String){
        arg1 = arg1.toString().toLowerCase();
        arg2 = arg2.toString().toLowerCase();
      }
      return super.compareObjects(arg1, arg2);
    }
  }

  // != predicates
  private static class NotEqualPredicate extends AttributePredicate {
    public NotEqualPredicate() {
      super("<>");
    }

    public boolean isTrue(Object arg1, Object arg2) {
      return compareObjects(arg1, arg2);
    }

    protected boolean testCompareValue(int comp) {
      return comp != 0;
    }
  }

  private static class NotEqualCIPredicate extends NotEqualPredicate {
    public boolean isTrue(Object arg1, Object arg2) {
      if (arg1 instanceof String){
        arg1 = arg1.toString().toLowerCase();
        arg2 = arg2.toString().toLowerCase();
      }
      return super.compareObjects(arg1, arg2);
    }
  }

  // < predicates
  private static class LessThanPredicate extends AttributePredicate {
    public LessThanPredicate() {
      super("<");
    }

    public boolean isTrue(Object arg1, Object arg2) {
      return compareObjects(arg1, arg2);
    }

    protected boolean testCompareValue(int comp) {
      return comp < 0;
    }
  }

  private static class LessThanCIPredicate extends LessThanPredicate {
    public boolean isTrue(Object arg1, Object arg2) {
      if (arg1 instanceof String){
        arg1 = arg1.toString().toLowerCase();
        arg2 = arg2.toString().toLowerCase();
      }
      return super.compareObjects(arg1, arg2);
    }
  }

  // <= predicates
  private static class LessThanOrEqualPredicate extends AttributePredicate {
    public LessThanOrEqualPredicate() {
      super("<=");
    }

    public boolean isTrue(Object arg1, Object arg2) {
      return compareObjects(arg1, arg2);
    }

    protected boolean testCompareValue(int comp) {
      return comp <= 0;
    }
  }

  private static class LessThanOrEqualCIPredicate extends LessThanOrEqualPredicate {
    public boolean isTrue(Object arg1, Object arg2) {
      if (arg1 instanceof String){
        arg1 = arg1.toString().toLowerCase();
        arg2 = arg2.toString().toLowerCase();
      }
      return super.compareObjects(arg1, arg2);
    }
  }

  // > predicates
  private static class GreaterThanPredicate extends AttributePredicate {
    public GreaterThanPredicate() {
      super(">");
    }

    public boolean isTrue(Object arg1, Object arg2) {
      return compareObjects(arg1, arg2);
    }

    protected boolean testCompareValue(int comp) {
      return comp > 0;
    }
  }

  private static class GreaterThanCIPredicate extends GreaterThanPredicate {
    public boolean isTrue(Object arg1, Object arg2) {
      if (arg1 instanceof String){
        arg1 = arg1.toString().toLowerCase();
        arg2 = arg2.toString().toLowerCase();
      }
      return super.compareObjects(arg1, arg2);
    }
  }

  // >= predicates
  private static class GreaterThanOrEqualPredicate extends AttributePredicate {
    public GreaterThanOrEqualPredicate() {
      super(">=");
    }

    public boolean isTrue(Object arg1, Object arg2) {
      return compareObjects(arg1, arg2);
    }

    protected boolean testCompareValue(int comp) {
      return comp >= 0;
    }
  }

  private static class GreaterThanOrEqualCIPredicate extends GreaterThanOrEqualPredicate {
    public boolean isTrue(Object arg1, Object arg2) {
      if (arg1 instanceof String){
        arg1 = arg1.toString().toLowerCase();
        arg2 = arg2.toString().toLowerCase();
      }
      return super.compareObjects(arg1, arg2);
    }
  }

  // substring predicates
  private static class ContainsPredicate extends AttributePredicate {
    public ContainsPredicate() {
      super(GenericNames.CONTAINS);
    }

    public boolean isTrue(Object arg1, Object arg2) {
      if (arg1 == null || arg2 == null)
        return false;
      return arg1.toString().indexOf(arg2.toString()) >= 0;
    }
  }

  private static class ContainsCIPredicate extends ContainsPredicate {
    public boolean isTrue(Object arg1, Object arg2) {
      if (arg1 == null || arg2 == null)
        return false;
      return arg1.toString().toLowerCase()
          .indexOf(arg2.toString().toLowerCase()) >= 0;
    }
  }

  // starts with predicates
  private static class StartsWithPredicate extends AttributePredicate {
    public StartsWithPredicate() {
      super(I18N.get("ui.plugin.analysis.AttributePredicate.starts-with"));
    }

    public boolean isTrue(Object arg1, Object arg2) {
      if (arg1 == null || arg2 == null)
        return false;
      return arg1.toString().startsWith(arg2.toString());
    }
  }

  private static class StartsWithCIPredicate extends StartsWithPredicate {
    public boolean isTrue(Object arg1, Object arg2) {
      if (arg1 == null || arg2 == null)
        return false;
      return arg1.toString().toLowerCase()
          .startsWith(arg2.toString().toLowerCase());
    }
  }

  // ends with predicates
  private static class EndsWithPredicate extends AttributePredicate {
    public EndsWithPredicate() {
      super(I18N.get("ui.plugin.analysis.AttributePredicate.ends-with"));
    }

    public boolean isTrue(Object arg1, Object arg2) {
      if (arg1 == null || arg2 == null)
        return false;
      return arg1.toString().endsWith(arg2.toString());
    }
  }

  private static class EndsWithCIPredicate extends EndsWithPredicate {
    public boolean isTrue(Object arg1, Object arg2) {
      if (arg1 == null || arg2 == null)
        return false;
      return arg1.toString().toLowerCase()
          .endsWith(arg2.toString().toLowerCase());
    }
  }

  // preg match predicates
  private static class MatchesPredicate extends AttributePredicate {
    public MatchesPredicate() {
      super(I18N.get("ui.plugin.analysis.AttributePredicate.matches"));
    }

    public boolean isTrue(Object arg1, Object arg2) {
      if (arg1 == null || arg2 == null)
        return false;
      return Pattern.compile(arg2.toString()).matcher(arg1.toString())
          .matches();
    }
  }

  private static class MatchesCIPredicate extends MatchesPredicate {
    public boolean isTrue(Object arg1, Object arg2) {
      if (arg1 == null || arg2 == null)
        return false;
      return Pattern.compile(arg2.toString(), Pattern.CASE_INSENSITIVE)
          .matcher(arg1.toString()).matches();
    }
  }
}
