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
package org.openjump.core.ui.plugin.customize;

import bsh.EvalError;
import bsh.Interpreter;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxDialog;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.task.TaskMonitorManager;

import java.io.File;
import java.io.IOException;

import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import org.apache.log4j.Logger;

/**
 * This OpenJUMP PlugIn adds the capability to launch a scripted file from the menu.
 * The original design is from ISA (Larry Becker - 2005)
 * Modified by Micha&euml;l Michaud in order to make hirarchical menus possible.
 */
public class BeanToolsPlugIn extends AbstractPlugIn {
	
	private static final Logger LOG = Logger.getLogger(BeanToolsPlugIn.class);
	private static final String sName = I18N.get("org.openjump.core.ui.plugin.customize.BeanToolsPlugIn.Bean-Tools");
	
    private String lastcmd = "";
    private String beanShellDirName;
    private TaskMonitorManager taskMonitorManager;
    private FeatureInstaller featureInstaller;
    
    public void initialize(PlugInContext context) throws Exception {
    	File plugInDirectory = context.getWorkbenchContext()
    	                              .getWorkbench()
    	                              .getPlugInManager()
    	                              .getPlugInDirectory();
    	if (null == plugInDirectory || !plugInDirectory.exists()) {
    		LOG.debug("BeanTools plugin has not been initialized : the plugin directory is missing");
    		return;
    	}
        beanShellDirName = plugInDirectory.getPath() +  File.separator +
            I18N.get("ui.plugin.customize.BeanToolsPlugIn.BeanTools");
        File beanShellDir = new File(beanShellDirName);
        featureInstaller = context.getFeatureInstaller();
        taskMonitorManager = new TaskMonitorManager();
        if (beanShellDir.exists()) {
            scanBeanShellDir(beanShellDir, context);
        }
    }
    
   /**
    * Extracts the filepath as a String from dir to file
    */ 
    private String ancestors(File dir, File file) throws IOException {
        String path = file.getCanonicalPath();
        return path.substring(path.lastIndexOf(dir.getName()),
                              path.lastIndexOf(file.getName()));
    }
    
   /**
    * Scan beanShellDir iteratively and makes a script menu-item from each
    * .bsh file.
    */
    private void scanBeanShellDir(final File file, final PlugInContext context) throws IOException {
        // iterates over subdirectories
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {scanBeanShellDir(f, context);}
        }
        // add a menu item for the beanshell script
        else if (file.getName().endsWith(".bsh")) {
            File beanShellDir = new File(beanShellDirName);
            String ancestors = ancestors(beanShellDir, file);
            String shellName = file.getName().substring(0, file.getName().length()-4);
            JMenu menu = featureInstaller.menuBarMenu(MenuNames.CUSTOMIZE);
            if (menu == null) {
                menu = (JMenu) featureInstaller.installMnemonic(new JMenu(I18N.get(MenuNames.CUSTOMIZE)), featureInstaller.menuBar());
                featureInstaller.menuBar().add(menu);
            }
            JMenu parent = featureInstaller.createMenusIfNecessary(menu, ancestors.split(File.separator.replace("\\","\\\\")));
            final JMenuItem menuItem = featureInstaller.installMnemonic(new JMenuItem(shellName), parent);
            final ActionListener listener = AbstractPlugIn.toActionListener(this, context.getWorkbenchContext(), taskMonitorManager);
            menuItem.addActionListener(new ActionListener() {
                // if one press this menu item
                public void actionPerformed(ActionEvent e) {
                    // change the lastcmd path
                    if (e != null) lastcmd = file.getPath();
                    // execute this plugin execute method
                    listener.actionPerformed(e);
                }
            });
            parent.add(menuItem);
        }
        // file is not a directory and it does not end with .bsh
        else;
    }
    
    public String getName() {
        return sName;
    }
    
    public boolean execute(final PlugInContext context) throws Exception {
    	ToolboxDialog toolbox = new ToolboxDialog(context.getWorkbenchContext());
    	try {
            Interpreter interpreter = new Interpreter();
            interpreter.setClassLoader(toolbox.getContext().getWorkbench()
    				.getPlugInManager().getClassLoader());
            interpreter.set("wc", toolbox.getContext());
            interpreter.eval("setAccessibility(true)");
            interpreter.eval("import com.vividsolutions.jts.geom.*");
            interpreter.eval("import com.vividsolutions.jump.feature.*");
            //interpreter.source(beanShellDirName + File.separator + lastcmd + ".bsh");
            interpreter.source(lastcmd);
        } 
    	catch (EvalError e) {
            toolbox.getContext().getErrorHandler().handleThrowable(e);
        }
    	return true;
    }
    
}