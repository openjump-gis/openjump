/*
 * Copyright (C) 2009 Integrated Systems Analysts, Inc.
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
 * Fort Walton Beach, Florida 32548
 * USA
 *
 * (850)862-7321
 */
package org.openjump.core.ui.plugin.customize;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Larry Becker
 * This class launches exernal programs with command line options.
 * Stack traces are printed when exceptions occur.
 */
public class CmdRunner {
	
	private static String[] envp = null;
	
	/**
	 * Essentialy a wrapper for Runtime.getRuntime().exec() that waits and catches exceptions.
	 * @param runStr
	 * @param runAndWait True - wait for process to end, False - do not wait for process to end
	 */
	public void run(String [] runStr, boolean runAndWait) {
				
		try
		{        
			Process p = Runtime.getRuntime().exec(runStr, envp);
			if (runAndWait) {
				p.waitFor();
				p.destroy();
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public static void addEnviroment(String enviromentVariable, String value) {
		
		if (envp == null) { //do initialization			
			   Map<String, String> env = System.getenv();
			   int i = 0;
			   envp = new String[env.size()];//TODO
			   for (Iterator it=env.entrySet().iterator(); it.hasNext(); ) {
			      Map.Entry entry = (Map.Entry)it.next();
			      envp[i++] = entry.getKey() + "=" + entry.getValue();
			    }
		}
		
		String[] newEnvp = new String[envp.length + 1];
		for (int i = 0; i < envp.length; i++ ) {
			newEnvp[i] = envp[i];
		}
		newEnvp[newEnvp.length - 1] = enviromentVariable + "=" + value;
		envp = newEnvp;
	}
	
	/**
	 * Run command line and do not wait for process to end
	 * 
	 * @param commandLine - command to run - must be executable at system command line.
	 * @param delimiter - character used to delimit parameters - must not occur inside parameters.
	 */
	public void runLater(String commandLine, char delimiter) {
		String regex = "" + delimiter;
		String [] runStr = commandLine.split(regex, 0);
		run(runStr, false);
	}
	
	/**
	 * Run command line, wait and catch exceptions
	 * 
	 * @param commandLine - command to run - must be executable at system command line.
	 * @param delimiter - character used to delimit parameters - must not occur inside parameters.
	 */
	public void run(String commandLine, char delimiter) {
		String regex = "" + delimiter;
		String [] runStr = commandLine.split(regex, 0);
		run(runStr, true);
	}

	
	/**
	 * @param command - (full) command to run - must be executable at system command line.
	 * @param delimiter - character used to delimit parameters - must not occur inside parameters.
	 * @return output of commmand or null if no ouput produced.
	 */
	public String[] runAndGetOutput(String command, char delimiter) {
		
		ArrayList<String> lines = new ArrayList<String>();
		try {
			File file = File.createTempFile("Cmd", null);
			String filePath = file.getCanonicalPath();
			
			String regex = "" + delimiter;
			String commandLine;
			if (isWindows())
				commandLine = "cmd" + delimiter + "/c" + delimiter;
			else
				commandLine = "sh" + delimiter;
			commandLine +=  command + delimiter + ">" + delimiter + filePath;  //> is redirect output
			commandLine +=  delimiter + "2>&1";  //get error output too
			String [] runStr = commandLine.split(regex, 0);
			run(runStr, true);
			
			file = new File(filePath);  //re-open the file
			
			if (!(file.exists() && file.isFile() && file.canRead()))
				return new String[0];  //command couldn't produce a file			
			
			//read the redirected output
			int fileSize = (int) file.length();
			if (fileSize == 0)
				return new String[0];  //empty file	
			
			FileReader fin = new FileReader(filePath);
			BufferedReader in = new BufferedReader(fin,fileSize);
			String line;
			while ((line = in.readLine()) != null) {				
				lines.add(line);
			}
			in.close();
			file.delete();
		} catch (Throwable t) {
 			t.printStackTrace();
		}
		
		String[] returnString = lines.toArray(new String[lines.size()]);
		
		return returnString;
	}
	
   private static String OS = null;
   
   public static String getOsName() {
      if(OS == null)  OS = System.getProperty("os.name");
      return OS;
   }
   
   public static boolean isWindows() {
      return getOsName().startsWith("Windows");
   }


}
