package org.openjump.core.ui.plugin.file.save;

import java.io.File;

import org.openjump.core.ui.io.file.DataSourceFileLayerSaver;
import org.openjump.core.ui.swing.wizard.AbstractWizardGroup;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datasource.FileDataSourceQueryChooser;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;

public class SaveFileWizard extends AbstractWizardGroup {
  /** The key for the wizard. */
  public static final String KEY = SaveFileWizard.class.getName();
  public static final String DATAKEY_FILE = KEY + ".selected-file";
  public static final String DATAKEY_DATASOURCEQUERYCHOOSER = KEY
      + ".selected-datasourcequerychooser";
  public static final String DATAKEY_FOLDER = KEY + ".last-folder";

  /** The plugin context. */
  private PlugInContext context;

  private File file;

  public SaveFileWizard(final PlugInContext context) {
    super(I18N.get(KEY), IconLoader.icon("disk_dots.png"), SelectFilePanel.KEY);
    this.context = context;
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

    DataSourceFileLayerSaver writer = new DataSourceFileLayerSaver(
        context.getWorkbenchContext(), fdsqc);

    writer.write(monitor, file.toURI(), null);
  }
}