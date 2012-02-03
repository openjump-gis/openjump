/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2005 Integrated Systems Analysts, Inc.
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
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida
 * USA
 *
 * (850)862-7321
 */
 
package org.openjump.util.python;

import org.python.util.InteractiveConsole;
import org.python.core.*;
import bsh.JavaCharStream;
import bsh.util.JConsole;
import java.io.*;


public class PythonInteractiveInterpreter extends InteractiveConsole implements Runnable
{
    transient Reader in;
    transient PrintStream out;
    transient PrintStream err;
    JConsole console; 
    
    public PythonInteractiveInterpreter(JConsole console)
    {
    	super();
    	this.console = console;
    	in = console.getIn();
    	out = console.getOut();
    	err = console.getErr();
    	setOut(out);
    	setErr(err);
    }
    
    public void run()
    {
        boolean eof = false;
    	JavaCharStream stream = new JavaCharStream(in, 1, 1);

		exec("_ps1 = sys.ps1");
		PyObject ps1Obj = get("_ps1");
		String ps1 = ps1Obj.toString();
		
		exec("_ps2 = sys.ps2");
		PyObject ps2Obj = get("_ps2");
		String ps2 = ps2Obj.toString();
		out.print(getDefaultBanner()+"\n");
		
		out.print(ps1);
		String line = "";
		
    	while( !eof )
    	{
    		// try to sync up the console
    		System.out.flush();
    		System.err.flush();
    		Thread.yield();  // this helps a little
    		
    		try
			{
    			boolean eol = false;
    			line = "";
    			
    			while (!eol)
    			{
    				char aChar = stream.readChar();
    				eol = (aChar == '\n');
    				if (!eol)
    					line = line + aChar;
    			}
    			
    			//hitting Enter at prompt returns a semicolon
    			//get rid of it since it returns an error when executed
    			if (line.equals(";"))line = "";

    			{	
    				boolean retVal = push(line);
    				
    				if (retVal)
    				{
    					out.print(ps2);
    				}
    				else
    				{
    					out.print(ps1);
    				}
    			}
			}
    		catch (IOException ex)
			{
			}
    	}
	} 
}
