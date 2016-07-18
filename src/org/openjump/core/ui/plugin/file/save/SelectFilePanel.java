package org.openjump.core.ui.plugin.file.save;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.FileChooserUI;
import javax.swing.plaf.basic.BasicFileChooserUI;

import org.openjump.core.ui.plugin.file.open.JFCWithEnterAction;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datasource.DataSourceQueryChooserManager;
import com.vividsolutions.jump.workbench.datasource.FileDataSourceQueryChooser;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.wizard.CancelNextException;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanelV2;

public class SelectFilePanel extends JFCWithEnterAction implements
    WizardPanelV2 {

  public static final String KEY = SelectFilePanel.class.getName();

  private static final String LASTFILE = null;

  private WorkbenchContext workbenchContext;
  private Set<InputChangedListener> listeners = new LinkedHashSet<InputChangedListener>();
  private WizardDialog dialog;

  private boolean isInputValidApproveRun = false;
  private boolean isButtonRefresh = false;

  public SelectFilePanel(final WorkbenchContext workbenchContext) {
    super();
    this.workbenchContext = workbenchContext;

    setControlButtonsAreShown(false);
    setMultiSelectionEnabled(false);
    setAcceptAllFileFilterUsed(false);

    // add file datasources
    List loadChoosers = DataSourceQueryChooserManager.get(
        workbenchContext.getBlackboard()).getSaveDataSourceQueryChoosers();
    for (Object chooser : loadChoosers) {
      if (chooser instanceof FileDataSourceQueryChooser) {
        FileFilter filter = new FileDataSourceQueryChooserExtensionFilter(
            (FileDataSourceQueryChooser) chooser);
        addChoosableFileFilter(filter);
      }
    }

    PropertyChangeListener changeListener = new PropertyChangeListener() {

      // user selected something in the fc
      public void propertyChange(PropertyChangeEvent evt) {
        // only listen to fc changes while we're on _this_ panel
        // OSX fc weirdly fires events even when we're on the next panel
        if (dialog == null
            || dialog.getData(WizardDialog.DATAKEY_CURRENTPANELID) != KEY) {
          return;
        }

        isButtonRefresh = true;
        fireInputChanged();
        isButtonRefresh = false;
      }
    };
    addPropertyChangeListener(changeListener);

    ActionListener actionListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // we ignore the isInputValid() call, as it is only
        // meant to fixup the selectedfile value for us
        if (isInputValidApproveRun)
          return;

        if (dialog != null)
          dialog.next();
      }
    };
    addActionListener(actionListener);

  }

  @Override
  public void enteredFromLeft(Map dataMap) {
    // restore last folder
    Blackboard blackboard = PersistentBlackboardPlugIn.get(workbenchContext);
    String lastFilePath = (String)blackboard.get(LASTFILE);
    if (lastFilePath != null)
      setCurrentDirectory(new File(lastFilePath).getParentFile());
    // update file view
    rescanCurrentDirectory();
    // reset selection
    setData(SaveFileWizard.DATAKEY_DATASOURCEQUERYCHOOSER, null);
    setData(SaveFileWizard.DATAKEY_FILE, null);
  }

  @Override
  public void exitingToRight() throws Exception {
    if (!isInputValid())
      throw new CancelNextException();
    // save last folder visited
    Blackboard blackboard = PersistentBlackboardPlugIn.get(workbenchContext);
    blackboard.put(LASTFILE, ((File)getData(SaveFileWizard.DATAKEY_FILE)).getPath());
  }

  public void add(InputChangedListener listener) {
    listeners.add(listener);
  }

  public void remove(InputChangedListener listener) {
    listeners.remove(listener);
  }

  private void fireInputChanged() {
    for (InputChangedListener listener : listeners) {
      listener.inputChanged();
    }
  }

  @Override
  public String getTitle() {
    return I18N.get(KEY + ".title");
  }

  @Override
  public String getID() {
    return KEY;
  }

  @Override
  public String getInstructions() {
    return I18N.get(KEY + ".instructions");
  }

  @Override
  public boolean isInputValid() {
    // reset selection
    setData(SaveFileWizard.DATAKEY_DATASOURCEQUERYCHOOSER, null);
    setData(SaveFileWizard.DATAKEY_FILE, null);

    // [2016.07 ede] the following runs the filechoosers own routine to fill up
    // the internal selected files vars properly
    JFileChooser jfc = this;
    FileChooserUI fcui = jfc.getUI();
    if (fcui instanceof BasicFileChooserUI) {
      BasicFileChooserUI bfcui = (BasicFileChooserUI) fcui;
      isInputValidApproveRun = true;
      bfcui.getApproveSelectionAction().actionPerformed(
          new ActionEvent(new JButton(), 0, "nix"));
      isInputValidApproveRun = false;
    }

    // no file selected
    File file = jfc.getSelectedFile();
    if (!(file instanceof File) || file.getName().isEmpty())
      return false;

    FileDataSourceQueryChooserExtensionFilter filter = (FileDataSourceQueryChooserExtensionFilter) getFileFilter();
    String[] extensions = filter.getExtensions();
    if (extensions.length > 0) {
      // only treat files w/ missing extension here
      if (!file.isDirectory() && !hasValidExtension(file, extensions)) {
        file = new File(file.getPath() + "." + extensions[0]);
      }
    }

    if (file.isDirectory() || file.getName().isEmpty())
      return false;

    // file overwriting is only checked when the selection is finally approved
    if (!isButtonRefresh && file.exists()) {
      boolean overwrite = GUIUtil.showConfirmOverwriteDialog(getDialog(), file);
      if (!overwrite)
        return false;
    }

    // save successful selection
    setData(SaveFileWizard.DATAKEY_DATASOURCEQUERYCHOOSER,
        filter.getFileDataSourceQueryChooser());
    setData(SaveFileWizard.DATAKEY_FILE, file);
    return true;
  }

  private boolean hasValidExtension(File file, String[] validExtensions) {
    String fileName = file.getName();
    for (String validExt : validExtensions) {
      if (fileName.toLowerCase().endsWith("." + validExt.toLowerCase())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String getNextID() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void enteredFromRight() throws Exception {
    // TODO Auto-generated method stub
  }

  public WizardDialog getDialog() {
    return dialog;
  }

  public void setDialog(WizardDialog dialog) {
    this.dialog = dialog;
  }

  private void setData(String key, Object value) {
    if (dialog != null)
      dialog.setData(key, value);
  }

  private Object getData(String key) {
    if (dialog != null)
      return dialog.getData(key);
    return null;
  }
}
