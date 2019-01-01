package org.openjump.core.ui.plugin.datastore;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.wizard.*;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;

/**
 * Wrapper class to include DataStoreSaveDriverPanel into the new SaveWizardPlugIn
 */
public class SaveToDataStorePanel extends AbstractWizardPanel {

  public static final String KEY = SaveToDataStorePanel.class.getName();

  private static final String TITLE = I18N.get(KEY);

  private static final String INSTRUCTIONS = I18N.get(KEY + ".instructions");

  private static final String LASTCONNECTION = SaveToDataStorePanel.class.getName()
          + " - LAST CONNECTION";

  private WorkbenchContext workbenchContext;
  private DataStoreSaveDriverPanel dataStoreSaveDriverPanel;

  public SaveToDataStorePanel(final WorkbenchContext workbenchContext) {
    super(KEY, TITLE, INSTRUCTIONS);
    this.workbenchContext = workbenchContext;
    setLayout(new BorderLayout());
    dataStoreSaveDriverPanel = new DataStoreSaveDriverPanel(workbenchContext.createPlugInContext());
    add(dataStoreSaveDriverPanel);
    dataStoreSaveDriverPanel.connectionPanel.addActionListener(new ActionListener() {
      public void actionPerformed( ActionEvent e ) {
        updateWizardDialog();
      }
    });
    dataStoreSaveDriverPanel.tableComboBox.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        updateWizardDialog();
      }
    });
    ((JTextComponent)dataStoreSaveDriverPanel.tableComboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(new DocumentListener() {
      @Override public void changedUpdate(DocumentEvent e) {updateWizardDialog();}
      @Override public void	insertUpdate(DocumentEvent e) {updateWizardDialog();}
      @Override public void	removeUpdate(DocumentEvent e) {updateWizardDialog();}
    });
  }

  private void updateWizardDialog() {
    Component c = dataStoreSaveDriverPanel;
    while ((c = c.getParent()) != null) {
      if (c instanceof WizardPanel) {
        ((SaveToDataStorePanel) c).selectionChanged();
        break;
      }
    }
  }

  public void selectionChanged(){
    fireInputChanged();
  }


  public boolean isInputValid() {
    String errorMessage = dataStoreSaveDriverPanel.getValidationError();
    if (dataStoreSaveDriverPanel.tableComboBox.getEditor().getItem().toString().trim().equals("")) errorMessage =
            I18N.getMessage("org.openjump.core.ui.plugin.datastore.postgis.PostGISSaveDataSourceQueryChooser.no-table-choosen");
    //if (errorMessage != null) workbenchContext.getWorkbench().getFrame().warnUser(errorMessage);
    return null == errorMessage;
  }

  @Override
  public void enteredFromLeft(Map dataMap) {
    // restore last folder
    super.enteredFromLeft(dataMap);
    Blackboard blackboard = PersistentBlackboardPlugIn.get(workbenchContext);
    Object connectionDescriptor = blackboard.get(LASTCONNECTION);
    if (connectionDescriptor != null) {
      dataStoreSaveDriverPanel.setConnectionDescriptor((ConnectionDescriptor)connectionDescriptor);
    }
    dataStoreSaveDriverPanel.getTableName();
    dataMap.put(WritableDataStoreDataSource.CONNECTION_DESCRIPTOR_KEY, dataStoreSaveDriverPanel.getConnectionDescriptor());
    //dataStoreSaveDriverPanel.setTableName(getData(SaveWizardPlugIn.DATAKEY_SIMPLIFIED_LAYERNAME).toString());
  }

  @Override
  public void exitingToRight() throws Exception {
    if (!dataStoreSaveDriverPanel.isInputValid())
      throw new CancelNextException();
    Blackboard blackboard = PersistentBlackboardPlugIn.get(workbenchContext);
    getData().put(WritableDataStoreDataSource.CONNECTION_DESCRIPTOR_KEY, dataStoreSaveDriverPanel.getConnectionDescriptor());
    getData().put(WritableDataStoreDataSource.DATASET_NAME_KEY, dataStoreSaveDriverPanel.getTableName()/*getData(SaveWizardPlugIn.DATAKEY_SIMPLIFIED_LAYERNAME)*/);
    getData().put(WritableDataStoreDataSource.GEOM_DIM_KEY, dataStoreSaveDriverPanel.writeCreate3dGeometriesSelected()?3:2);
    getData().put(WritableDataStoreDataSource.NAN_Z_TO_VALUE_KEY, dataStoreSaveDriverPanel.nanZToValue());
    getData().put(WritableDataStoreDataSource.NARROW_GEOMETRY_TYPE_KEY, dataStoreSaveDriverPanel.isNarrowGeometryType());
    getData().put(WritableDataStoreDataSource.CONVERT_TO_MULTIGEOMETRY_KEY, dataStoreSaveDriverPanel.isConvertToMultiGeometry());
    getData().put(WritableDataStoreDataSource.CREATE_PK, dataStoreSaveDriverPanel.isCreatePrimaryKeyColumnSelected());
    //getData().put(WritableDataStoreDataSource.DATAKEY_NORMALIZE_TABLE_NAME, isNormalizedTableName());
    getData().put(WritableDataStoreDataSource.NORMALIZED_COLUMN_NAMES, dataStoreSaveDriverPanel.isNormalizedColumnNames());
    blackboard.put(LASTCONNECTION, dataStoreSaveDriverPanel.getConnectionDescriptor());
  }

}
