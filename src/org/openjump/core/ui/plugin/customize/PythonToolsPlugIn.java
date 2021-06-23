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
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Properties;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.openjump.util.python.JUMP_GIS_Framework;
import org.openjump.util.python.ModifyGeometry;
import org.openjump.util.python.PythonInteractiveInterpreter;
import org.python.core.PySystemState;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.JUMPException;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxDialog;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxPlugInV2;

import bsh.util.JConsole;

public class PythonToolsPlugIn extends ToolboxPlugInV2 {

  private static final ImageIcon icon = IconLoader
      .icon("famfam/application_python.png");

  public String getName() {
    return I18N.getInstance().get("org.openjump.core.ui.plugin.customize.PythonToolsPlugIn.Python-Console-and-Tools");
  }

  public void initialize(PlugInContext context) throws Exception {
    FeatureInstaller.getInstance().addMainMenuPlugin(this, new String[] { MenuNames.CUSTOMIZE });
  }

  protected ToolboxDialog initializeToolbox() throws Exception {
    WorkbenchContext context = JUMPWorkbench.getInstance().getContext();
    
    // setup the interpreter
    ClassLoader classLoader = context.getWorkbench()
        .getPlugInManager().getClassLoader();
    Properties preProperties = new Properties(System.getProperties());
    String homepath = preProperties.getProperty("user.home");

    String sep = File.separator;

    // find files via classloader which works even when lib/ext/ is not -plug-in-dir
    String startupfile, startuppath, extPath, libPath = "";

    URL res = classLoader.getResource("jython/startup.py");
    if (res == null)
      throw new JUMPException("missing jython/startup.py in classpath.");
    File file = Paths.get(res.toURI()).toFile();

    startupfile = file.getPath();
    // make sure folder string end w/ path separator as startup.py does assume they do
    startuppath = file.getParentFile().getPath() + sep;
    extPath = file.getParentFile().getParent() + sep;
    libPath = file.getParentFile().getParentFile().getParent() + sep;

    // files found? let's get ready to rumble
    ToolboxDialog toolbox = new ToolboxDialog(context);
    toolbox.setTitle(getName());
    
    final JConsole console = new JConsole();
    console.setPreferredSize(new Dimension(450, 120));
    console
        .println(new ImageIcon(getClass().getResource("jython_small_c.png")));
    toolbox.getCenterPanel().add(console, BorderLayout.CENTER);
    toolbox.setTitle("Jython");
    toolbox.setInitialLocation(new GUIUtil.Location(0, true, 0, true));
    toolbox.setResizable(true);
    
    toolbox.setIconImage(icon.getImage());

    Properties postProperties = new Properties();
    postProperties.put("python.home", homepath);
    postProperties.put("python.path", startuppath);
    PySystemState.initialize(preProperties, postProperties,
        new String[] { "" }, classLoader);

    PySystemState.add_extdir(libPath);
    PySystemState.add_extdir(extPath);
    PythonInteractiveInterpreter interpreter = new PythonInteractiveInterpreter(
        console);
    interpreter.set("wc", toolbox.getContext());
    interpreter.set("toolbox", toolbox);
    interpreter.set("startuppath", startuppath);
    interpreter.exec("import sys");
    JUMP_GIS_Framework.setWorkbenchContext(toolbox.getContext());
    ModifyGeometry.setWorkbenchContext(toolbox.getContext());
    toolbox.addToolBar(); // add a new tool bar to the console
    JMenuBar jMenuBar = new JMenuBar();
    jMenuBar.add(new JMenu(MenuNames.TOOLS));
    toolbox.setJMenuBar(jMenuBar);
    if (new File(startupfile).exists())
      interpreter.execfile(startupfile);
    new Thread(interpreter).start();
    
    return toolbox;
  }

  public static Icon getIcon(){
    return icon;
  }

  @Override
  public EnableCheck getEnableCheck() {
    EnableCheckFactory checkFactory = EnableCheckFactory.getInstance();
    // add parent's enablecheck as well to switch en/disable states properly
    return new MultiEnableCheck().add(super.getEnableCheck()).add(checkFactory
        .createTaskWindowMustBeActiveCheck());
  }
}
