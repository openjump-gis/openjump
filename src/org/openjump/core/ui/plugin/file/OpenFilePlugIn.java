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
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.Icon;

import org.openjump.core.ui.enablecheck.BooleanPropertyEnableCheck;
import org.openjump.core.ui.io.file.FileLayerLoader;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;
import org.openjump.core.ui.plugin.file.open.OpenFileWizard;
import org.openjump.core.ui.plugin.file.open.OpenFileWizardState;
import org.openjump.core.ui.plugin.file.open.SelectFileLoaderPanel;
import org.openjump.core.ui.plugin.file.open.SelectFileOptionsPanel;
import org.openjump.core.ui.plugin.file.open.SelectFilesPanel;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.registry.Registry;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.WorkbenchToolBar;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

/**
 * Plug-in to open files using a wizard.
 * 
 * @author Paul Austin
 */
public class OpenFilePlugIn extends AbstractThreadedUiPlugIn {
  private static final String KEY = OpenFilePlugIn.class.getName();

  private static final String FILE_DOES_NOT_EXIST = I18N.get(KEY
    + ".file-does-not-exist");

  /** The registry for the workbench. */
  private Registry registry;

  /** The current state of the open file wizard. */
  private OpenFileWizardState state;

  /** The file to load if this is an Open recent plug-in. */
  private File[] files;

  /** The open recent plug-in to save loaded files to. */
  private OpenRecentPlugIn recentPlugin;

  /**
   * Construct the main Open File plug-in.
   */
  public OpenFilePlugIn() {
    super(IconLoader.icon("Open.gif"));
  }

  /**
   * Construct an Open File for the recent menu to load an individual file.
   * 
   * @param workbenchContext The workbench context.
   * @param file The file to load.
   */
  public OpenFilePlugIn(final WorkbenchContext workbenchContext, final File file) {
    super(file.getName(), file.getAbsolutePath());
    this.files = new File[] {
      file
    };
    setWorkbenchContext(workbenchContext);
    MultiEnableCheck enableCheck = new MultiEnableCheck();
    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
    enableCheck.add(checkFactory.createWindowWithLayerManagerMustBeActiveCheck());
    enableCheck.add(new BooleanPropertyEnableCheck(file, "exists", true,
      FILE_DOES_NOT_EXIST +": " + file.getAbsolutePath()));
    this.enableCheck = enableCheck;
  }

  public OpenFilePlugIn(WorkbenchContext workbenchContext, File[] files) {
    this(workbenchContext, files[0]);
  }

  /**
   * Initialise the main instance of this plug-in, should not be called for the
   * Recent menu open file plug-ins.
   * 
   * @param context The plug-in context.
   * @exception Exception If there was an error initialising the plug-in.
   */
  public void initialize(final PlugInContext context) throws Exception {
    if (files == null && workbenchContext == null) {
      setWorkbenchContext(context.getWorkbenchContext());
      EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
      this.enableCheck = checkFactory.createWindowWithLayerManagerMustBeActiveCheck();
      FeatureInstaller featureInstaller = new FeatureInstaller(workbenchContext);

      // Add File Menu
      featureInstaller.addMainMenuItem(new String[] {
        MenuNames.FILE
      }, this, 2);
      
      // Register the Open File Wizard
      OpenFileWizard openFileWizard = new OpenFileWizard(workbenchContext);
      OpenWizardPlugIn.addWizard(workbenchContext, openFileWizard);
    }
  }

  /**
   * Execute the plug-in displaying the OpenFile wizard.
   * 
   * @param The plug-in context.
   * @exception Exception If there was an error executing the plug-in.
   */
  public boolean execute(PlugInContext context) throws Exception {
    reportNothingToUndoYet(context);
    LayerManager layerManager = context.getLayerManager();
    if (layerManager == null) {
      // TODO Add a dialog to select a current project or create a new one
    }

    WizardDialog dialog = new WizardDialog(context.getWorkbenchFrame(),
      getName(), context.getErrorHandler());
    OpenFileWizardState state = new OpenFileWizardState(
      context.getErrorHandler());
    List<FileLayerLoader> loaders = registry.getEntries(FileLayerLoader.KEY);
    for (FileLayerLoader fileLayerLoader : loaders) {
      state.addFileLoader(fileLayerLoader);
    }
    dialog.setData(OpenFileWizardState.KEY, state);
    SelectFilesPanel filesPanel = new SelectFilesPanel(workbenchContext, state);
    SelectFileLoaderPanel loaderPanel = new SelectFileLoaderPanel(state);
    SelectFileOptionsPanel optionsPanel = new SelectFileOptionsPanel(
      workbenchContext, state);
    WizardPanel[] wizardPanels = null;
    if (files != null) {
      state.setupFileLoaders(files, null);
      String nextPanel = state.getNextPanel(SelectFilesPanel.KEY);
      if (nextPanel == SelectFileLoaderPanel.KEY) {
        wizardPanels = new WizardPanel[] {
          loaderPanel, optionsPanel
        };
      } else if (nextPanel == SelectFileOptionsPanel.KEY) {
        wizardPanels = new WizardPanel[] {
          optionsPanel
        };
      } else {
        this.state = state;
        return true;
      }
    } else {
      wizardPanels = new WizardPanel[] {
        filesPanel, loaderPanel, optionsPanel
      };
    }
    dialog.init(wizardPanels);

    dialog.setSize(700, 580);
    dialog.setVisible(true);
    if (dialog.wasFinishPressed()) {
      this.state = state;
      return true;
    }
    return false;
  }

  /**
   * Load the selected file or files.
   * 
   * @param monitor The task monitor.
   * @param context The plug-in context.
   */
  public void run(final TaskMonitor monitor, final PlugInContext context)
    throws Exception {
    Set<File> openedFiles = new LinkedHashSet<File>();
    try {
      monitor.allowCancellationRequests();
      for (Entry<URI, FileLayerLoader> entry : state.getFileLoaders()
        .entrySet()) {
        URI uri = entry.getKey();
        FileLayerLoader loader = entry.getValue();
        Map<String, Object> options = state.getOptions(uri);
        try {
          if (loader.open(monitor, uri, options)) {
            if (uri.getScheme().equals("zip")) {
              openedFiles.add(org.openjump.util.UriUtil.getZipFile(uri));
            } else {
              openedFiles.add(new File(uri));
            }
          }
        } catch (Exception e) {
          monitor.report(e);
        }
      }
    } finally {
      state = null;
      for (File file : openedFiles) {
        recentPlugin.addRecentFile(file);
      }
    }
  }

  /**
   * Set the workbench context and related attributes.
   * 
   * @param workbenchContext The workbench context.
   */
  private void setWorkbenchContext(WorkbenchContext workbenchContext) {
    this.workbenchContext = workbenchContext;
    registry = workbenchContext.getRegistry();
    recentPlugin = OpenRecentPlugIn.get(workbenchContext);
  }
}
