package org.openjump.core.ui.plugin.datastore.postgis2;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.DataStoreMetadata;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.datastore.ConnectionManager;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.AbstractDriverPanel;
import com.vividsolutions.jump.workbench.ui.OKCancelPanel;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.ConnectionPanel;
import org.openjump.core.ui.plugin.datastore.WritableDataStoreDataSource;
import org.openjump.core.ui.plugin.datastore.postgis.SaveToPostGISDataSource;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * This class contains the user interface elements to save a layer into a
 * PostGIS table.
 */
public class PostGISSaveDriverPanel extends AbstractDriverPanel {

    public static final String KEY = PostGISSaveDriverPanel.class.getName();

    static final String TITLE   = I18N.get(KEY + ".title");

    static final String CREATE_DB_PK = I18N.get(KEY + ".create-database-primary-key");

    // UI elements
    private ConnectionPanel connectionPanel;
    private JComboBox tableComboBox;
    private JCheckBox createPrimaryKeyCheckBox;
    private OKCancelPanel okCancelPanel;

    // context variables
    private WorkbenchContext wbContext;
    private String lastUsedLayerName = null;
    private Map<String,String> layer2TableMap = new HashMap<String,String>();
    private DefaultComboBoxModel tableList = new DefaultComboBoxModel();

    public PostGISSaveDriverPanel(PlugInContext context) {
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
        JLabel title = new JLabel("<html><h2>" + TITLE + "</h2><br/></br></html>");
        gbConstraints.gridx = 0;
        gbConstraints.gridy = 0;
        gbConstraints.gridwidth = 3;
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
        addAncestorListener(new DPAncestorListener());

        gbConstraints.gridx = 0;
        gbConstraints.gridy = 1;
        gbConstraints.gridwidth = 3;
        //gbConstraints.gridheight = 1;
        gbLayout.setConstraints(connectionPanel, gbConstraints);
        add(connectionPanel);

        // table
        JLabel tableLabel = new JLabel("Table");
        gbConstraints.gridx = 0;
        gbConstraints.gridy = 2;
        gbConstraints.gridwidth = 1;
        gbLayout.setConstraints(tableLabel, gbConstraints);
        add(tableLabel);
        tableComboBox = new JComboBox(tableList);
        tableComboBox.setPrototypeDisplayValue("abcdefghijklmnopqrstuvwxyz");
        // tableComboBox.addActionListener(new ActionListener(){
        //     public void actionPerformed(ActionEvent e) {
        //         resetPKChooser();
        //     }
        // });
        gbConstraints.gridx = 1;
        gbConstraints.gridy = 2;
        gbLayout.setConstraints(tableComboBox, gbConstraints);
        add(tableComboBox);

        createPrimaryKeyCheckBox = new JCheckBox(CREATE_DB_PK);
        createPrimaryKeyCheckBox.setSelected(true);
        gbConstraints.gridx = 0;
        gbConstraints.gridy = 3;
        gbLayout.setConstraints(createPrimaryKeyCheckBox, gbConstraints);
        add(createPrimaryKeyCheckBox);

    }

    class DPAncestorListener implements AncestorListener {
        // called when the panel or an ancestor is made visible
        // call layerChanged if the source layer has changed since last call
        public void ancestorAdded(AncestorEvent e) {
            Layer[] layers = wbContext.getLayerNamePanel().getSelectedLayers();
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


    public String getTableName() {
        //System.out.println("table name : " + tableComboBox.getSelectedItem().toString());
        layer2TableMap.put(lastUsedLayerName, tableComboBox.getSelectedItem().toString());
        return tableComboBox.getSelectedItem().toString();
    }


    //public String getSaveMethod() {
    //    return methodButtons.getSelection().getActionCommand();
    //}


    //public String getPrimaryKey() {
    //    Object selection = primaryKeyComboBox.getSelectedItem();
    //    if (selection == null) return null;
    //        //else if (selection.equals(NO_PK)) return SaveToPostGISDataSource.NO_LOCAL_ID;
    //    else return selection.toString();
    //}

    public boolean isCreatePrimaryKeyColumnSelected() {
        return createPrimaryKeyCheckBox.isSelected();
    }

    // Called if the source layer has changed
    // Select the create option and choose the layer name as table name
    private void layerChanged() {
        //System.out.println("layer changed");
        //createButton.setSelected(true);
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

    // Warning : nothing says this is a PostGIS connection
    //private Connection getConnection() {
    //    Connection connection = null;
    //    try {
    //        DataStoreConnection dsConnection = getDSConnection();
    //        if (dsConnection == null || !(dsConnection instanceof PostgisDSConnection)) return null;
    //        connection = ((PostgisDSConnection)dsConnection).getConnection();
    //    } catch (Exception e) {
    //        e.printStackTrace();
    //    }
    //    return connection;
    //}

    // Update table list to choose from, using database metadata
    // Eventually add source layer name to the list if create option is selected
    private void updateTableList(DataStoreMetadata metadata) {
        //System.out.println("update table list");
        Layer[] layers = wbContext.getLayerNamePanel().getSelectedLayers();
        // If create option is selected, default table name will be the layer name
        if (layers.length == 1 /*&& createButton.isSelected()*/) {
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
    private void addItemToTableList(DefaultComboBoxModel model, String item) {
        for (int i = 0 ; i < model.getSize() ; i++) {
            String item_i = (String)model.getElementAt(i);
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
