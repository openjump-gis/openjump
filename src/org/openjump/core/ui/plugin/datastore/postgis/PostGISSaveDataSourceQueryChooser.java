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

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.vividsolutions.jump.workbench.datasource.DataSourceQueryChooser;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

/**
 * A DataSourceQueryChooser for writing to a PostGIS data source.
 */
public class PostGISSaveDataSourceQueryChooser implements DataSourceQueryChooser {
    
    private PostGISSaveDriverPanel panel;
    private SaveToPostGISDataSource dataSource;
    private HashMap properties;
    
    ButtonGroup methodButtons;
    JRadioButton insertButton;
    JRadioButton updateButton;
    JTextArea help;
    JTextField uniqueField;
  
    /**
     * Creates a new query chooser.
     * @param dataSource DataSource object to be queried against.
     */
    public PostGISSaveDataSourceQueryChooser(SaveToPostGISDataSource dataSource, PlugInContext context) {
        this.dataSource = dataSource; 
        panel = new PostGISSaveDriverPanel(context);
        properties = new HashMap();
    }
  
    /**
     * @see com.vividsolutions.jump.workbench.datasource.DataSourceQueryChooser#getComponent()
     */
    public Component getComponent() {
        return panel; 
    }
  
    /**
     * Since the ui does not allow for loading of multiple tables, 
     * the returned collection only contains a single element.
     * @see com.vividsolutions.jump.workbench.datasource.DataSourceQueryChooser#getDataSourceQueries()
     */
    public Collection getDataSourceQueries() {
        // Get the name of the table to update 
        String updateQuery = (String)properties.get(SaveToPostGISDataSource.TABLE_KEY);
        // Create a DataSourceQuery from a datasource, and a query   
        SaveToPostGISDataSourceQuery query = new SaveToPostGISDataSourceQuery(
            getDataSource(), updateQuery, 
            (String)properties.get(SaveToPostGISDataSource.TABLE_KEY)
        );    
        query.setProperties(getProperties());
        
        List queries = new ArrayList();
        queries.add(query);
      
        return queries;
    }
  
    /**
     * Checks that user input is valid.
     * @see com.vividsolutions.jump.workbench.datasource.DataSourceQueryChooser#isInputValid()
     */
    public boolean isInputValid() {
        //if (!super.isInputValid()) return(false);
        if (panel.getConnectionDescriptor() == null) { 
                JOptionPane.showMessageDialog(panel, 
                    "No connection choosen!",
                    "Error!", JOptionPane.ERROR_MESSAGE );
            return false;
        }
        else if (!panel.getConnectionDescriptor()
                       .getDataStoreDriverClassName()
                       .equals("com.vividsolutions.jump.datastore.postgis.PostgisDataStoreDriver")) {
                JOptionPane.showMessageDialog(null,
                    "The selected Connection is not a PostGIS connection!",
                    "Error!", JOptionPane.ERROR_MESSAGE );
            return false;
        }
        else if (panel.getTableName() == null || 
            panel.getTableName().trim().length() == 0) { 
                JOptionPane.showMessageDialog(panel, 
                    "You must choose a table to write to!",
                    "Error!", JOptionPane.ERROR_MESSAGE );
            return false;
        }
        else if ((panel.getIdColumn() == null || panel.getIdColumn().trim().length() == 0) && 
            (panel.getSaveMethod().equals(SaveToPostGISDataSource.SAVE_METHOD_UPDATE) || 
             panel.getSaveMethod().equals(SaveToPostGISDataSource.SAVE_METHOD_DELETE))) { 
                JOptionPane.showMessageDialog(panel, 
                    "Unique Column does not exist!",
                    "Error!", JOptionPane.ERROR_MESSAGE );
            return false;
        }
        return true;
    }
  
    /**
     * Reads all the connection + query properties from the ui.
     */
    protected HashMap getProperties() {
        if (properties == null) properties = new HashMap();
        properties.put(SaveToPostGISDataSource.CONNECTION_DESCRIPTOR_KEY, panel.getConnectionDescriptor());
        properties.put(SaveToPostGISDataSource.TABLE_KEY, panel.getTableName());
        properties.put(SaveToPostGISDataSource.SAVE_METHOD_KEY, panel.getSaveMethod());
        //properties.put(SaveToPostGISDataSource.GEOMETRY_COLUMN_KEY, panel.getGeometryColumn());
        properties.put(SaveToPostGISDataSource.ID_COLUMN_KEY, panel.getIdColumn());
        return properties;
    }
    
    protected SaveToPostGISDataSource getDataSource() {
        return dataSource;   
    }
  
    public String toString() {
        return "PostGIS Table";
    }

}