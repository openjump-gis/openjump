package org.openjump.core.ui.plugin.datastore.postgis;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureSchema;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * static methods to help formatting sql statements for PostGIS
 */
public class PostGISConnectionUtil {
    
    Connection connection;
    
    public PostGISConnectionUtil(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Returns the geometry dimension defined in geometry_columns for this table.
     */
    public int getGeometryDimension(String dbSchema, String dbTable, int defaultDim) {
        try {
            StringBuilder query = new StringBuilder("SELECT coord_dimension FROM geometry_columns WHERE ");
            if (dbSchema != null) query.append("f_table_schema = '").append(dbSchema).append("' AND ");
            query.append("f_table_name = '").append(dbTable).append("';");
            ResultSet rs = connection.createStatement().executeQuery(query.toString());
            if (rs.next()) return rs.getInt(1);
            else return defaultDim;
        } catch(SQLException sqle) {
            return defaultDim;
        }
    }
    
    /**
     * Returns the srid defined in geometry_columns for this table.
     */
    public int getGeometrySrid(String dbSchema, String dbTable, int defaultSrid) {
        try {
            StringBuilder query = new StringBuilder("SELECT srid FROM geometry_columns WHERE ");
            if (dbSchema != null) query.append("f_table_schema = '").append(dbSchema).append("' AND ");
            query.append("f_table_name = '").append(dbTable).append("';");
            ResultSet rs = connection.createStatement().executeQuery(query.toString());
            if (rs.next()) return rs.getInt(1);
            else return defaultSrid;
        } catch(SQLException sqle) {
            return defaultSrid;
        }
    }
    
    /** 
     * Returns a list of attributes compatible between postgis table and featureSchema.
     */
    public String[] compatibleSchemaSubset(String schemaName, String tableName,
                FeatureSchema featureSchema, boolean normalizedColumnNames) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        ResultSet rs = metadata.getColumns(null, schemaName, tableName, null);
        // map database column names to cooresponding feature attribute types
        Map<String,AttributeType> map = new HashMap<String,AttributeType>();
        while (rs.next()) {
            String name = rs.getString("COLUMN_NAME");
            AttributeType type = PostGISQueryUtil.getAttributeType(rs.getInt("DATA_TYPE"), rs.getString("TYPE_NAME"));
            // Only one attribute must use the AttributeType.GEOMETRY
            if (type == AttributeType.GEOMETRY && featureSchema.getAttributeType(name) != AttributeType.GEOMETRY) {
                map.put(name, AttributeType.OBJECT);
            }
            else map.put(name, type);
        }
        List<String> subset = new ArrayList<String>();
        for (int i = 0 ; i < featureSchema.getAttributeCount() ; i++) {
            String attribute = normalizedColumnNames ?
                    PostGISQueryUtil.normalize(featureSchema.getAttributeName(i))
                    :featureSchema.getAttributeName(i);
            AttributeType type = featureSchema.getAttributeType(i);
            if (map.containsKey(attribute) && (map.get(attribute)==type)) {
                subset.add(attribute);
            }
        }
        return subset.toArray(new String[subset.size()]);
    }

}