package org.openjump.core.ui.plugin.datastore;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.DataStoreMetadata;
import com.vividsolutions.jump.datastore.SQLUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.datastore.ConnectionManager;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.AbstractDriverPanel;
import com.vividsolutions.jump.workbench.ui.OKCancelPanel;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.ConnectionPanel;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Panel containing user interface elements to save a layer into a DataStore.
 */
public class DataStoreSaveDriverPanel extends AbstractDriverPanel {

    public static final String KEY = DataStoreSaveDriverPanel.class.getName();

    private static final String TABLE                    = I18N.get(KEY + ".table");

    private static final String WRITE_3D_GEOM            = I18N.get(KEY + ".write-3d-geometries");
    private static final String CONVERT_NAN_Z            = I18N.get(KEY + ".convert-nan-z");
    private static final String NARROW_GEOMETRY_TYPE     = I18N.get(KEY + ".narrow-geometry-type");
    private static final String CONVERT_TO_MULTIGEOMETRY = I18N.get(KEY + ".convert-to-multigeometry");
    private static final String CREATE_DB_PK             = I18N.get(KEY + ".create-database-primary-key");
    private static final String NORMALIZED_TABLE_NAME    = I18N.get(KEY + ".normalized-table-name-key");
    private static final String NORMALIZED_COLUMN_NAMES  = I18N.get(KEY + ".normalized-column-names-key");

    // UI elements
    private ConnectionPanel connectionPanel;
    private JComboBox<String> tableComboBox;
    private JCheckBox createPrimaryKeyCheckBox;
    private JCheckBox write3dGeomCheckBox;
    private JTextField convertNaNZTextField;
    private JCheckBox narrowGeometryTypeCheckBox;
    private JCheckBox convertToMultiGeometryCheckBox;
    private JCheckBox normalizedTableNameCheckBox;
    private JCheckBox normalizedColumnNamesCheckBox;
    private OKCancelPanel okCancelPanel = new OKCancelPanel();

    // context variables
    private WorkbenchContext wbContext;
    private String lastUsedLayerName = null;
    private Map<String,String> layer2TableMap = new HashMap<>();
    private DefaultComboBoxModel<String> tableList = new DefaultComboBoxModel<>();

    public DataStoreSaveDriverPanel(PlugInContext context) {
        try {
            jbInit(context);
            wbContext = context.getWorkbenchContext();
        } catch( Exception e ) {
            e.printStackTrace();
        }
    }

    // User interface initialization
    protected void jbInit(final PlugInContext context) throws Exception {

        GridBagLayout gbLayout = new GridBagLayout();
        GridBagConstraints gbConstraints = new GridBagConstraints();
        setLayout(gbLayout);

        gbConstraints.insets = new Insets(2, 2, 2, 2);
        gbConstraints.anchor = GridBagConstraints.WEST;

        // title
        JLabel title = new JLabel("<html><h2>" + getTitle() + "</h2><br/></br></html>");
        gbConstraints.gridx = 0;
        gbConstraints.gridy = 0;
        gbConstraints.gridwidth = 2;
        gbLayout.setConstraints(title, gbConstraints);
        add(title);

        // connection panel and listener for connection changes
        connectionPanel = new ConnectionPanel(context.getWorkbenchContext());
        connectionPanel.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                connectionChanged();
            }
        });
        // listen to ancestor to re-init the layer name when the source layer changes
        addAncestorListener(new PanelAncestorListener());
        gbConstraints.gridy += 1;
        gbLayout.setConstraints(connectionPanel, gbConstraints);
        add(connectionPanel);

        // table
        JLabel tableLabel = new JLabel(TABLE);
        gbConstraints.gridx = 0;
        gbConstraints.gridy += 1;
        gbConstraints.gridwidth = 1;
        gbLayout.setConstraints(tableLabel, gbConstraints);
        add(tableLabel);

        tableComboBox = new JComboBox<>(tableList);
        tableComboBox.setPrototypeDisplayValue("abcdefghijklmnopqrstuvwxyz.abcdefghijklmnopqrstuvwxyz");
        gbConstraints.gridx = 1;
        gbLayout.setConstraints(tableComboBox, gbConstraints);
        add(tableComboBox);

        // Primary key checkbox
        createPrimaryKeyCheckBox = new JCheckBox(CREATE_DB_PK);
        createPrimaryKeyCheckBox.setSelected(true);
        gbConstraints.gridx = 0;
        gbConstraints.gridy += 1;
        gbLayout.setConstraints(createPrimaryKeyCheckBox, gbConstraints);
        add(createPrimaryKeyCheckBox);

        // Geometry dimension key checkbox
        write3dGeomCheckBox = new JCheckBox(WRITE_3D_GEOM);
        write3dGeomCheckBox.setSelected(false);
        gbConstraints.gridy += 1;
        gbLayout.setConstraints(write3dGeomCheckBox, gbConstraints);
        add(write3dGeomCheckBox);

        // Convert NaN z
        JLabel convertNaNZLabel = new JLabel(CONVERT_NAN_Z);
        gbConstraints.gridy += 1;
        gbConstraints.gridx = 0;
        gbLayout.setConstraints(convertNaNZLabel, gbConstraints);
        add(convertNaNZLabel);

        convertNaNZTextField = new JTextField("NaN", 12);
        convertNaNZTextField.setEnabled(false);
        convertNaNZTextField.setEditable(false);
        convertNaNZTextField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                return ((JTextField)input).getText().matches("NaN|\\d+(\\.\\d+)?");
            }
        });
        gbConstraints.gridx = 1;
        gbLayout.setConstraints(convertNaNZTextField, gbConstraints);
        add(convertNaNZTextField);
        write3dGeomCheckBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                convertNaNZTextField.setEnabled(((JCheckBox)e.getSource()).isSelected());
                convertNaNZTextField.setEditable(((JCheckBox) e.getSource()).isSelected());
            }
        });

        // narrowGeometryTypeCheckBox checkbox
        narrowGeometryTypeCheckBox = new JCheckBox(NARROW_GEOMETRY_TYPE);
        narrowGeometryTypeCheckBox.setSelected(false);
        gbConstraints.gridx = 0;
        gbConstraints.gridy += 1;
        gbLayout.setConstraints(narrowGeometryTypeCheckBox, gbConstraints);
        add(narrowGeometryTypeCheckBox);

        // convertToMultiGeometryCheckBox checkbox
        convertToMultiGeometryCheckBox = new JCheckBox(CONVERT_TO_MULTIGEOMETRY);
        convertToMultiGeometryCheckBox.setSelected(false);
        gbConstraints.gridy += 1;
        gbLayout.setConstraints(convertToMultiGeometryCheckBox, gbConstraints);
        add(convertToMultiGeometryCheckBox);

        // Normalize column names checkbox
        normalizedTableNameCheckBox = new JCheckBox(NORMALIZED_TABLE_NAME);
        normalizedTableNameCheckBox.setSelected(false);
        gbConstraints.gridx = 0;
        gbConstraints.gridy += 1;
        gbLayout.setConstraints(normalizedTableNameCheckBox, gbConstraints);
        add(normalizedTableNameCheckBox);
        normalizedTableNameCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (((JCheckBox)e.getSource()).isSelected()) {
                    tableComboBox.setSelectedItem(SQLUtil.normalize(
                            wbContext.getLayerableNamePanel().getSelectedLayers()[0].getName()));
                } else {
                    tableComboBox.setSelectedItem(
                            wbContext.getLayerableNamePanel().getSelectedLayers()[0].getName());
                }
            }
        });

        // Normalize column names checkbox
        normalizedColumnNamesCheckBox = new JCheckBox(NORMALIZED_COLUMN_NAMES);
        normalizedColumnNamesCheckBox.setSelected(false);
        gbConstraints.gridy += 1;
        gbLayout.setConstraints(normalizedColumnNamesCheckBox, gbConstraints);
        add(normalizedColumnNamesCheckBox);

    }

    protected String getTitle() {
        return I18N.get(this.getClass().getName() + ".title");
    }

    private class PanelAncestorListener implements AncestorListener {
        // called when the panel or an ancestor is made visible
        // call layerChanged if the source layer has changed since last call
        public void ancestorAdded(AncestorEvent e) {
            Layer[] layers = wbContext.getLayerableNamePanel().getSelectedLayers();
            if (layers.length == 1) {
                // call connectionChanged to refresh the list of tables available
                // in the database in case it has been changed by another client
                connectionChanged();
                String layerName = layers[0].getName();
                // if selected layer has changed, refresh tableList state
                if (!layerName.equals(lastUsedLayerName)) {
                    lastUsedLayerName = layerName;
                    layerChanged();
                }
                // if layerName has already been associated to a different table name
                // select this table name in the comboBox
                if (layer2TableMap.get(layerName) != null) {
                    tableList.setSelectedItem(layer2TableMap.get(layerName));
                }
                else {
                    tableList.setSelectedItem(layerName);
                }
            }
        }
        public void ancestorMoved(AncestorEvent event) {}
        public void ancestorRemoved(AncestorEvent event) {}
    }


    public String getValidationError() {
        return null;
    }


    public void addActionListener(ActionListener l) {
        okCancelPanel.addActionListener( l );
    }


    public void removeActionListener(ActionListener l) {
        okCancelPanel.removeActionListener( l );
    }

    public boolean wasOKPressed() {
        return okCancelPanel.wasOKPressed();
    }


    public ConnectionDescriptor getConnectionDescriptor() {
        return connectionPanel.getConnectionDescriptor();
    }

    public void setConnectionDescriptor(ConnectionDescriptor cd) {
        connectionPanel.setConnectionDescriptor(cd);
    }


    public String getTableName() {
        layer2TableMap.put(lastUsedLayerName, tableComboBox.getSelectedItem().toString());
        return tableComboBox.getSelectedItem().toString();
    }

    public void setTableName(String tableName) {
        tableComboBox.setSelectedItem(tableName);
    }

    public boolean isCreatePrimaryKeyColumnSelected() {
        return createPrimaryKeyCheckBox.isSelected();
    }

    //public boolean writeCreateMultiGeometriesSelected() {
    //    return writeMultiGeomCheckBox.isSelected();
    //}

    public boolean writeCreate3dGeometriesSelected() {
        return write3dGeomCheckBox.isSelected();
    }

    public double nanZToValue() {
        return Double.parseDouble(convertNaNZTextField.getText());
    }

    public boolean isNarrowGeometryType() {
        return narrowGeometryTypeCheckBox.isSelected();
    }

    public boolean isConvertToMultiGeometry() {
        return convertToMultiGeometryCheckBox.isSelected();
    }

    public boolean isNormalizedTableName() {
        return normalizedTableNameCheckBox.isSelected();
    }

    public boolean isNormalizedColumnNames() {
        return normalizedColumnNamesCheckBox.isSelected();
    }

    // Called if the source layer has changed
    // Select the create option and choose the layer name as table name
    private void layerChanged() {
        addItemToTableList(tableList, lastUsedLayerName);
    }

    // Called if the connection changed
    // The list of candidate table names is updated using database metadata
    private void connectionChanged() {
        //System.out.println("change connection");
        try {
            if (ConnectionManager.instance(wbContext) == null ||
                    connectionPanel.getConnectionDescriptor() == null ||
                    getDSConnection() == null) {
                updateTableList(null);
            }
            else {
                DataStoreMetadata metadata = getDSConnection().getMetadata();
                updateTableList(metadata);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private DataStoreConnection getDSConnection() throws Exception {
        return ConnectionManager.instance(wbContext)
                .getOpenConnection(connectionPanel.getConnectionDescriptor());
    }

    // Update table list to choose from, using database metadata
    // Eventually add source layer name to the list if create option is selected
    private void updateTableList(DataStoreMetadata metadata) {
        Layer[] layers = wbContext.getLayerableNamePanel().getSelectedLayers();
        if (layers.length == 1) {
            String layerName = layers[0].getName();
            addItemToTableList(tableList, layerName);
            tableComboBox.setSelectedItem(layerName);
        }
        if (metadata != null) {
            String[] tableNames = metadata.getDatasetNames();
            for (String t : tableNames) {
                addItemToTableList(tableList, t);
            }
        }
        tableComboBox.setEditable(true);
        this.validate();
    }

    /**
     * Adds a new item to the model, without duplicate, and in alphabetical order
     */
    private void addItemToTableList(DefaultComboBoxModel<String> model, String item) {
        for (int i = 0 ; i < model.getSize() ; i++) {
            String item_i = model.getElementAt(i);
            int compare = item.compareTo(item_i);
            if (compare < 0) {
                model.insertElementAt(item, i);
                return;
            }
            else if (compare == 0) return;
        }
        model.insertElementAt(item, model.getSize());
    }

}
