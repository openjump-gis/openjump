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
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.HTMLFrame;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.task.TaskMonitorManager;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTextArea;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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
      File plugInDirectory = context.getWorkbenchContext().getWorkbench()
          .getPlugInManager().getPlugInDirectory();
      if (null == plugInDirectory || !plugInDirectory.exists()) {
        LOG.debug("BeanTools plugin has not been initialized : the plugin directory is missing");
        return;
      }
      beanShellDirName = plugInDirectory.getPath() + File.separator
          + I18N.get("ui.plugin.customize.BeanToolsPlugIn.BeanTools");
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
            File[] files = file.listFiles();
            Arrays.sort(files, new Comparator<File>(){
                    Pattern pattern = Pattern.compile(".*?([0-9]+).*");
                    public int compare(File f1, File f2) {
                        Matcher m1 = pattern.matcher(f1.getName());
                        Matcher m2 = pattern.matcher(f2.getName());
                        if (m1.matches() && m2.matches()) {
                            return Integer.valueOf(m1.group(1)).compareTo(Integer.valueOf(m2.group(1)));
                        }
                        else return f1.getName().compareTo(f2.getName());
                    }
                    public boolean equals(Object obj) {
                        return this == obj;
                    } 
            });
            for (File f : files) {scanBeanShellDir(f, context);}
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
            JMenu parent = (JMenu) featureInstaller.createMenusIfNecessary(
                FeatureInstaller.wrapMenu(menu),
                ancestors.split(File.separator.replace("\\", "\\\\"))).getWrappee();
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
    	//final ToolboxDialog toolbox = new ToolboxDialog(context.getWorkbenchContext());
    	// [mmichaud 2012-08-25] added an output TextArea to display script
    	// outputs.
    	// I did not use HTMLFrame because when the script output is redirected
    	// to HTMLFrame, a new line is automatically inserted after each print.
    	// Instead, copying JTextArea content to HTMLFrame after the end is OK.
    	final JTextArea console = new JTextArea(15,60);
    	final OutputStream out = getOutputStream(console);
    	
        console.append("************************************************************");
    	console.append("\nScript started from \"" + lastcmd + "\" at\n" +
    	    String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS", new Date()));
    	console.append("\n************************************************************\n");
    	final JDialog dialog = displayConsole(context, console);
    	
    	long t0 = System.currentTimeMillis();
    	try {
            final Interpreter interpreter = new Interpreter();
            interpreter.setOut(new PrintStream(out));
            interpreter.setErr(new PrintStream(out));
            interpreter.setClassLoader(context.getWorkbenchContext().getWorkbench()
    				.getPlugInManager().getClassLoader());
            interpreter.set("wc", context.getWorkbenchContext());
            interpreter.eval("setAccessibility(true)");
            interpreter.eval("import com.vividsolutions.jts.geom.*");
            interpreter.eval("import com.vividsolutions.jump.feature.*");
            interpreter.source(lastcmd);
            console.append("\nExecuted in " + (System.currentTimeMillis()-t0) + " ms\n");
            
            dialog.addWindowListener(new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                        HTMLFrame outputFrame = context.getOutputFrame();
                        outputFrame.createNewDocument();
                        outputFrame.addText(console.getText());
                    } 
            });
        } 
    	catch (EvalError e) {
    	    console.append("\n" + e.getMessage());
    	    for (int i = 0 ; i < e.getStackTrace().length ; i++) {
    	        console.append("\n" + e.getStackTrace()[i].toString());
    	    }
    	    console.append("\nExecuted with errors in " + (System.currentTimeMillis()-t0) + " ms");
            //toolbox.getContext().getErrorHandler().handleThrowable(e);
        }
    	return true;
    }
    
    private OutputStream getOutputStream(final JTextArea textArea) {
        return new java.io.OutputStream() {
    	    public void write(int b) {
    	        textArea.append(String.valueOf((char) b));
    	    }
    	    public void write(byte b[], int off, int len) {
    	        textArea.append(new String(b, off, len));
    	    }
    	};
    }
    
    private JDialog displayConsole(final PlugInContext context, final JTextArea textArea) {
        JDialog consoleDialog = new JDialog(context.getWorkbenchFrame(), "BeanTool output", false);
        consoleDialog.add(new JScrollPane(textArea));
        consoleDialog.setMinimumSize(textArea.getPreferredSize());
        GUIUtil.centre(consoleDialog, context.getWorkbenchFrame());
    	consoleDialog.setVisible(true);
    	return consoleDialog;
    }
    
}