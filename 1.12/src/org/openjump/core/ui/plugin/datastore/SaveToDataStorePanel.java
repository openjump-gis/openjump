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

  private static final String LASTCONNECTION = SaveToDataStorePanel.class.getName()
          + " - LAST CONNECTION";

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
    setTableName(getData(SaveWizardPlugIn.DATAKEY_SIMPLIFIED_LAYERNAME).toString());
  }

  @Override
  public void exitingToRight() throws Exception {
    if (!isInputValid())
      throw new CancelNextException();

    Blackboard blackboard = PersistentBlackboardPlugIn.get(workbenchContext);
    blackboard.put(LASTCONNECTION, WritableDataStoreDataSource.CONNECTION_DESCRIPTOR_KEY);
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
    setData(WritableDataStoreDataSource.NARROW_GEOMETRY_TYPE_KEY, null);
    setData(WritableDataStoreDataSource.CONVERT_TO_MULTIGEOMETRY_KEY, null);
    setData(WritableDataStoreDataSource.CREATE_PK, null);
    //setData(WritableDataStoreDataSource.DATAKEY_NORMALIZE_TABLE_NAME, null);
    setData(WritableDataStoreDataSource.NORMALIZED_COLUMN_NAMES, null);

    if (getConnectionDescriptor() == null ||
            getTableName() == null || getTableName().trim().length() == 0) {
      return false;
    }

    setData(WritableDataStoreDataSource.CONNECTION_DESCRIPTOR_KEY, getConnectionDescriptor());
    setData(WritableDataStoreDataSource.DATASET_NAME_KEY, getData(SaveWizardPlugIn.DATAKEY_SIMPLIFIED_LAYERNAME));
    setData(WritableDataStoreDataSource.GEOM_DIM_KEY, writeCreate3dGeometriesSelected()?3:2);
    setData(WritableDataStoreDataSource.NAN_Z_TO_VALUE_KEY, nanZToValue());
    setData(WritableDataStoreDataSource.NARROW_GEOMETRY_TYPE_KEY, isNarrowGeometryType());
    setData(WritableDataStoreDataSource.CONVERT_TO_MULTIGEOMETRY_KEY, isConvertToMultiGeometry());
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
