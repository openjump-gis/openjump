package org.openjump.core.ui.plugin.file.open;

import java.io.File;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.openjump.core.ui.io.file.FileLayerLoader;
import org.openjump.core.ui.plugin.file.OpenRecentPlugIn;
import org.openjump.core.ui.swing.wizard.AbstractWizardGroup;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.registry.Registry;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

public class OpenFileWizard extends AbstractWizardGroup {
  /** The key for the wizard. */
  public static final String KEY = OpenFileWizard.class.getName();

  /** The current state of the wizard. */
  private OpenFileWizardState state;

  /** The workbench context. */
  private WorkbenchContext workbenchContext;

  private File[] files;

  private ChooseProjectPanel chooseProjectPanel;

  private SelectFilesPanel selectFilesPanel;

  private SelectFileLoaderPanel selectFileLoaderPanel;

  private SelectFileOptionsPanel selectFileOptionsPanel;

  /**
   * Construct a new OpenFileWizard.
   * 
   * @param workbenchContext The workbench context.
   */
  public OpenFileWizard(final WorkbenchContext workbenchContext) {
    super(I18N.get(KEY), IconLoader.icon("Open.gif"), SelectFilesPanel.KEY);
    initPanels(workbenchContext);
  }

  public OpenFileWizard(final WorkbenchContext workbenchContext,
    final File[] files) {
    this.files = files;
  }

  public void initialize(final WorkbenchContext workbenchContext,
    WizardDialog dialog) {
    this.workbenchContext = workbenchContext;
    initPanels(workbenchContext);
    state = new OpenFileWizardState(workbenchContext.getErrorHandler());
    Registry registry = workbenchContext.getRegistry();
    List<FileLayerLoader> loaders = registry.getEntries(FileLayerLoader.KEY);
    for (FileLayerLoader fileLayerLoader : loaders) {
      state.addFileLoader(fileLayerLoader);
    }
    if (selectFilesPanel != null) {
      selectFilesPanel.setState(state);
      selectFilesPanel.setDialog(dialog);
    }
    selectFileLoaderPanel.setState(state);
    selectFileOptionsPanel.setState(state);
    if (files != null) {
      state.setupFileLoaders(files, null);
    }
  }

  private void initPanels(final WorkbenchContext workbenchContext) {
    if (selectFileLoaderPanel == null) {
      if (files == null) {
        chooseProjectPanel = new ChooseProjectPanel(workbenchContext,
          SelectFilesPanel.KEY);
        addPanel(chooseProjectPanel);
        selectFilesPanel = new SelectFilesPanel(workbenchContext);
        addPanel(selectFilesPanel);
      } else {
        chooseProjectPanel = new ChooseProjectPanel(workbenchContext,
          SelectFileLoaderPanel.KEY);
        addPanel(chooseProjectPanel);
      }
      selectFileLoaderPanel = new SelectFileLoaderPanel();
      addPanel(selectFileLoaderPanel);
      selectFileOptionsPanel = new SelectFileOptionsPanel(workbenchContext);
      addPanel(selectFileOptionsPanel);
    }
  }

  public String getFirstId() {
    String firstId;
    if (files != null) {
      firstId = state.getNextPanel(SelectFilesPanel.KEY);
    } else {
      firstId = SelectFilesPanel.KEY;
    }
    if (!chooseProjectPanel.hasActiveTaskFrame()
      && chooseProjectPanel.hasTaskFrames()) {
      chooseProjectPanel.setNextID(firstId);
      return chooseProjectPanel.getID();
    } else {
      return firstId;
    }
  }
 
  /**
   * Load the files selected in the wizard.
   * 
   * @param monitor The task monitor.
   */
  public void run(WizardDialog dialog, TaskMonitor monitor) {
    chooseProjectPanel.activateSelectedProject();
    Set<File> openedFiles = new LinkedHashSet<File>();
    try {
      monitor.allowCancellationRequests();
      Map<URI, FileLayerLoader> fileLoaders = state.getFileLoaders();
      for (Entry<URI, FileLayerLoader> entry : fileLoaders.entrySet()) {
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
      OpenRecentPlugIn recentPlugin = OpenRecentPlugIn.get(workbenchContext);

      for (File file : openedFiles) {
        recentPlugin.addRecentFile(file);
      }
    }
  }

}
