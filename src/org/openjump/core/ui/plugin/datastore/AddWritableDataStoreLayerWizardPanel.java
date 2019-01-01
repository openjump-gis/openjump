package org.openjump.core.ui.plugin.datastore;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.wizard.AbstractWizardPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

/**
 * WizardPanel to connect to a read/write datastore.
 */
public class AddWritableDataStoreLayerWizardPanel extends AbstractWizardPanel {

    private static final String KEY = AddWritableDataStoreLayerWizardPanel.class.getName();

    private static final String TITLE = I18N.get(KEY);

    private static final String INSTRUCTIONS = I18N.get(KEY + ".instructions");

    private AddWritableDataStoreLayerPanel dataStorePanel;

    public AddWritableDataStoreLayerWizardPanel(WorkbenchContext context) {
        super(KEY, TITLE, INSTRUCTIONS);
        setLayout(new BorderLayout());
        dataStorePanel = new AddWritableDataStoreLayerPanel(context);
        dataStorePanel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectionChanged();
            }
        });
        add(new JScrollPane(dataStorePanel), BorderLayout.CENTER);
    }

    public void enteredFromLeft(Map dataMap) {
        dataStorePanel.populateConnectionComboBox();
    }

    public void exitingToRight() throws Exception {
    }

    /**
     * expose the input listeners fire for the wrapped panel to use
     */
    public void selectionChanged(){
        fireInputChanged();
    }

    public boolean isInputValid() {
        return dataStorePanel.validateInput() == null;
    }

    /**
     * @return the dataStorePanel
     */
    public AddWritableDataStoreLayerPanel getDataStorePanel() {
        return dataStorePanel;
    }
}
