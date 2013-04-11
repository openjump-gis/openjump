
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

package com.vividsolutions.jump.io;

import com.vividsolutions.jump.JUMPException;


/**
 *Simple exception class to express problems parsing data.
 */
public class ParseException extends JUMPException {
    //<<TODO:NAMING>> Perhaps we should expand these names to full words; for example,
    //fileName. cpos is kind of cryptic. Also, the Java naming convention is to
    //separate words with capitals; for example, lineNo rather than lineno. [Jon Aquino]
    public String fname;
    public int lineno;
    public int cpos;

    /** construct exception with a message*/
    public ParseException(String message) {
        super(message);
    }

    /**
     *  More explictly construct a parse exception.
     *  Resulting message will be :message + " in file '" + newFname +"', line " + newLineno + ", char " + newCpos
     * @param message information about the type of error
     * @param newFname filename the error occurred in
     * @param newLineno line number the error occurred at
     * @param newCpos character position on the line
     *
     **/
    public ParseException(String message, String newFname, int newLineno,
        int newCpos) {
        super(message + " in file '" + newFname + "', line " + newLineno +
            ", char " + newCpos);

        //  super(message);
        fname = newFname;
        lineno = newLineno;
        cpos = newCpos;
    }
}
