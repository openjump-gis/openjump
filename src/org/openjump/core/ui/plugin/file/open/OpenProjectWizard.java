package org.openjump.core.ui.plugin.file.open;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.coordsys.CoordinateSystemRegistry;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.datasource.Connection;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.util.java2xml.XML2Java;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.PlugInManager;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.TaskFrame;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.WorkbenchContextReference;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import org.openjump.core.model.TaskEvent;
import org.openjump.core.model.TaskListener;
import org.openjump.core.ui.plugin.file.FindFile;
import org.openjump.core.ui.plugin.file.OpenProjectPlugIn;
import org.openjump.core.ui.plugin.file.OpenRecentPlugIn;
import org.openjump.core.ui.swing.wizard.AbstractWizardGroup;

import javax.swing.*;
import javax.xml.namespace.QName;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OpenProjectWizard extends AbstractWizardGroup {
  /** The key for the wizard. */
  public static final String KEY = OpenProjectWizard.class.getName();
  
  public static final String FILE_CHOOSER_DIRECTORY_KEY = KEY
      + " - FILE CHOOSER DIRECTORY";

  /** The workbench context. */
  private WorkbenchContext workbenchContext;

  private SelectProjectFilesPanel selectProjectPanel;

  private Task sourceTask;

  private Task newTask;

  private File[] files;

  private Envelope savedTaskEnvelope = null;

  /**
   * Construct a new OpenFileWizard.
   * 
   * @param workbenchContext The workbench context.
   */
  public OpenProjectWizard(final WorkbenchContext workbenchContext) {
    super(I18N.get(KEY), OpenProjectPlugIn.ICON, 
      SelectProjectFilesPanel.KEY);
    this.workbenchContext = workbenchContext;
    initPanels(workbenchContext);
  }

  public OpenProjectWizard(final WorkbenchContext workbenchContext,
    final File[] files) {
    this.workbenchContext = workbenchContext;
    this.files = files;
    initPanels(workbenchContext);
  }

  private void initPanels(final WorkbenchContext workbenchContext) {
    selectProjectPanel = new SelectProjectFilesPanel(workbenchContext);
    addPanel(selectProjectPanel);
  }

  public void initialize(WorkbenchContext workbenchContext, WizardDialog dialog) {
    selectProjectPanel.setDialog(dialog);
  }

  /**
   * Load the files selected in the wizard.
   * 
   * @param monitor The task monitor.
   * @throws Exception 
   */
  public void run(WizardDialog dialog, TaskMonitor monitor) throws Exception {
    // local list for internal usage OR let user select via gui
    File[] selectedFiles = (files!=null) ? files : 
                                           selectProjectPanel.getSelectedFiles();
    open(selectedFiles, monitor);
  }

  private void open(File[] files, TaskMonitor monitor) throws Exception {
    for (File file : files) {
        open(file, monitor);
    }
  }

  public void open(File file, TaskMonitor monitor) throws Exception {

      // persist last used directory in workbench-state.xml
      Blackboard blackboard = PersistentBlackboardPlugIn.get(workbenchContext);
      blackboard.put(FILE_CHOOSER_DIRECTORY_KEY, file.getAbsoluteFile().getParent());
    
      FileReader reader = new FileReader(file);
      JUMPWorkbench workbench = null;
      WorkbenchFrame workbenchFrame = null;
      try {
        workbench = workbenchContext.getWorkbench();
        workbenchFrame = workbench.getFrame();
        PlugInManager plugInManager = workbench.getPlugInManager();
        ClassLoader pluginClassLoader = plugInManager.getClassLoader();
        sourceTask = (Task)new XML2Java(pluginClassLoader).read(reader,
          Task.class);
        initializeDataSources(sourceTask, workbenchFrame.getContext());
        newTask = new Task();
        newTask.setName(GUIUtil.nameWithoutExtension(file));
        newTask.setProjectFile(file);
        newTask.setProperties(sourceTask.getProperties());
        
        newTask.setTaskWindowLocation(sourceTask.getTaskWindowLocation());
        newTask.setTaskWindowSize(sourceTask.getTaskWindowSize());
        newTask.setMaximized(sourceTask.getMaximized());
        newTask.setSavedViewEnvelope(sourceTask.getSavedViewEnvelope());

        TaskFrame frame = workbenchFrame.addTaskFrame(newTask);
        Dimension size = newTask.getTaskWindowSize();
        if (size != null)
          frame.setSize(size);
//        Point location = newTask.getTaskWindowLocation();
//        if ( (location != null)
//        		&& (location.x < workbenchFrame.getSize().width)
//        		&& (location.y < workbenchFrame.getSize().height))
//        	frame.setLocation(location);
        if (newTask.getMaximized()) frame.setMaximum(true);
        savedTaskEnvelope = newTask.getSavedViewEnvelope();

        LayerManager sourceLayerManager = sourceTask.getLayerManager();
        LayerManager newLayerManager = newTask.getLayerManager();
        CoordinateSystemRegistry crsRegistry = CoordinateSystemRegistry.instance(workbenchContext.getBlackboard());
        
        workbenchContext.getLayerViewPanel().setDeferLayerEvents(true);
        
        loadLayers(sourceLayerManager, newLayerManager, crsRegistry, monitor);

        workbenchContext.getLayerViewPanel().setDeferLayerEvents(false);

        OpenRecentPlugIn.get(workbenchContext).addRecentProject(file);

      }
      catch (ClassNotFoundException e) {
          workbenchFrame.log(file.getPath() + " can not be loaded");
          workbenchFrame.warnUser("Missing class: " + e.getCause());
      }
      catch (Exception cause) {
        Exception e = new Exception(I18N.getMessage(KEY
            + ".could-not-open-project-file-{0}-with-error-{1}", new Object[] {
            file, cause.getLocalizedMessage() }), cause);
        monitor.report(e);
        throw e;
      }
      finally {
        reader.close();
      }
  }

  private void initializeDataSources(Task task, WorkbenchContext context) throws Exception {
    LayerManager layerManager = task.getLayerManager();
    List<Layer> layers = layerManager.getLayers();
    List<Layer> layersToBeRemoved = new ArrayList<Layer>();
    for (Layer layer : layers) {
      DataSourceQuery dataSourceQuery = layer.getDataSourceQuery();
      DataSource dataSource = dataSourceQuery.getDataSource();
      if (dataSource == null) {
        context.getWorkbench().getFrame().warnUser(I18N.getMessage(KEY + ".datasource-not-found",
                new Object[]{layer.getName()}));
        //context.getWorkbench().getFrame().warnUser("DataSource not found for " + layer.getName());
        layerManager.remove(layer);
        continue;
      }
      if (dataSource instanceof WorkbenchContextReference) {
          try {
              WorkbenchContextReference workbenchRef = (WorkbenchContextReference)dataSource;
              workbenchRef.setWorkbenchContext(context);
          } catch (Exception e) {
              int response = JOptionPane.showConfirmDialog(
                    workbenchContext.getWorkbench().getFrame(),
                    "<html>" +
                	    I18N.getMessage(KEY + ".opening-datasource-{0}-failed-with-error", 
                	        new Object[] {/*layer.getDataSourceQuery().toString()*/layer.getName()}) + "<br>" + 
                	    StringUtil.split(e.getLocalizedMessage(), 80).replaceAll("\n","<br>") + "<br>" +
                        I18N.get(KEY + ".click-yes-to-continue-or-no-to-remove-layer") + 
                    "</html>", 
                    "OpenJUMP", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
	
	    	  if (response != JOptionPane.YES_OPTION) {
	    	      layersToBeRemoved.add(layer);
	    	  }
              else {
                  continue;
              }
          }
      }
    }
    for (Layer layer : layersToBeRemoved) layerManager.remove(layer);
  }

  private void loadLayers(LayerManager sourceLayerManager,
    LayerManager newLayerManager, CoordinateSystemRegistry registry,
    TaskMonitor monitor) throws Exception {
    JUMPWorkbench workbench = workbenchContext.getWorkbench();
    WorkbenchFrame workbenchFrame = workbench.getFrame();
    FindFile findFile = new FindFile(workbenchFrame);
    boolean displayDialog = true;
    
    String oldProjectPath = sourceTask.getProperty(new QName(Task.PROJECT_FILE_KEY));
    boolean updateResources = false;
    boolean updateOnlyMissingResources = false;
    File oldProjectFile = null;
    if(oldProjectPath != null && !oldProjectPath.equals("")){
        oldProjectFile = new File(oldProjectPath);
        if(!oldProjectFile.equals(newTask.getProjectFile())){
    		JCheckBox checkbox = new JCheckBox(I18N.get("ui.plugin.OpenProjectPlugIn.Only-for-missing-resources"));  
            String message = I18N.get("ui.plugin.OpenProjectPlugIn."
                    + "The-project-has-been-moved-Do-you-want-to-update-paths-below-the-project-folder");  
            Object[] params = {message, checkbox};  
            int answer = JOptionPane.showConfirmDialog(workbenchFrame, 
                    params, "OpenJUMP", JOptionPane.YES_NO_OPTION);
            if(answer == JOptionPane.YES_OPTION){
                updateResources = true;
                if(checkbox.isSelected())
                    updateOnlyMissingResources = true;
            }            
        }
    }

    try {				
    List<Category> categories = sourceLayerManager.getCategories();
    for (Category sourceLayerCategory : categories) {
      newLayerManager.addCategory(sourceLayerCategory.getName());

      // LayerManager#addLayerable adds layerables to the top. So reverse
      // the order.
      ArrayList<Layerable> layerables = new ArrayList<Layerable>(
        sourceLayerCategory.getLayerables());
      Collections.reverse(layerables);

      for (Layerable layerable : layerables) {
        if (monitor != null) {
          monitor.report(I18N.get("ui.plugin.OpenProjectPlugIn.loading") + " "
            + layerable.getName());
        }
        layerable.setLayerManager(newLayerManager);

        if (layerable instanceof Layer) {
          Layer layer = (Layer)layerable;
          File layerFile = getLayerFileProperty(layer);
          if(!updateOnlyMissingResources || !layerFile.exists()){
              if(updateResources && layerFile != null && isLocatedBellow(oldProjectFile.getParentFile(), layerFile)) {
                  File newLayerFile = updateResourcePath(oldProjectFile, newTask.getProjectFile(), layerFile);
                  setLayerFileProperty(layer, newLayerFile);
              }
          }
          try {
            load(layer, registry, monitor);
          } catch (FileNotFoundException ex) {
            if (displayDialog) {
              displayDialog = false;

              int response = JOptionPane.showConfirmDialog(
                workbenchFrame,
                I18N.get("ui.plugin.OpenProjectPlugIn.At-least-one-file-in-the-task-could-not-be-found")
                  + "\n"
                  + I18N.get("ui.plugin.OpenProjectPlugIn.Do-you-want-to-locate-it-and-continue-loading-the-task"),
                "OpenJUMP", JOptionPane.YES_NO_OPTION);

              if (response != JOptionPane.YES_OPTION) {
                break;
              }
            }

            DataSourceQuery dataSourceQuery = layer.getDataSourceQuery();
            DataSource dataSource = dataSourceQuery.getDataSource();
            Map properties = dataSource.getProperties();
            String fname = properties.get(DataSource.FILE_KEY).toString();
            String filename = findFile.getFileName(fname);
            if (filename.length() > 0) {
              // set the new source for this layer
              properties.put(DataSource.FILE_KEY, filename);
              dataSource.setProperties(properties);
              load(layer, registry, monitor);
            } else {
              break;
            }
          }
        }

        newLayerManager.addLayerable(sourceLayerCategory.getName(), layerable);
      }
    }
	// fire TaskListener's
	  Object[] listeners =  workbenchFrame.getTaskListeners().toArray();
	  for (int i = 0; i < listeners.length; i++) {
		  TaskListener l = (TaskListener) listeners[i];
		  l.taskLoaded(new TaskEvent(this, newLayerManager.getTask()));
	  }
	} finally {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					if (savedTaskEnvelope == null)
						workbenchContext.getLayerViewPanel().getViewport().zoomToFullExtent();
					else
						workbenchContext.getLayerViewPanel().getViewport().zoom(savedTaskEnvelope);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
	}
  }

  public static void load(Layer layer, CoordinateSystemRegistry registry,
    TaskMonitor monitor) throws Exception {
    DataSourceQuery dataSourceQuery = layer.getDataSourceQuery();
    String query = dataSourceQuery.getQuery();
    DataSource dataSource = dataSourceQuery.getDataSource();
    FeatureCollection features = executeQuery(query, dataSource, registry,
      monitor);
    layer.setFeatureCollection(features);
    layer.setFeatureCollectionModified(false);
  }

  private static FeatureCollection executeQuery(String query,
    DataSource dataSource, CoordinateSystemRegistry registry,
    TaskMonitor monitor) throws Exception {
    Connection connection = dataSource.getConnection();
    try {
      FeatureCollection features = connection.executeQuery(query, monitor);
      return dataSource.installCoordinateSystem(features, registry);
    } finally {
      connection.close();
    }
  }
  
  
  private File updateResourcePath(File oldProjectFile, File newProjectFile, File layerFile) {
      String oldParent = oldProjectFile.getParentFile().getAbsolutePath();
      String newParent = newProjectFile.getParentFile().getAbsolutePath();
      String child = layerFile.getAbsolutePath();
      String relativePath = child.substring(oldParent.length()+1);
         return new File(newParent, relativePath);
   }

   private boolean isLocatedBellow(File parentDir, File layerFile) {
      if(layerFile == null)
          return false;
      for (File layerParent = layerFile.getParentFile(); layerParent != null; layerParent = layerParent.getParentFile()) {
        if(layerParent.equals(parentDir))
          return true;
      }
      
      return false;
  }

    private File getLayerFileProperty(Layer layer) {
      DataSourceQuery dataSourceQuery = layer.getDataSourceQuery();
      DataSource dataSource = dataSourceQuery.getDataSource();
      Map properties = dataSource.getProperties();
      Object property = properties.get(DataSource.FILE_KEY);
      if(property == null || property.toString().equals(""))
          return null;
      File layerFile = new File(property.toString());
      return layerFile;
   }

   private void setLayerFileProperty(Layer layer, File file) {
      DataSourceQuery dataSourceQuery = layer.getDataSourceQuery();
      DataSource dataSource = dataSourceQuery.getDataSource();
      Map properties = dataSource.getProperties();
      properties.put(DataSource.FILE_KEY, file.getAbsolutePath());
   }
  


}
