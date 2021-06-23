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

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

import com.vividsolutions.jump.I18N;

//<<TODO:NAMING>> Move the command package under ui [Jon Aquino]

/**
 * A class to parse Unix (and DOS/Win)-style application command-lines.
 */
public class CommandLine {

  // store options defs
  private final Vector<OptionSpec> optSpecs = new Vector<>();

  // store optionless file parameters
  private final Vector<String> parVec = new Vector<>(); // store plain params (e.g. projects/files to open)

  private final char optionChar; // the char that indicates an option. Default is '/', which is
                   // NT Standard, but this causes problems on Unix systems, so
                   // '-' is used by JUMPWorkbench (better for cross-platform apps)

  public CommandLine() {
    this('/');
  }

  public CommandLine(char optionCh) {
    optionChar = optionCh;
  }

  public void addOptionSpec(OptionSpec optSpec) {
    // should check for duplicate option names here
    optSpecs.add(optSpec);

  }

  private OptionSpec getOptionSpec(String name) {
    for (OptionSpec optionSpec : optSpecs) {
      if (optionSpec.matches(name)) {
        return optionSpec;
      }
    }
    return null;
  }

  public Option getOption(String name) {
    OptionSpec spec = getOptionSpec(name);
    return spec != null ? spec.getOption() : null;
  }

  /**
   * get all values of all parameters(options) with the given name
   * see {@link OptionSpec#getAllArguments()}
   * 
   * @param name name of the arguments
   * @return an iterator to iterate through arguments with this name
   */
  public Iterator<String> getAllArguments(String name) {
    OptionSpec spec = getOptionSpec(name);
    return spec != null ? spec.getAllArguments() : Collections.emptyIterator();
  }

  public Iterator<String> getParams() {
    return parVec.iterator();
  }

  public boolean hasOption(String name) {
    OptionSpec spec = getOptionSpec(name);
    return spec != null  && spec.hasOption();
  }

  public String printDoc() {
    return printDoc(null);
  }
  
  public String printDoc(Exception e) {

    StringBuilder out = new StringBuilder();
        
    if (e != null)
      out.append("Error:\n  ").append(e.getMessage()).append("\n\n");

    out.append("Syntax:\n  oj_starter [-option [<parameter>]]... [<project_file>]... [<data_file>]...\n\nOptions:\n");

    for (OptionSpec optionSpec : optSpecs) {
      String names = "";
      for (String name : optionSpec.getNames()) {
        names = names.isEmpty() ? optionChar + name : names + ", " + optionChar + name;
      }
      out.append("  ").append(names).append("\n    ").append(optionSpec.getDesc()).append("\n");
    }

    return out.toString();
  }

  public void parse(String[] args) throws ParseException {
    int i = 0;

    while (i < args.length) {
      OptionSpec optSpec;
      // check for valid option
      if (args[i].charAt(0) == optionChar
          && (optSpec = getOptionSpec(args[i].substring(1))) != null) {

        Option opt = optSpec.addOption(splitOptionParams(args, i));

        // forward pointer to after options params
        i += 1 + opt.getNumArgs();
      }
      // check for files
      else if (new File(args[i]).exists()) {
        parVec.add(args[i]);
        i++;
      } else
        throw new ParseException(I18N.getInstance().get(getClass().getName()
            + ".unknown-option-or-file-not-found-{0}", args[i]));
    }

  }

  // create list containing only the parameters up to the next valid parameter
  private Vector<String> splitOptionParams(String[] args, int i) {
    Vector<String> params = new Vector<>();
    for (int j = ++i; j < args.length; j++) {
      String param = args[j];
      if (param.charAt(0) == optionChar
          && getOptionSpec(param.substring(1)) != null)
        break;
      params.add(param);
    }
    return params;
  }
}
