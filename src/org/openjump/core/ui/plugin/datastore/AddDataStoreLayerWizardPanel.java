package org.openjump.core.ui.plugin.datastore;

import java.awt.BorderLayout;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

public class AddDataStoreLayerWizardPanel extends JPanel implements WizardPanel {
  private static final String KEY = AddDataStoreLayerWizardPanel.class.getName();

  private static final String TITLE = I18N.get(KEY);

  private static final String INSTRUCTIONS = I18N.get(KEY + ".instructions");

  private AddDatastoreLayerPanel dataStorePanel;

  public AddDataStoreLayerWizardPanel(WorkbenchContext context) {
    super(new BorderLayout());
    dataStorePanel = new AddDatastoreLayerPanel(context);
    add(new JScrollPane(dataStorePanel), BorderLayout.CENTER);
  }

  public void add(InputChangedListener listener) {
  }

  public void enteredFromLeft(Map dataMap) {
    dataStorePanel.populateConnectionComboBox();
  }

  public void exitingToRight() throws Exception {
  }

  public String getID() {
    return KEY;
  }

  public String getInstructions() {
    return INSTRUCTIONS;
  }

  public String getNextID() {
    return null;
  }

  public String getTitle() {
    return TITLE;
  }

  public boolean isInputValid() {
    return true;
    //return validateInput() == nulldataStorePanel.isValid();
  }

  public void remove(InputChangedListener listener) {
  }

  /**
   * @return the dataStorePanel
   */
  public AddDatastoreLayerPanel getDataStorePanel() {
    return dataStorePanel;
  }

}
