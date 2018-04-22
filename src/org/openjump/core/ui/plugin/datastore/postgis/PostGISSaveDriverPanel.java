/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package org.openjump.core.ui.plugin.datastore.postgis;

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

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This class contains the user interface elements to save a layer into a
 * PostGIS table.
 */
public class PostGISSaveDriverPanel extends AbstractDriverPanel implements ActionListener {
	
	public static final String KEY = PostGISSaveDriverPanel.class.getName();
	
	static final String CREATE_HELP_STRING  = I18N.get(KEY + ".create-or-replace-help-string");
	static final String REPLACE_HELP_STRING = I18N.get(KEY + ".replace-table-rows-help-string");
	static final String INSERT_HELP_STRING  = I18N.get(KEY + ".insert-only-help-string");
	static final String UPDATE_HELP_STRING  = I18N.get(KEY + ".insert-or-update-help-string");
	static final String DELETE_HELP_STRING  = I18N.get(KEY + ".insert-update-or-delete-help-string");
	
	static final String TITLE   = I18N.get(KEY + ".title");
	static final String SELECT_SAVE_METHOD = I18N.get(KEY + ".select-save-method");
	static final String CREATE  = I18N.get(KEY + ".create-or-replace");
	static final String REPLACE = I18N.get(KEY + ".replace-table-rows");
	static final String INSERT  = I18N.get(KEY + ".insert-only");
	static final String UPDATE  = I18N.get(KEY + ".insert-or-update");
	static final String DELETE  = I18N.get(KEY + ".insert-update-or-delete");

	//static final String GEOMETRY_COLUMN = I18N.get(KEY + ".geometry-Column");
	static final String CHOOSE_PK    = I18N.get(KEY + ".primary_key");
	static final String CREATE_DB_PK = I18N.get("org.openjump.core.ui.plugin.datastore.DataStoreSaveDriverPanel.create-database-primary-key");
	
	// UI elements
	private ButtonGroup methodButtons;
	private JRadioButton createButton;
	private JRadioButton replaceButton;
	private JRadioButton insertButton;
	private JRadioButton updateButton;
	private JRadioButton deleteButton;
	//private JLabel geometryColumnLabel;
	private JLabel primaryKeyLabel;
	private JTextArea help;
	private JComboBox<String> primaryKeyComboBox;
	private ConnectionPanel connectionPanel;
	private JComboBox<String> tableComboBox;
	private JCheckBox createPrimaryKeyCheckBox;
	private OKCancelPanel okCancelPanel;
    
	// context variables
	private WorkbenchContext wbContext;
	private String lastUsedLayerName = null;
	private Map<String,String> layer2TableMap = new HashMap<>();
	private DefaultComboBoxModel<String> tableList = new DefaultComboBoxModel<>();

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
		gbConstraints.gridy = 4;
		gbConstraints.gridwidth = 3;
		gbConstraints.gridheight = 1;
		gbLayout.setConstraints(connectionPanel, gbConstraints);
		add(connectionPanel);
		
		// table
		JLabel tableLabel = new JLabel("Table");
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 5;
		gbConstraints.gridwidth = 1;
		gbLayout.setConstraints(tableLabel, gbConstraints);
		add(tableLabel);
		tableComboBox = new JComboBox<String>(tableList);
		tableComboBox.setPrototypeDisplayValue("abcdefghijklmnopqrstuvwxyz0123456789.abcdefghijklmnopqrstuvwxyz0123456789");
		tableComboBox.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent e) {
		        resetPKChooser();
		    }
		});
		gbConstraints.gridx = 1;
		gbConstraints.gridy = 5;
		gbLayout.setConstraints(tableComboBox, gbConstraints);
		add(tableComboBox);
		
		// method
		JLabel methodLabel = new JLabel(SELECT_SAVE_METHOD);
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 6;
		//gbConstraints.gridheight = 1;
		gbLayout.setConstraints(methodLabel, gbConstraints);
		add(methodLabel);
		
		createButton = new JRadioButton (CREATE);
		createButton.setActionCommand(SaveToPostGISDataSource.SAVE_METHOD_CREATE);
		createButton.addActionListener(this);
		gbConstraints.gridx = 1;
		gbConstraints.gridy = 7;
		//gbConstraints.gridheight = 1;
		gbLayout.setConstraints(createButton, gbConstraints);
		add(createButton);
		
		replaceButton = new JRadioButton (REPLACE);
		replaceButton.setActionCommand(SaveToPostGISDataSource.SAVE_METHOD_REPLACE);
		replaceButton.addActionListener(this);
		gbConstraints.gridx = 1;
		gbConstraints.gridy = 8;
		//gbConstraints.gridheight = 1;
		gbLayout.setConstraints(replaceButton, gbConstraints);
		add(replaceButton);
		
		insertButton = new JRadioButton (INSERT);
		insertButton.setActionCommand(SaveToPostGISDataSource.SAVE_METHOD_INSERT);
		insertButton.setSelected(true);
		insertButton.addActionListener(this);
		gbConstraints.gridy = 9;
		gbLayout.setConstraints(insertButton, gbConstraints);
		add(insertButton);
		
		updateButton = new JRadioButton(UPDATE);
		updateButton.setActionCommand(SaveToPostGISDataSource.SAVE_METHOD_UPDATE);
		updateButton.addActionListener(this);
		updateButton.setSelected(false);
		gbConstraints.gridy = 10;
		gbLayout.setConstraints(updateButton, gbConstraints);
		add(updateButton);

		deleteButton = new JRadioButton(DELETE);
		deleteButton.setActionCommand(SaveToPostGISDataSource.SAVE_METHOD_DELETE);
		deleteButton.addActionListener(this);
		deleteButton.setSelected(false);
		gbConstraints.gridy = 11;
		gbLayout.setConstraints(deleteButton, gbConstraints);
		add(deleteButton);
		
		methodButtons = new ButtonGroup();
		methodButtons.add(createButton);
		methodButtons.add(replaceButton);
		methodButtons.add(insertButton);
		methodButtons.add(updateButton);
		methodButtons.add(deleteButton);
		methodButtons.setSelected(createButton.getModel(), true);

		primaryKeyLabel = new JLabel(CHOOSE_PK);
        primaryKeyLabel.setEnabled(true);
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 12;
		gbLayout.setConstraints(primaryKeyLabel, gbConstraints);
		add(primaryKeyLabel);
        primaryKeyComboBox = new JComboBox<>(new String[0]);
        primaryKeyComboBox.setPrototypeDisplayValue("abcdefghijklmnopqrstuvwxyz");
        primaryKeyComboBox.setEnabled(false);
		gbConstraints.gridx = 1;
		gbLayout.setConstraints(primaryKeyComboBox, gbConstraints);
		add(primaryKeyComboBox);

        createPrimaryKeyCheckBox = new JCheckBox(CREATE_DB_PK);
        createPrimaryKeyCheckBox.setEnabled(createButton.isSelected());
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 13;
		gbLayout.setConstraints(createPrimaryKeyCheckBox, gbConstraints);
		add(createPrimaryKeyCheckBox);
		
		JPanel helpPanel = new JPanel();
		helpPanel.setBorder(BorderFactory.createBevelBorder(2, Color.BLACK, Color.GRAY));
		help = new JTextArea(4, 32);
		help.setEditable(false);
		help.setLineWrap(true);
		help.setWrapStyleWord(true);
		help.setBackground(getBackground());
		help.setFont(new Font("Sans-Serif", Font.PLAIN, 12));
		gbConstraints.gridx = 2;
		gbConstraints.gridy = 7;
		gbConstraints.gridwidth = 1;
		gbConstraints.gridheight = 5;
		helpPanel.add(help);
		gbLayout.setConstraints(helpPanel, gbConstraints);
		add(helpPanel);
		
		help.setText(CREATE_HELP_STRING);

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

	//public void setCache(DriverPanelCache cache) {
	//	super.setCache(cache);
	//	commonPanel.setCache(cache);
	//	if(cache.get(PostGISDataSource.SAVE_METHOD_KEY) != null ) {
	//		setSaveMethod((String)cache.get(PostGISDataSource.SAVE_METHOD_KEY));
	//	}
	//	if( cache.get(PostGISDataSource.UNIQUE_COLUMN_KEY) != null ) {
	//		uniqueField.setText((String)cache.get(PostGISDataSource.UNIQUE_COLUMN_KEY));
	//	} 
	//}

	//public DriverPanelCache getCache() {
	//	DriverPanelCache cache = super.getCache();
	//	commonPanel.putCache(cache);
	//	cache.put(PostGISDataSource.SAVE_METHOD_KEY, getSaveMethod());
	//	cache.put(PostGISDataSource.UNIQUE_COLUMN_KEY, uniqueField.getText());
	//	return cache;
	//}
	
	public ConnectionDescriptor getConnectionDescriptor() {
	    return connectionPanel.getConnectionDescriptor();
	}
	
	
	public String getTableName() {
	    layer2TableMap.put(lastUsedLayerName, (String)tableComboBox.getSelectedItem());
	    return (String)tableComboBox.getSelectedItem();
	}
	

	public String getSaveMethod() {
	    return methodButtons.getSelection().getActionCommand();
	}
	
	
	public String getPrimaryKey() {
		if (!primaryKeyComboBox.isEnabled()) return null;
		Object selection = primaryKeyComboBox.getSelectedItem();
		if (selection == null) return null;
		else return selection.toString();
	}
	
	public boolean isCreatePrimaryKeyColumnSelected() {
	    return createPrimaryKeyCheckBox.isEnabled() && createPrimaryKeyCheckBox.isSelected();
	}
	

	public void actionPerformed(ActionEvent ae) {
		String action = ae.getActionCommand();

		if(action.equals(SaveToPostGISDataSource.SAVE_METHOD_CREATE)) {
			tableComboBox.setEditable(true);
            primaryKeyLabel.setEnabled(false);
			resetPKChooser();
            primaryKeyComboBox.setEnabled(false);
            createPrimaryKeyCheckBox.setEnabled(true);
			help.setText(CREATE_HELP_STRING);
		}

		if(action.equals(SaveToPostGISDataSource.SAVE_METHOD_REPLACE)) {
			tableComboBox.setEditable(false);
            primaryKeyLabel.setEnabled(false);
            primaryKeyComboBox.setEnabled(false);
            createPrimaryKeyCheckBox.setEnabled(false);
			help.setText(REPLACE_HELP_STRING);
		}

		if(action.equals(SaveToPostGISDataSource.SAVE_METHOD_INSERT)) {
			tableComboBox.setEditable(false);
            primaryKeyLabel.setEnabled(false);
            resetPKChooser();
            primaryKeyComboBox.setEnabled(false);
            createPrimaryKeyCheckBox.setEnabled(false);
			help.setText(INSERT_HELP_STRING);
		}

		if(action.equals(SaveToPostGISDataSource.SAVE_METHOD_UPDATE)) {
			tableComboBox.setEditable(false);
            primaryKeyLabel.setEnabled(true);
            resetPKChooser();
            primaryKeyComboBox.setEnabled(true);
            createPrimaryKeyCheckBox.setEnabled(false);
			help.setText(UPDATE_HELP_STRING);
		}

		if(action.equals(SaveToPostGISDataSource.SAVE_METHOD_DELETE)) {
			tableComboBox.setEditable(false);
            primaryKeyLabel.setEnabled(true);
            resetPKChooser();
            primaryKeyComboBox.setEnabled(true);
            createPrimaryKeyCheckBox.setEnabled(false);
			help.setText(DELETE_HELP_STRING);
		}
	}
	
	// Called if the source layer has changed
	// Select the create option and choose the layer name as table name
	private void layerChanged() {
	    //System.out.println("layer changed");
	    createButton.setSelected(true);
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
		if (layers.length == 1 && createButton.isSelected()) {
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
	    
		if (tableComboBox.getSelectedItem() != null) {
			resetPKChooser();
		}
	}

	// Initialization of the primary key combo box
	private void resetPKChooser() {
		// Remember last PK choosen
		Object oldValue = primaryKeyComboBox.getSelectedItem();
		Layer[] layers = wbContext.getLayerNamePanel().getSelectedLayers();
		if (layers.length == 1) {
			FeatureSchema schema = layers[0].getFeatureCollectionWrapper().getFeatureSchema();
			List<String> list = new ArrayList<>();
			for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
				if (schema.getAttributeType(i) == AttributeType.LONG ||
								schema.getAttributeType(i) == AttributeType.STRING ||
								schema.getAttributeType(i) == AttributeType.INTEGER ||
								schema.getAttributeType(i) == AttributeType.OBJECT ||
								schema.getExternalPrimaryKeyIndex() == i) {
					list.add(schema.getAttributeName(i));
				}
			}
			primaryKeyComboBox.setModel(new DefaultComboBoxModel<>(list.toArray(new String[list.size()])));
			if (oldValue != null && list.contains(oldValue.toString())) {
				primaryKeyComboBox.setSelectedItem(oldValue);
			}
			this.validate();
		}
	}
	
	/**
	 * Adds a new item to the model, without duplicate, and in alphabetical order
	 */
	private void addItemToTableList(DefaultComboBoxModel<String> model, String item) {
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

	private boolean hasPrimaryKey() {
		Layer[] layers = wbContext.getLayerNamePanel().getSelectedLayers();
		return layers.length == 1 &&
			layers[0].getFeatureCollectionWrapper().getFeatureSchema().getExternalPrimaryKeyIndex() >= 0;
	}

}
