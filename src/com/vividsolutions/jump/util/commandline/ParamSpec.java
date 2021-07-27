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

package com.vividsolutions.jump.util.commandline;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.JUMPWorkbench;

/**
 * Specifies the syntax for a single option on a command line.
 */
public class ParamSpec {

  private final static int NARGS_ZERO_OR_MORE = -1;
  private final static int NARGS_ONE_OR_MORE = -2;
  private final static int NARGS_ZERO_OR_ONE = -3;

  private final Vector<String> names = new Vector<>();
  // number of arguments needed, will be checked
  private final int nNeededArgs;
  private String syntaxPattern;
  private String doc; // option description
  private final Vector<Param> params = new Vector<>();

  public ParamSpec(String[] optNames, int numberOfNeededArgs, String desc) {
    for (String name : optNames) {
      names.add(name.toLowerCase());
    }
    doc = desc;
    nNeededArgs = numberOfNeededArgs;
  }

  public ParamSpec(String optName, int needed, String desc) {
    this(new String[] { optName }, needed, desc);
  }

  public void setDoc(String docLine) {
    doc = docLine;
  }

  public String getDesc() {
    return doc;
  }

  public int getNumOptions() {
    return params.size();
  }

  public Param getOption(int i) {
    if (params.size() > 0) {
      return params.elementAt(i);
    }

    return null;
  }

  /**
   * @return last param, if at least one was defined, else null 
   */
  public Param getParam() {
    if (params.size() > 0) {
      return params.lastElement();
    }

    return null;
  }

  /**
   * merge all option args into one list e.g. 
   * value1, value2
   * from
   * -param value1 -param value2
   */
  public Iterator<String> getAllArguments() {
    Vector<String> all = new Vector<>();
    for (Param option : params) {
      all.addAll(Arrays.asList(option.getArgs()));
    }
    return all.iterator();
  }

  public boolean hasArguments() {
    return params.size() > 0;
  }

  final Vector<String> getNames() {
    return names;
  }

  public boolean matches(String name) {
    return names.contains(name.toLowerCase());
  }

  int getAllowedArgs() {
    return nNeededArgs;
  }

  private void checkNumArgs(String[] args) throws ParseException {
    //if (nNeededArgs == NARGS_ZERO_OR_MORE) {
      // this is senseless as it allows everything
    //} else
    if (nNeededArgs == NARGS_ONE_OR_MORE) {
      if (args.length <= 0) {
        throw new ParseException("option " + names
            + ": expected one or more args, found " + args.length);
      }
    } else if (nNeededArgs == NARGS_ZERO_OR_ONE) {
      if (args.length > 1) {
        throw new ParseException("option " + names
            + ": expected zero or one arg, found " + args.length);
      }
    }
    // we complain only if there are too few arguments
    // more can as well be files that were carelessly placed
    else if (args.length < nNeededArgs) {
      String msg = I18N.getInstance().get(JUMPWorkbench.I18NPREFIX
          + "option-{0}-needs-{1}-parameters-but-only-{2}-were-given.",
          StringUtils.join(names, ", "), nNeededArgs, args.length);
      msg += "\n\n" + getDesc();
      throw new ParseException(msg);
    }
  }

  public Param addParam(Vector<String> v) throws ParseException {
    String[] args = v.toArray(new String[0]);
    checkNumArgs(args);
    String[] argsNeeded = new String[nNeededArgs];
    System.arraycopy(args, 0, argsNeeded, 0, nNeededArgs);
    Param opt = new Param(this, argsNeeded);
    params.add(opt);
    return opt;
  }
}
