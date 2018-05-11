package org.openjump.core.ui.plugin.datastore;

import java.awt.BorderLayout;
import java.util.List;
import java.util.Map;

import javax.swing.JScrollPane;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel;
import com.vividsolutions.jump.workbench.ui.wizard.AbstractWizardPanel;

public class AddDataStoreLayerWizardPanel extends AbstractWizardPanel {

  private static final String KEY = AddDataStoreLayerWizardPanel.class.getName();

  private static final String TITLE = I18N.get(KEY);

  private static final String INSTRUCTIONS = I18N.get(KEY + ".instructions");

  private AddDatastoreLayerPanel dataStorePanel;

  public AddDataStoreLayerWizardPanel(WorkbenchContext context) {
    super();
    setLayout(new BorderLayout());
    dataStorePanel = new AddDatastoreLayerPanel(context);
    add(new JScrollPane(dataStorePanel), BorderLayout.CENTER);
  }

  public void enteredFromLeft(Map dataMap) {
    dataStorePanel.populateConnectionComboBox();
  }

  public String getID() {
    return KEY;
  }

  public String getInstructions() {
    return INSTRUCTIONS;
  }

  public String getTitle() {
    return TITLE;
  }

  /**
   * @return the dataStorePanel
   */
  public AddDatastoreLayerPanel getDataStorePanel() {
    return dataStorePanel;
  }

  /**
   * expose the input listeners fire for the wrapped panel to use
   */
  public void selectionChanged(){
    fireInputChanged();
  }

  @Override
  public boolean isInputValid() {
    List l = dataStorePanel.getDatasetLayers();
    return l != null && l.size() > 0;
  }

}
