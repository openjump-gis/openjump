
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
 * www.ashs.isa.com
 */


package org.openjump.core.ui.plugin.customize;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import bsh.util.JConsole;

import org.apache.log4j.Logger;
import org.openjump.util.python.JUMP_GIS_Framework;
import org.openjump.util.python.ModifyGeometry;
import org.openjump.util.python.PythonInteractiveInterpreter;
import org.python.core.*;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxDialog;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxPlugIn;
import java.util.Properties;
import java.io.File;

public class PythonToolsPlugIn extends ToolboxPlugIn
{
	private static final Logger LOG = Logger.getLogger(PythonToolsPlugIn.class);
	private static String sName = "Python Console and Tools";
	//-- add string to language files
	//private static final String sName = I18N.get("org.openjump.core.ui.plugin.customize.PythonToolsPlugIn.Python-Tools");

    
    public String getName() {
        return sName;
    }
    
	public void initialize(PlugInContext context) throws Exception
    {        
		this.sName = I18N.get("org.openjump.core.ui.plugin.customize.PythonToolsPlugIn.Python-Console-and-Tools");
        //[sstein - old, access via button]
		//context.getWorkbenchContext().getWorkbench().getFrame().getToolBar().addPlugIn(getIcon(), this, createEnableCheck(context.getWorkbenchContext()), context.getWorkbenchContext());
        //[sstein - neu, access via menu]
		FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
    	featureInstaller.addMainMenuItem(
    	        this,								//exe
                new String[] {MenuNames.CUSTOMIZE}, 	//menu path
                this.sName + "...", //name methode .getName recieved by AbstractPlugIn 
                false,			//checkbox
                null,			//icon
                createEnableCheck(context.getWorkbenchContext())); //enable check
    }
    
    protected void initializeToolbox(ToolboxDialog toolbox) 
    {
    	final JConsole console = new JConsole();
    	console.setPreferredSize(new Dimension(450, 120));
    	console.println(new ImageIcon(getClass().getResource("jython_small_c.png")));
    	toolbox.getCenterPanel().add(console, BorderLayout.CENTER);
    	toolbox.setTitle("Jython");
    	toolbox.setInitialLocation(new GUIUtil.Location(0, true, 0, true));
    	toolbox.setResizable(true);

    	//setup the interpreter
    	ClassLoader classLoader = toolbox.getContext().getWorkbench().getPlugInManager().getClassLoader();
      	Properties preProperties = new Properties(System.getProperties());
    	String homepath = preProperties.getProperty("user.home");
    	File empty = new File("");
    	String sep = File.separator;
    	//-- [sstein] - old */
    	/*
    	String WORKING_DIR = empty.getAbsoluteFile().getParent() + sep;
    	String jarpathX = new String(WORKING_DIR + "lib");
    	String startuppathX = new String(WORKING_DIR + "lib" + sep + "ext"  + sep + "jython"  + sep);
    	*/
    	//-- [sstein] - new
    	File plugInDirectory = toolbox.getContext().getWorkbench().getPlugInManager().getPlugInDirectory();
    	String jarpath = plugInDirectory.getPath();
    	String startuppath = plugInDirectory.getPath() + sep + "jython"  + sep;
    		
    	Properties postProperties = new Properties();
    	postProperties.put("python.home",homepath);
    	postProperties.put("python.path",startuppath);
    	PySystemState.initialize( preProperties,postProperties,new String[] {""},classLoader);
    	String startupfile = startuppath + "startup.py";
       	PySystemState.add_extdir(jarpath);
       	PySystemState.add_extdir(jarpath + sep + "ext");
     	PythonInteractiveInterpreter interpreter = new PythonInteractiveInterpreter(console);
    	interpreter.set("wc", toolbox.getContext());
    	interpreter.set("toolbox", toolbox);
    	interpreter.set("startuppath", startuppath);
    	interpreter.exec("import sys");
     	JUMP_GIS_Framework.setWorkbenchContext(toolbox.getContext());
     	ModifyGeometry.setWorkbenchContext(toolbox.getContext());
    	toolbox.addToolBar();  //add a new tool bar to the console
       	JMenuBar jMenuBar = new JMenuBar();
       	jMenuBar.add(new JMenu(MenuNames.TOOLS));
       	toolbox.setJMenuBar(jMenuBar);
    	if (new File(startupfile).exists()) interpreter.execfile(startupfile);
    	new Thread(interpreter).start();
    }
	
    private Icon getIcon()
    {
        return new ImageIcon(getClass().getResource("Jython.gif"));
    } 
    
    public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
            .add(checkFactory.createTaskWindowMustBeActiveCheck());
    }
}

