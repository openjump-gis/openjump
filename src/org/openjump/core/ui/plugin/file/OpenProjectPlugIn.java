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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.openjump.core.ui.enablecheck.BooleanPropertyEnableCheck;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;
import org.openjump.core.ui.plugin.file.open.OpenProjectWizard;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.coordsys.CoordinateSystemRegistry;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.datasource.Connection;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.java2xml.XML2Java;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.Task;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.SaveProjectAsPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.WorkbenchContextReference;

public class OpenProjectPlugIn extends AbstractThreadedUiPlugIn {
  private static final String KEY = OpenProjectPlugIn.class.getName();

  private static final String FILE_DOES_NOT_EXIST = I18N.get(KEY
    + ".file-does-not-exist");

  private JFileChooser fileChooser;

  private Task newTask;

  private Task sourceTask;

  private File[] files;

  private File selectedFile;

  public OpenProjectPlugIn() {
    super(IconLoader.icon("Open.gif"));
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

    OpenProjectWizard wizard = new OpenProjectWizard(workbenchContext);
    OpenWizardPlugIn.addWizard(workbenchContext, wizard);
  }

  private void initFileChooser() {
    if (fileChooser == null) {
      fileChooser = GUIUtil.createJFileChooserWithExistenceChecking();
      fileChooser.setDialogTitle(I18N.get("ui.plugin.OpenProjectPlugIn.open-project"));
      fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
      fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      fileChooser.setMultiSelectionEnabled(false);
      GUIUtil.removeChoosableFileFilters(fileChooser);
      fileChooser.addChoosableFileFilter(SaveProjectAsPlugIn.JUMP_PROJECT_FILE_FILTER);
      fileChooser.addChoosableFileFilter(GUIUtil.ALL_FILES_FILTER);
      fileChooser.setFileFilter(SaveProjectAsPlugIn.JUMP_PROJECT_FILE_FILTER);
    }
  }

  public boolean execute(PlugInContext context) throws Exception {
    initFileChooser();
    reportNothingToUndoYet(context);
    if (files == null) {
      if (JFileChooser.APPROVE_OPTION != fileChooser.showOpenDialog(context.getWorkbenchFrame())) {
        return false;
      }

      open(fileChooser.getSelectedFile(), context.getWorkbenchFrame());

      return true;
    } else {
      for (File file : files) {
        open(file, context.getWorkbenchFrame());
      }
      return true;
    }
  }

  public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
    loadLayers(context, sourceTask.getLayerManager(),
      newTask.getLayerManager(),
      CoordinateSystemRegistry.instance(workbenchContext.getBlackboard()),
      monitor);
  }

  public void open(File file, WorkbenchFrame workbenchFrame) throws Exception {
    FileReader reader = new FileReader(file);

    try {
      sourceTask = (Task)new XML2Java(workbenchContext.getWorkbench()
        .getPlugInManager()
        .getClassLoader()).read(reader, Task.class);
      initializeDataSources(sourceTask, workbenchFrame.getContext());
      newTask = new Task();
      newTask.setName(GUIUtil.nameWithoutExtension(file));
      newTask.setProjectFile(file);

      workbenchFrame.addTaskFrame(newTask);

      OpenRecentPlugIn.get(workbenchContext).addRecentProject(file);
    } finally {
      reader.close();
    }
  }

  private void initializeDataSources(Task task, WorkbenchContext context) {
    for (Iterator i = task.getLayerManager().getLayers().iterator(); i.hasNext();) {
      Layer layer = (Layer)i.next();
      if (layer.getDataSourceQuery().getDataSource() instanceof WorkbenchContextReference) {
        ((WorkbenchContextReference)layer.getDataSourceQuery().getDataSource()).setWorkbenchContext(context);
      }
    }
  }

  private void loadLayers(PlugInContext context,
    LayerManager sourceLayerManager, LayerManager newLayerManager,
    CoordinateSystemRegistry registry, TaskMonitor monitor) throws Exception {
    FindFile findFile = new FindFile(context);
    boolean displayDialog = true;

    for (Iterator i = sourceLayerManager.getCategories().iterator(); i.hasNext();) {
      Category sourceLayerCategory = (Category)i.next();
      // Explicitly add categories. Can't rely on
      // LayerManager#addLayerable to add the categories, because a
      // category might not have any layers. [Jon Aquino]
      newLayerManager.addCategory(sourceLayerCategory.getName());

      // LayerManager#addLayerable adds layerables to the top. So reverse
      // the order. [Jon Aquino]
      ArrayList layerables = new ArrayList(sourceLayerCategory.getLayerables());
      Collections.reverse(layerables);

      for (Iterator j = layerables.iterator(); j.hasNext();) {
        Layerable layerable = (Layerable)j.next();
        if (monitor != null) {
          monitor.report(I18N.get("ui.plugin.OpenProjectPlugIn.loading") + " "
            + layerable.getName());
        }
        layerable.setLayerManager(newLayerManager);

        if (layerable instanceof Layer) {
          Layer layer = (Layer)layerable;
          try {
            load(layer, registry, monitor);
          } catch (FileNotFoundException ex) {
            if (displayDialog) {
              displayDialog = false;

              int response = JOptionPane.showConfirmDialog(
                context.getWorkbenchFrame(),
                I18N.get("ui.plugin.OpenProjectPlugIn.At-least-one-file-in-the-task-could-not-be-found")
                  + "\n"
                  + I18N.get("ui.plugin.OpenProjectPlugIn.Do-you-want-to-locate-it-and-continue-loading-the-task"),
                "JUMP", JOptionPane.YES_NO_OPTION);

              if (response != JOptionPane.YES_OPTION) {
                break;
              }
            }

            String fname = layer.getDataSourceQuery()
              .getDataSource()
              .getProperties()
              .get("File")
              .toString();
            String filename = findFile.getFileName(fname);
            if (filename.length() > 0) {
              // set the new source for this layer
              Map properties = layer.getDataSourceQuery()
                .getDataSource()
                .getProperties();
              properties.put(DataSource.FILE_KEY, filename);
              layer.getDataSourceQuery().getDataSource().setProperties(
                properties);
              load(layer, registry, monitor);
            } else {
              break;
            }
          }
        }

        newLayerManager.addLayerable(sourceLayerCategory.getName(), layerable);
      }
    }
  }

  public static void load(Layer layer, CoordinateSystemRegistry registry,
    TaskMonitor monitor) throws Exception {
    layer.setFeatureCollection(executeQuery(layer.getDataSourceQuery()
      .getQuery(), layer.getDataSourceQuery().getDataSource(), registry,
      monitor));
    layer.setFeatureCollectionModified(false);
  }

  private static FeatureCollection executeQuery(String query,
    DataSource dataSource, CoordinateSystemRegistry registry,
    TaskMonitor monitor) throws Exception {
    Connection connection = dataSource.getConnection();
    try {
      return dataSource.installCoordinateSystem(connection.executeQuery(query,
        monitor), registry);
    } finally {
      connection.close();
    }
  }

}
