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

public class OpenFileWizard extends AbstractWizardGroup {
  /** The key for the wizard. */
  public static final String KEY = OpenFileWizard.class.getName();

  /** The current state of the wizard. */
  private OpenFileWizardState state;

  /** The workbench context. */
  private WorkbenchContext workbenchContext;

  /**
   * Construct a new OpenFileWizard.
   * 
   * @param workbenchContext The workbench context.
   */
  public OpenFileWizard(final WorkbenchContext workbenchContext) {
    super(I18N.get(KEY), IconLoader.icon("Open.gif"), SelectFilesPanel.KEY);
    this.workbenchContext = workbenchContext;
    state = new OpenFileWizardState(workbenchContext.getErrorHandler());
    Registry registry = workbenchContext.getRegistry();
    List<FileLayerLoader> loaders = registry.getEntries(FileLayerLoader.KEY);
    for (FileLayerLoader fileLayerLoader : loaders) {
      state.addFileLoader(fileLayerLoader);
    }

    addPanel(new SelectFilesPanel(workbenchContext, state));
    addPanel(new SelectFileLoaderPanel(state));
    addPanel(new SelectFileOptionsPanel(workbenchContext, state));
  }

  /**
   * Load the files selected in the wizard.
   * @param monitor The task monitor.
   */
  public void run(WizardDialog dialog, TaskMonitor monitor) {
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
      OpenRecentPlugIn recentPlugin = OpenRecentPlugIn.get(workbenchContext);

      for (File file : openedFiles) {
        recentPlugin.addRecentFile(file);
      }
    }
  }

}
