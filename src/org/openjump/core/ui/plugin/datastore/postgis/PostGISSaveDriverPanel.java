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

import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.DataStoreMetadata;
import com.vividsolutions.jump.datastore.postgis.PostgisDSConnection;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.datastore.ConnectionManager;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.ConnectionPanel;


/**
 * This class contains the ui for the save PostGIS plugin.
 */
public class PostGISSaveDriverPanel extends AbstractDriverPanel implements ActionListener {
	
    public static final String KEY = PostGISSaveDriverPanel.class.getName();
	
	static final String CREATE_HELP_STRING = I18N.get(KEY + ".create-or-replace-help-string");
	static final String INSERT_HELP_STRING = I18N.get(KEY + ".insert-only-help-string");
	static final String UPDATE_HELP_STRING = I18N.get(KEY + ".insert-or-update-help-string");
	static final String DELETE_HELP_STRING = I18N.get(KEY + ".insert-update-or-delete-help-string"); 
	
	static final String TITLE  = I18N.get(KEY + ".title");
	static final String SELECT_SAVE_METHOD = I18N.get(KEY + ".select-save-method");
	static final String CREATE = I18N.get(KEY + ".create-or-replace");
	static final String INSERT = I18N.get(KEY + ".insert-only");
	static final String UPDATE = I18N.get(KEY + ".insert-or-update");
	static final String DELETE = I18N.get(KEY + ".insert-update-or-delete");

	//static final String GEOMETRY_COLUMN = I18N.get(KEY + ".geometry-Column");
	static final String ID_COLUMN = I18N.get(KEY + ".id-column");
	
    ButtonGroup methodButtons;
    JRadioButton createButton;
    JRadioButton insertButton;
    JRadioButton updateButton;
    JRadioButton deleteButton;
    JLabel geometryColumnLabel;
    JLabel idColumnLabel;
    JTextArea help;
    //JTextField geometryField;
    JComboBox idColumnComboBox;
    ConnectionPanel connectionPanel;
    JComboBox tableComboBox;
    OKCancelPanel okCancelPanel;
    WorkbenchContext wbContext;
    
    public PostGISSaveDriverPanel(PlugInContext context) {
        try {
            jbInit(context);
            wbContext = context.getWorkbenchContext();
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}

	void jbInit(final PlugInContext context) throws Exception {
		
		GridBagLayout gbLayout = new GridBagLayout();
		GridBagConstraints gbConstraints = new GridBagConstraints();
		setLayout(gbLayout);
		Insets insets = new Insets(2, 2, 2, 2);
		gbConstraints.insets = insets;
		gbConstraints.anchor = GridBagConstraints.WEST;
		//setBorder(BorderFactory.createLineBorder(Color.black));
		
		// title
		JLabel title = new JLabel("<html><h2>" + I18N.get(TITLE) + "</h2><br/></br></html>");
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 0;
		gbConstraints.gridwidth = 3;
		gbLayout.setConstraints(title, gbConstraints);
		add(title);
		
		// help
		//JEditorPane helpPane = new JEditorPane("text/html", "<html>This is a help string</html>");
		//gbConstraints.gridx = 0;
		//gbConstraints.gridy = 1;
		//gbConstraints.gridwidth = 4;
		//gbConstraints.gridheight = 3;
		//gbLayout.setConstraints(helpPane, gbConstraints);
		//add(helpPane);
		
		// connection
		connectionPanel = new ConnectionPanel(context.getWorkbenchContext());
		connectionPanel.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent e) {
		        changeConnection();
		    }
		});
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
		//Layer[] layers = context.getLayerNamePanel().getSelectedLayers();
		//DefaultComboBoxModel model;
		//System.out.println("length"+layers.length);
	    //if (layers.length == 1) {
	    //    model = new DefaultComboBoxModel(new Object[]{layers[0].getName()});
	    //} else {
	    //    model = new DefaultComboBoxModel(new Object[0]);
	    //}
		tableComboBox = new JComboBox(new DefaultComboBoxModel(new Object[0]));
		tableComboBox.setPrototypeDisplayValue("abcdefghijklmnopqrstuvwxyz");
		tableComboBox.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent e) {
		        resetKeyChooser();
		    }
		});
		gbConstraints.gridx = 1;
		gbConstraints.gridy = 5;
		gbLayout.setConstraints(tableComboBox, gbConstraints);
		add(tableComboBox);
		
		// method
		JLabel methodLabel = new JLabel("Save method");
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
		
		insertButton = new JRadioButton (INSERT);
		insertButton.setActionCommand(SaveToPostGISDataSource.SAVE_METHOD_INSERT);
		insertButton.setSelected(true);
		insertButton.addActionListener(this);
		gbConstraints.gridy = 8;
		gbLayout.setConstraints(insertButton, gbConstraints);
		add(insertButton);
		
		updateButton = new JRadioButton(UPDATE);
		updateButton.setActionCommand(SaveToPostGISDataSource.SAVE_METHOD_UPDATE);
		updateButton.addActionListener(this);
		updateButton.setSelected(false);
		gbConstraints.gridy = 9;
		gbLayout.setConstraints(updateButton, gbConstraints);
		add(updateButton);
		
		deleteButton = new JRadioButton(DELETE);
		deleteButton.setActionCommand(SaveToPostGISDataSource.SAVE_METHOD_DELETE);
		deleteButton.addActionListener(this);
		deleteButton.setSelected(false);
		gbConstraints.gridy = 10;
		gbLayout.setConstraints(deleteButton, gbConstraints);
		add(deleteButton);
		
		methodButtons = new ButtonGroup();
		methodButtons.add(createButton);
		methodButtons.add(insertButton);
		methodButtons.add(updateButton);
		methodButtons.add(deleteButton);
		methodButtons.setSelected(createButton.getModel(), true);
		
		idColumnLabel = new JLabel(ID_COLUMN);
		idColumnLabel.setEnabled(true);
		gbConstraints.gridx = 0;
		gbConstraints.gridy = 11;
		gbLayout.setConstraints(idColumnLabel, gbConstraints);
		add(idColumnLabel);
		idColumnComboBox = new JComboBox(new Object[0]);
		idColumnComboBox.setPrototypeDisplayValue("abcdefghijklmnopqrstuvwxyz");
		idColumnComboBox.setEnabled(true);
		gbConstraints.gridx = 1;
		gbLayout.setConstraints(idColumnComboBox, gbConstraints);
		add(idColumnComboBox);
		
		JPanel helpPanel = new JPanel();
		helpPanel.setBorder(BorderFactory.createBevelBorder(2,Color.BLACK, Color.GRAY));
		help = new JTextArea(4, 32);
		help.setEditable(false);
		help.setLineWrap(true);
		help.setWrapStyleWord(true);
		help.setBackground(getBackground());
		help.setFont(new Font("Sans-Serif", Font.PLAIN, 12));
		gbConstraints.gridx = 2;
		gbConstraints.gridy = 7;
		gbConstraints.gridwidth = 1;
		gbConstraints.gridheight = 4;
		helpPanel.add(help);
		gbLayout.setConstraints(helpPanel, gbConstraints);
		add(helpPanel);
		
        // U.D. Default overwrite-info		
		help.setText(CREATE_HELP_STRING);

        //setSaveMethod(SaveToPostGISDataSource.SAVE_METHOD_CREATE);
		
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
	    //System.out.println("table name : " + tableComboBox.getSelectedItem().toString());
	    //System.out.println("table name : " + tableComboBox.getEditor().getItem());
	    return tableComboBox.getSelectedItem().toString();
	}

    //private void setSaveMethod(String method) {
	//	if(method.equals(SaveToPostGISDataSource.SAVE_METHOD_UPDATE)) {
	//		updateButton.doClick();
	//	}
	//}

	public String getSaveMethod() {
	    return methodButtons.getSelection().getActionCommand();
	}
	
	//public String getGeometryColumn() {
	//	return geometryField.getText();
	//}
	//
	//public void setGeometryColumn(String geometryColumn) {
	//	geometryField.setText(geometryColumn);
	//}
	
	public String getIdColumn() {
	    Object selection = idColumnComboBox.getSelectedItem();
		return selection == null ? null : selection.toString();
	}
	
	//public void setIdColumnModel() {
	//}

	public void actionPerformed(ActionEvent ae) {
		String action = ae.getActionCommand();
		if(action.equals(SaveToPostGISDataSource.SAVE_METHOD_CREATE)) {
		    Layer[] layers = wbContext.getLayerNamePanel().getSelectedLayers();
		    DefaultComboBoxModel model;
	        if (layers.length == 1) {
	            model = new DefaultComboBoxModel(new Object[]{layers[0].getName()});
	        } else {
	            model = new DefaultComboBoxModel(new Object[0]);
	        }
		    tableComboBox.setModel(model);
			tableComboBox.setEditable(true);
			idColumnLabel.setEnabled(true);
			idColumnComboBox.setEnabled(true);
			help.setText(CREATE_HELP_STRING);
		}
		if(action.equals(SaveToPostGISDataSource.SAVE_METHOD_INSERT)) {
			tableComboBox.setEditable(false);
			idColumnLabel.setEnabled(false);
			idColumnComboBox.setEnabled(false);
			help.setText(INSERT_HELP_STRING);
		}
		if(action.equals(SaveToPostGISDataSource.SAVE_METHOD_UPDATE)) {
			tableComboBox.setEditable(false);
			idColumnLabel.setEnabled(true);
			idColumnComboBox.setEnabled(true);
			//idColumnComboBox.setEditable(true);
			help.setText(UPDATE_HELP_STRING);
		}
		if(action.equals(SaveToPostGISDataSource.SAVE_METHOD_DELETE)) {
			tableComboBox.setEditable(false);
			idColumnLabel.setEnabled(true);
			idColumnComboBox.setEnabled(true);
			//idColumnComboBox.setEditable(true);
			help.setText(DELETE_HELP_STRING);
		}
	}
	
	private void changeConnection() {
	    System.out.println("change connection");
	    if (ConnectionManager.instance(wbContext) == null) return;
	    try {
	        DataStoreConnection dsConnection = getDSConnection();
	        if (dsConnection == null) return;
	        DataStoreMetadata metadata = dsConnection.getMetadata();
	        updateTableList(metadata);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	private DataStoreConnection getDSConnection() throws Exception {
	    return ConnectionManager.instance(wbContext)
	            .getOpenConnection(connectionPanel.getConnectionDescriptor());
	}
	
	// Attention, rien ne garantit pour l'instant que c'est une connection PostGIS
	private Connection getConnection() {
	    Connection connection = null;
	    try {
	        DataStoreConnection dsConnection = getDSConnection();
	        if (dsConnection == null || !(dsConnection instanceof PostgisDSConnection)) return null;
	        connection = ((PostgisDSConnection)dsConnection).getConnection();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return connection;
	}
	
	private void updateTableList(DataStoreMetadata metadata) {
	    System.out.println("update table list");
	    String previousSelection = (String)tableComboBox.getSelectedItem();
	    tableComboBox.setModel(new DefaultComboBoxModel(metadata.getDatasetNames()));
	    tableComboBox.setEditable(true);
	    this.validate();
	    if (Arrays.asList(metadata.getDatasetNames()).contains(previousSelection)) {
	        tableComboBox.setSelectedItem(previousSelection);
	    }
	    else if (createButton.isSelected()) {
	        tableComboBox.getEditor().setItem("");
	    }
	    else {
	        resetKeyChooser();
	    }
	}
	
	//private String[] getColumns(Connection conn, String table, boolean includeGeometry, boolean includeNullable) throws SQLException {
	//    DatabaseMetaData metadata = conn.getMetaData();
	//    //String[] schema_table = getSchemaTable(table);
	//    String[] schema_table = PostGISUtil.divideTableName(table);
	//    ResultSet rs = metadata.getColumns(null, schema_table[0], schema_table[1], null);
	//    List<String> columns = new ArrayList<String>();
	//    while (rs.next()) {
	//        if (!includeGeometry && rs.getString("TYPE_NAME").equals("GEOMETRY")) continue;
	//        if (!includeNullable && rs.getString("IS_NULLABLE").equals("YES")) continue;
	//        columns.add(rs.getString("COLUMN_NAME"));
	//    }
	//    return columns.toArray(new String[columns.size()]);
	//}
	
	private void resetKeyChooser() {
	    Layer[] layers = wbContext.getLayerNamePanel().getSelectedLayers();
	    if (layers.length == 1) {
	        FeatureSchema schema = layers[0].getFeatureCollectionWrapper().getFeatureSchema();
	        List<String> list = new ArrayList<String>();
	        for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
	            if (schema.getAttributeType(i) == AttributeType.STRING ||
	                schema.getAttributeType(i) == AttributeType.INTEGER) {
	                list.add(schema.getAttributeName(i));
	            }
	        }
	        idColumnComboBox.setModel(new DefaultComboBoxModel(list.toArray(new String[list.size()])));
	        this.validate();
	    }
	}

}
