/* *****************************************************************************
 The Open Java Unified Mapping Platform (OpenJUMP) is an extensible, interactive
 GUI for visualizing and manipulating spatial features with geometry and
 attributes. 

 Copyright (C) 2007  Revolution Systems Inc.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 For more information see:
 
 http://openjump.org/

 ******************************************************************************/
package org.openjump.core.ui.plugin.file;

import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.ImageIcon;

import org.openjump.core.ui.enablecheck.BooleanPropertyEnableCheck;
import org.openjump.core.ui.images.IconLoader;
import org.openjump.core.ui.plugin.AbstractWizardPlugin;
import org.openjump.core.ui.plugin.file.open.OpenFileWizard;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
 * Plug-in to open files using a wizard.
 * 
 * @author Paul Austin
 */
public class OpenFilePlugIn extends AbstractWizardPlugin {
  private static final String KEY = OpenFilePlugIn.class.getName();
  private static final ImageIcon ICON = IconLoader.icon("folder_page.png");

  private static final String FILE_DOES_NOT_EXIST = I18N.get(KEY
    + ".file-does-not-exist");

  /**
   * Construct the main Open File plug-in.
   */
  public OpenFilePlugIn() {
    super(ICON);
  }

  /**
   * Construct an Open File for the recent menu to load an individual file.
   * 
   * @param workbenchContext The workbench context.
   * @param file The file to load.
   */
  public OpenFilePlugIn(final WorkbenchContext workbenchContext, final File file) {
    super(file.getName(), ICON, file.getAbsolutePath());
    setWorkbenchContext(workbenchContext);
    File[] files = new File[] {
      file
    };
    // en/disable recent entry according to file availability
    this.enableCheck = new BooleanPropertyEnableCheck(file, "exists", true,
      FILE_DOES_NOT_EXIST + ": " + file.getAbsolutePath());
    OpenFileWizard openFileWizard = new OpenFileWizard(workbenchContext, files);
    setWizard(openFileWizard);
  }

  // for internal non gui use
  public OpenFilePlugIn(WorkbenchContext workbenchContext, File[] files) {
    setWorkbenchContext(workbenchContext);
    OpenFileWizard openFileWizard = new OpenFileWizard(workbenchContext, files);
    setWizard(openFileWizard);
  }

  /**
   * Initialise the main instance of this plug-in, should not be called for the
   * Recent menu open file plug-ins.
   * 
   * @param context The plug-in context.
   * @exception Exception If there was an error initialising the plug-in.
   */
  public void initialize(final PlugInContext context) throws Exception {
    super.initialize(context);
    FeatureInstaller featureInstaller = new FeatureInstaller(workbenchContext);

    // Add File Menu
    featureInstaller.addMainMenuPlugin(this, new String[] {MenuNames.FILE});

    // Register the Open File Wizard
    OpenFileWizard openFileWizard = new OpenFileWizard(workbenchContext);
    setWizard(openFileWizard);
    OpenWizardPlugIn.addWizard(workbenchContext, openFileWizard);
    
    // register shortcut
    AbstractPlugIn.registerShortcuts(this);
  }

}
