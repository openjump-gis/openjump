package org.openjump.core.ui.plugin.file.save;

import java.io.File;
import java.util.Collection;

import org.openjump.core.ui.io.file.DataSourceFileLayerSaver;
import org.openjump.core.ui.plugin.file.SaveWizardPlugIn;
import org.openjump.core.ui.plugin.file.open.OpenFileWizard;
import org.openjump.core.ui.swing.wizard.AbstractWizardGroup;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.JUMPException;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datasource.FileDataSourceQueryChooser;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;

public class SaveToFileWizard extends AbstractWizardGroup {
  /** The key for the wizard. */
  public static final String KEY = SaveToFileWizard.class.getName();
  public static final String DATAKEY_FILE = KEY + ".selected-file";
  public static final String DATAKEY_DATASOURCEQUERYCHOOSER = KEY
      + ".selected-datasourcequerychooser";
  public static final String DATAKEY_FOLDER = KEY + ".last-folder";
  //public static final String DATAKEY_LAYERNAME = KEY + ".layername";

  /** The plugin context. */
  private PlugInContext context;

  private File file;

  public SaveToFileWizard(final PlugInContext context) {
    super(I18N.get(OpenFileWizard.KEY), IconLoader.icon("disk_dots.png"), SelectFilePanel.KEY);
//    this.context = context;
  }

  @Override
  public void initialize(WorkbenchContext workbenchContext, WizardDialog dialog) {
//    //for debugging
//    removeAllPanels();
    // already initialized
    if (!getPanels().isEmpty())
      return;

    SelectFilePanel selectFilePanel = new SelectFilePanel(workbenchContext);
    selectFilePanel.setDialog(dialog);
    addPanel(selectFilePanel);
  }

  
  @Override
  public void run(WizardDialog dialog, TaskMonitor monitor) throws Exception {
    // retrieve selected file
    File file = (File) dialog.getData(DATAKEY_FILE);
    // retrieve selected file loader
    FileDataSourceQueryChooser fdsqc = (FileDataSourceQueryChooser) dialog
        .getData(DATAKEY_DATASOURCEQUERYCHOOSER);
    // retrieve selected layer
    Collection<Layerable> layers = (Collection<Layerable>) dialog.getData(SaveWizardPlugIn.DATAKEY_SELECTED_LAYERABLES);
    if (layers == null || layers.isEmpty())
      throw new JUMPException("no layers selected");
    
    Layerable layerable = layers.iterator().next();
    if (!(layerable instanceof Layer))
      throw new JUMPException("selected layerable is not of type layer");

    Layer layer = (Layer)layerable;
    DataSourceFileLayerSaver writer = new DataSourceFileLayerSaver(
        layer, fdsqc);

    writer.write(monitor, file.toURI(), null);
  }
}