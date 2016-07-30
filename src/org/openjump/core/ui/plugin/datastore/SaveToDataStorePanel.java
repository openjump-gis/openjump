package org.openjump.core.ui.plugin.datastore;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.wizard.CancelNextException;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanelV2;
import org.openjump.core.ui.plugin.file.SaveWizardPlugIn;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper class to include DataStoreSaveDriverPanel into the new SaveWizardPlugIn
 */
public class SaveToDataStorePanel extends DataStoreSaveDriverPanel implements WizardPanelV2 {

  public static final String KEY = SaveToDataStorePanel.class.getName();

  private static final String LASTCONNECTION = null;

  private WorkbenchContext workbenchContext;
  private Set<InputChangedListener> listeners = new LinkedHashSet<InputChangedListener>();
  private WizardDialog dialog;

  public SaveToDataStorePanel(final WorkbenchContext workbenchContext) {
    super(workbenchContext.createPlugInContext());
    this.workbenchContext = workbenchContext;
  }

  @Override
  public void enteredFromLeft(Map dataMap) {
    // restore last folder
    Blackboard blackboard = PersistentBlackboardPlugIn.get(workbenchContext);
    Object connectionDescriptor = blackboard.get(LASTCONNECTION);
    if (connectionDescriptor != null) {
      setConnectionDescriptor((ConnectionDescriptor)connectionDescriptor);
    }

//
    //// reset selection
    //setData(SaveFileWizard.DATAKEY_DATASOURCEQUERYCHOOSER, null);
    //setData(SaveFileWizard.DATAKEY_FILE, null);
    //setSelectedFile(new File(""));
//
    //// preset selected layer name, if set
    //String dataSetName = (String) getData(SaveFileWizard.DATAKEY_LAYERNAME);
    //if (dataSetName != null && !dataSetName.isEmpty()) {
    //  setSelectedFile(new File(dataSetName));
    //}
  }

  @Override
  public void exitingToRight() throws Exception {
    if (!isInputValid())
      throw new CancelNextException();

    //File file = (File) getData(SaveFileWizard.DATAKEY_FILE);
    //// file overwriting is only checked when the selection is finally approved
    //if (file.exists()) {
    //  boolean overwrite = GUIUtil.showConfirmOverwriteDialog(getDialog(), file);
    //  if (!overwrite)
    //    throw new CancelNextException();
    //}
    //// save last folder visited
    //Blackboard blackboard = PersistentBlackboardPlugIn.get(workbenchContext);
    //blackboard.put(LASTFILE, file.getPath());
  }

  @Override
  public void add(InputChangedListener listener) {
    listeners.add(listener);
  }

  @Override
  public void remove(InputChangedListener listener) {
    listeners.remove(listener);
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
    setData(WritableDataStoreDataSource.CONNECTION_DESCRIPTOR_KEY, null);
    setData(WritableDataStoreDataSource.DATASET_NAME_KEY, null);
    setData(WritableDataStoreDataSource.GEOM_DIM_KEY, null);
    setData(WritableDataStoreDataSource.NAN_Z_TO_VALUE_KEY, null);
    setData(WritableDataStoreDataSource.CREATE_PK, null);
    //setData(WritableDataStoreDataSource.DATAKEY_NORMALIZE_TABLE_NAME, null);
    setData(WritableDataStoreDataSource.NORMALIZED_COLUMN_NAMES, null);
    //setData(SaveToDataStoreWizard.DATAKEY_DATASOURCEQUERYCHOOSER, null);
    //setData(SaveToDataStoreWizard.DATAKEY_FILE, null);

    // [2016.07 ede] the following runs the filechoosers own routine to fill up
    // the internal selected files vars properly
    //JFileChooser jfc = this;
    //FileChooserUI fcui = jfc.getUI();
    //if (!isInputValidApproveRun && fcui instanceof BasicFileChooserUI) {
    //  BasicFileChooserUI bfcui = (BasicFileChooserUI) fcui;
    //  // we insist on some filename in the textfield
    //  String filename = bfcui.getFileName();
    //  if (!(filename instanceof String) || filename.length() < 1)
    //    return false;
//
    //  isInputValidApproveRun = true;
    //  bfcui.getApproveSelectionAction().actionPerformed(
    //          new ActionEvent(new JButton(), 0, "nix"));
    //  isInputValidApproveRun = false;
    //}
//
    //// no file selected
    //File file = jfc.getSelectedFile();
    //if (!(file instanceof File) || file.getName().isEmpty())
    //  return false;
//
    //FileFilter filter = getFileFilter();
    //// no valid filter selected
    //if (!(filter instanceof FileDataSourceQueryChooserExtensionFilter))
    //  return false;
    //FileDataSourceQueryChooserExtensionFilter datasourcefilter = (FileDataSourceQueryChooserExtensionFilter) filter;
    //String[] extensions = ((FileDataSourceQueryChooserExtensionFilter) filter)
    //        .getExtensions();
    //if (extensions.length > 0) {
    //  // only treat files w/ missing extension here
    //  if (!file.isDirectory() && !hasValidExtension(file, extensions)) {
    //    file = new File(file.getPath() + "." + extensions[0]);
    //  }
    //}
//
    //if (file.isDirectory() || file.getName().isEmpty())
    //  return false;
//
    //// save successful selection
    //setData(SaveFileWizard.DATAKEY_DATASOURCEQUERYCHOOSER,
    //        datasourcefilter.getFileDataSourceQueryChooser());
    //setData(SaveFileWizard.DATAKEY_FILE, file);

    setData(WritableDataStoreDataSource.CONNECTION_DESCRIPTOR_KEY, getConnectionDescriptor());
    setData(WritableDataStoreDataSource.DATASET_NAME_KEY, getData(SaveWizardPlugIn.DATAKEY_SIMPLIFIED_LAYERNAME));
    setData(WritableDataStoreDataSource.GEOM_DIM_KEY, writeCreate3dGeometriesSelected()?3:2);
    setData(WritableDataStoreDataSource.NAN_Z_TO_VALUE_KEY, nanZToValue());
    setData(WritableDataStoreDataSource.CREATE_PK, isCreatePrimaryKeyColumnSelected());
    //setData(WritableDataStoreDataSource.DATAKEY_NORMALIZE_TABLE_NAME, isNormalizedTableName());
    setData(WritableDataStoreDataSource.NORMALIZED_COLUMN_NAMES, isNormalizedColumnNames());
    return true;
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
