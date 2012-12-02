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

/**
 * Specifies the syntax for a single option on a command line.
 */
public class OptionSpec {
  public final static int NARGS_ZERO_OR_MORE = -1;
  public final static int NARGS_ONE_OR_MORE = -2;
  public final static int NARGS_ZERO_OR_ONE = -3;

  Vector<String> names = new Vector<String>();
  // number of arguments needed, will be checked
  int nNeededArgs = 0;
  String syntaxPattern;
  String doc = ""; // option description
  Vector<Option> options = new Vector<Option>();

  public OptionSpec(String[] optNames, int needed, String desc) {
    for (String name : optNames) {
      names.add(name.toLowerCase());
    }
    doc = desc;
    nNeededArgs = needed;
  }

  public OptionSpec(String optName, int needed, String desc) {
    this(new String[] { optName }, needed, desc);
  }

  public void setDoc(String docLine) {
    doc = docLine;
  }

  public String getDesc() {
    return doc;
  }

  public int getNumOptions() {
    return options.size();
  }

  public Option getOption(int i) {
    if (options.size() > 0) {
      return (Option) options.elementAt(i);
    }

    return null;
  }

  public Option getOption() {
    if (options.size() > 0) {
      return (Option) options.lastElement();
    }

    return null;
  }

  // merge all options into one list e.g. -param value1 -param value2
  public Iterator getAllOptions() {
    Vector all = new Vector();
    for (Option option : options) {
      all.addAll(Arrays.asList(option.getArgs()));
    }
    return all.iterator();
  }

  public boolean hasOption() {
    return options.size() > 0;
  }

  final Vector<String> getNames() {
    return names;
  }

  boolean matches(String name) {
    return names.contains(name.toLowerCase());
  }

  int getAllowedArgs() {
    return nNeededArgs;
  }

  void checkNumArgs(String[] args) throws ParseException {
    if (nNeededArgs == NARGS_ZERO_OR_MORE) {
      // this is senseless as it allows everything
    } else if (nNeededArgs == NARGS_ONE_OR_MORE) {
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
      throw new ParseException("option " + names + ": expected " + nNeededArgs
          + " args, found " + args.length);
    }
  }

  public Option addOption(Vector v) throws ParseException {
    String[] args = (String[]) v.toArray(new String[] {});
    checkNumArgs(args);
    String[] argsNeeded = new String[nNeededArgs];
    for (int i = 0; i < nNeededArgs; i++) {
      argsNeeded[i] = args[i];
    }

    Option opt = new Option(this, argsNeeded);
    options.add(opt);
    return opt;
  }
}
