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

/**
 * A class to parse application command-line parameters and arguments.
 */
public class CommandLine {

  // store parameter specifications in the order defined
  private final Vector<ParamSpec> paramSpecs = new Vector<>();
  // store parameters in the order parsed
  private final Vector<Param> params = new Vector<>();

  // store plain arguments, like file/project files
  private final Vector<String> arguments = new Vector<>();

  private char optionChar = '-'; // '-' is used for cross-platform compatibility

  public CommandLine() {
  }

  public CommandLine(char optionCh) {
    optionChar = optionCh;
  }

  public void addParamSpec(ParamSpec paramSpec) {
    // TODO: should probably check for duplicate option names here
    paramSpecs.add(paramSpec);
  }

  private ParamSpec getParamSpec(String name) {
    for (ParamSpec paramSpec : paramSpecs) {
      if (paramSpec.matches(name)) {
        return paramSpec;
      }
    }
    return null;
  }

  /**
   * get last Param with that name
   * @param name
   * @return Param or null
   */
  public Param getParam(String name) {
    ParamSpec spec = getParamSpec(name);
    return spec != null ? spec.getParam() : null;
  }

  public Iterator<ParamSpec> getParamSpecs() {
    return paramSpecs.iterator();
  }

  public Iterator<Param> getParams() {
    return params.iterator();
  }

  public Iterator<String> getArguments() {
    return arguments.iterator();
  }

  public boolean hasParam(String name) {
    ParamSpec spec = getParamSpec(name);
    return spec != null  && spec.hasArguments();
  }

  /**
   * get all values of all parameters(options) with the given name
   * see {@link ParamSpec#getAllArguments()}
   * 
   * @param name name of the arguments
   * @return an iterator to iterate through arguments with this name
   */
  public Iterator<String> getParamsArguments(String name) {
    ParamSpec spec = getParamSpec(name);
    return spec != null ? spec.getAllArguments() : Collections.emptyIterator();
  }

  public String printDoc() {
    return printDoc(null);
  }

  public String printDoc(Exception e) {

    StringBuilder out = new StringBuilder();

    if (e != null)
      out.append("Error:\n  ").append(e.getMessage()).append("\n\n");

    out.append("Syntax:\n  oj_starter [-parameter [<argument>]]... [<project_file>]... [<data_file>]...\n\nParameters:\n");

    for (ParamSpec paramSpec : paramSpecs) {
      String names = "";
      for (String name : paramSpec.getNames()) {
        names = names.isEmpty() ? optionChar + name : names + ", " + optionChar + name;
      }
      out.append("  ").append(names).append("\n    ").append(paramSpec.getDesc()).append("\n");
    }

    return out.toString();
  }

  public void parse(String[] args) throws ParseException {
    if (params.size() > 0 || arguments.size() > 0)
      throw new ParseException("CommandLine already contains parsed parameters or arguments!");

    int i = 0;
    while (i < args.length) {
      ParamSpec paramSpec;
      // check for valid option
      if (args[i].charAt(0) == optionChar
          && (paramSpec = getParamSpec(args[i].substring(1))) != null) {

        Param param = paramSpec.addParam(splitParamArguments(args, i));
        // forward pointer to after this option's arguments
        i += 1 + param.getNumArgs();
        
        // add to list
        params.add(param);
      }
      // check for files
      else if (new File(args[i]).exists()) {
        arguments.add(args[i]);
        i++;
      } else
        throw new ParseException(I18N.getInstance().get(getClass().getName()
            + ".unknown-option-or-file-not-found-{0}", args[i]));
    }
  }

  // create list containing only the parameters up to the next valid parameter
  private Vector<String> splitParamArguments(String[] args, int i) {
    Vector<String> params = new Vector<>();
    for (int j = ++i; j < args.length; j++) {
      String param = args[j];
      if (param.charAt(0) == optionChar
          && getParamSpec(param.substring(1)) != null)
        break;
      params.add(param);
    }
    return params;
  }
}
