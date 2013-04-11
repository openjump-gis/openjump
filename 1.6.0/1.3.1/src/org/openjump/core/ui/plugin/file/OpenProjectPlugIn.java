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

import java.io.File;

import org.openjump.core.ui.enablecheck.BooleanPropertyEnableCheck;
import org.openjump.core.ui.images.IconLoader;
import org.openjump.core.ui.plugin.AbstractWizardPlugin;
import org.openjump.core.ui.plugin.file.open.OpenProjectWizard;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

public class OpenProjectPlugIn extends AbstractWizardPlugin {
  private static final String KEY = OpenProjectPlugIn.class.getName();

  private static final String FILE_DOES_NOT_EXIST = I18N.get(KEY
    + ".file-does-not-exist");

  private File[] files;

  private OpenProjectWizard wizard;

  public OpenProjectPlugIn() {
    super(IconLoader.icon("folder_layout_add.png"));
  }

  public OpenProjectPlugIn(WorkbenchContext workbenchContext, File file) {
    super(file.getName(), file.getAbsolutePath());
    this.workbenchContext = workbenchContext;
    this.files = new File[] {
      file
    };
    this.enableCheck = new BooleanPropertyEnableCheck(file, "exists", true,
      FILE_DOES_NOT_EXIST + ": " + file.getAbsolutePath());
  }

  public OpenProjectPlugIn(WorkbenchContext workbenchContext, File[] files) {
    this.workbenchContext = workbenchContext;
    this.files = files;
  }

  public void initialize(PlugInContext context) throws Exception {
    super.initialize(context);
    FeatureInstaller featureInstaller = context.getFeatureInstaller();

    // Add File Menu
    featureInstaller.addMainMenuItem(new String[] {
      MenuNames.FILE
    }, this, 3);

    wizard = new OpenProjectWizard(workbenchContext);
    setWizard(wizard);
    OpenWizardPlugIn.addWizard(workbenchContext, wizard);
  }

  @Override
  public boolean execute(PlugInContext context) throws Exception {
    if (wizard == null) {
      wizard = new OpenProjectWizard(workbenchContext, files);
      setWizard(wizard);
    }
    // TODO Auto-generated method stub
    return super.execute(context);
  }
}
